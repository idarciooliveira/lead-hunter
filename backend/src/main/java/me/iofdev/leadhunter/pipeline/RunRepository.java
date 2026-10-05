package me.iofdev.leadhunter.pipeline;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import me.iofdev.leadhunter.maps.ExternalRunResult;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Repository;

@Repository
public class RunRepository {

    /** UsageRepository labels runs by location, so review runs use this location. */
    public static final String REVIEWS_LOCATION = "reviews";

    /** Kinds of {@code campaign_run} rows. Parents are SCRAPE, ENRICH or DRY_RUN; children are SCRAPE or REVIEWS. */
    public static final String KIND_SCRAPE = "SCRAPE";
    public static final String KIND_ENRICH = "ENRICH";
    public static final String KIND_DRY_RUN = "DRY_RUN";
    public static final String KIND_REVIEWS = "REVIEWS";

    /**
     * One job as the UI polls it (ADR 0033): the parent row plus the cost of
     * its children. {@code done} is what the job reported so far, then its
     * verdict. {@code costUsd} is null when a finished part has no known cost.
     */
    public record JobView(
            long id,
            String campaignSlug,
            String kind,
            String status,
            OffsetDateTime startedAt,
            long done,
            Integer total,
            BigDecimal costUsd,
            String error) {
    }

    /**
     * Apify cost comes from the children, or from the row itself for the
     * standalone rows the CLI wrote before jobs had parents. An enrichment
     * also owns the LLM calls its campaign made while it ran: one job per
     * campaign at a time makes that window exact.
     */
    private static final String JOB_SELECT = """
            select p.id, c.slug as campaign_slug, p.kind, p.status, p.started_at,
                coalesce(p.places_found, 0) as done, p.total,
                case when a.unpriced or l.unpriced
                          or (p.location is not null and p.status <> 'RUNNING' and p.cost_usd is null) then null
                     else coalesce(p.cost_usd, 0) + a.cost + l.cost end as cost_usd,
                p.error
            from campaign_run p
            join campaign c on c.id = p.campaign_id
            cross join lateral (
                select coalesce(sum(r.cost_usd), 0) as cost,
                       coalesce(bool_or(r.status <> 'RUNNING' and r.cost_usd is null), false) as unpriced
                from campaign_run r
                where r.parent_id = p.id
            ) a
            cross join lateral (
                select coalesce(sum(m.cost_usd), 0) as cost, coalesce(bool_or(m.cost_usd is null), false) as unpriced
                from llm_call m
                where p.kind = 'ENRICH' and m.campaign_id = p.campaign_id
                  and m.created_at >= p.started_at and m.created_at <= coalesce(p.finished_at, now())
            ) l
            """;

    private static final RowMapper<JobView> JOB_VIEW = (rs, row) -> new JobView(
            rs.getLong("id"),
            rs.getString("campaign_slug"),
            rs.getString("kind"),
            rs.getString("status"),
            rs.getObject("started_at", OffsetDateTime.class),
            rs.getLong("done"),
            rs.getObject("total", Integer.class),
            rs.getBigDecimal("cost_usd"),
            rs.getString("error"));

    /** The two-key advisory lock space of job leases, apart from Flyway's single-key locks. */
    private static final int LOCK_SPACE = 0x4C48;
    static final String UNLOCK = "select pg_advisory_unlock(" + lockKey("?") + ")";

    private final JdbcClient jdbc;
    private final DataSource dataSource;

    public RunRepository(JdbcClient jdbc, DataSource dataSource) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
    }

    public long start(long campaignId, String location, List<String> terms, int maxPlaces) {
        return start(campaignId, location, terms, maxPlaces, null, KIND_SCRAPE);
    }

    public long start(long campaignId, String location, List<String> terms, int maxPlaces,
                      Long parentId, String kind) {
        return jdbc.sql("""
                        insert into campaign_run (campaign_id, location, search_terms, max_places, status, parent_id, kind)
                        values (:campaignId, :location, :terms, :maxPlaces, 'RUNNING', :parentId, :kind)
                        returning id
                        """)
                .param("campaignId", campaignId)
                .param("location", location)
                .param("terms", terms.toArray(String[]::new))
                .param("maxPlaces", maxPlaces)
                .param("parentId", parentId)
                .param("kind", kind)
                .query(Long.class)
                .single();
    }

    public void succeed(long runId, ExternalRunResult result, int placesFound) {
        jdbc.sql("""
                        update campaign_run
                        set status = 'SUCCEEDED', external_run_id = :externalRunId, dataset_id = :datasetId,
                            places_found = :placesFound, cost_usd = :costUsd, finished_at = now()
                        where id = :id
                        """)
                .param("id", runId)
                .param("externalRunId", result.externalRunId())
                .param("datasetId", result.datasetId())
                .param("placesFound", placesFound)
                .param("costUsd", result.costUsd())
                .update();
    }

    /** A failed run still costs money, so costUsd is stored when Apify reported one. */
    public void fail(long runId, RunFailure failure) {
        jdbc.sql("""
                        update campaign_run
                        set status = 'FAILED', external_run_id = :externalRunId, error = :error,
                            cost_usd = :costUsd, finished_at = now()
                        where id = :id
                        """)
                .param("id", runId)
                .param("externalRunId", failure.externalRunId())
                .param("error", failure.error())
                .param("costUsd", failure.costUsd())
                .update();
    }

    /**
     * Opens the parent row of one job, from the UI or the CLI (ADR 0033), and
     * takes its lease in the same statement, so no other process ever sees
     * the row without its lock (ADR 0036). Parents span locations, so they
     * hold no location of their own. The partial unique index rejects a
     * second RUNNING job per campaign.
     */
    public JobLease startJob(long campaignId, String kind, Integer total) {
        Connection connection = connect();
        try {
            long jobId = session(connection).sql("""
                            with job as (
                                insert into campaign_run (campaign_id, kind, status, total)
                                values (:campaignId, :kind, 'RUNNING', :total)
                                returning id
                            )
                            select id from job where pg_try_advisory_lock(%s)
                            """.formatted(lockKey("id")))
                    .param("campaignId", campaignId)
                    .param("kind", kind)
                    .param("total", total)
                    .query(Long.class)
                    .single();
            return new JobLease(jobId, connection);
        } catch (RuntimeException e) {
            closeQuietly(connection);
            throw e;
        }
    }

    /** What a running job has done so far: places found for a scrape, leads enriched for an enrichment. */
    public void progressJob(long jobId, long done) {
        jdbc.sql("update campaign_run set places_found = :done where id = :id")
                .param("id", jobId)
                .param("done", done)
                .update();
    }

    /** Closes a job with the verdict the runner returned. Cost stays on the children; the view sums it. */
    public void finishJob(long jobId, long done) {
        jdbc.sql("""
                        update campaign_run
                        set status = 'SUCCEEDED', places_found = :done, finished_at = now()
                        where id = :id
                        """)
                .param("id", jobId)
                .param("done", done)
                .update();
    }

    /** Also fails a child the job left RUNNING when it crashed mid-search. */
    public void failJob(long jobId, String error) {
        jdbc.sql("""
                        update campaign_run
                        set status = 'FAILED', error = :error, finished_at = now()
                        where id = :id or (parent_id = :id and status = 'RUNNING')
                        """)
                .param("id", jobId)
                .param("error", error)
                .update();
    }

    /** A dry run costs nothing but lands in the history, so the UI shows the estimates it showed. */
    public long recordDryRun(long campaignId, long done, Integer total) {
        return jdbc.sql("""
                        insert into campaign_run (campaign_id, kind, status, places_found, total,
                            cost_usd, started_at, finished_at)
                        values (:campaignId, :kind, 'SUCCEEDED', :done, :total, 0, now(), now())
                        returning id
                        """)
                .param("campaignId", campaignId)
                .param("kind", KIND_DRY_RUN)
                .param("done", done)
                .param("total", total)
                .query(Long.class)
                .single();
    }

    /**
     * Fails the RUNNING jobs whose process is gone, with their children (ADR
     * 0036): a job whose lease this session can take has no live owner. A
     * null campaign checks every campaign. Live jobs, in this process or
     * another, keep their lock and are left alone. Returns the jobs failed.
     */
    public int failAbandoned(Long campaignId) {
        Connection connection = connect();
        try {
            JdbcClient session = session(connection);
            List<Long> running = session.sql("""
                            select id from campaign_run
                            where status = 'RUNNING' and parent_id is null
                              and (cast(:campaignId as bigint) is null or campaign_id = cast(:campaignId as bigint))
                            """)
                    .param("campaignId", campaignId)
                    .query(Long.class)
                    .list();
            int failed = 0;
            for (long jobId : running) {
                boolean abandoned = session.sql("select pg_try_advisory_lock(" + lockKey(":id") + ")")
                        .param("id", jobId)
                        .query(Boolean.class)
                        .single();
                if (!abandoned) {
                    continue;
                }
                int updated = session.sql("""
                                update campaign_run
                                set status = 'FAILED', error = 'interrupted before it finished', finished_at = now()
                                where status = 'RUNNING' and (id = :id or parent_id = :id)
                                """)
                        .param("id", jobId)
                        .update();
                session.sql("select pg_advisory_unlock(" + lockKey(":id") + ")").param("id", jobId).query(Boolean.class).single();
                if (updated > 0) {
                    failed++;
                }
            }
            return failed;
        } finally {
            closeQuietly(connection);
        }
    }

    public Optional<JobView> findJob(long jobId) {
        return jdbc.sql(JOB_SELECT + "where p.id = :id")
                .param("id", jobId)
                .query(JOB_VIEW)
                .optional();
    }

    /** Newest first: job parents plus the standalone rows the CLI wrote, never the children. */
    public List<JobView> listJobs(long campaignId) {
        return jdbc.sql(JOB_SELECT
                        + "where p.campaign_id = :campaignId and p.parent_id is null and p.kind <> 'REVIEWS' "
                        + "order by p.started_at desc, p.id desc")
                .param("campaignId", campaignId)
                .query(JOB_VIEW)
                .list();
    }

    /** Job ids are bigint and lock keys int; ids that wrap only collide 2^31 jobs apart. */
    private static String lockKey(String jobId) {
        return LOCK_SPACE + ", cast(" + jobId + " % 2147483647 as integer)";
    }

    /** Leases need their own connection: an advisory lock lives as long as its session. */
    private Connection connect() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("could not open a database connection", e);
        }
    }

    private static JdbcClient session(Connection connection) {
        return JdbcClient.create(new SingleConnectionDataSource(connection, true));
    }

    private static void closeQuietly(Connection connection) {
        try {
            connection.close();
        } catch (SQLException e) {
            // The pool drops a broken connection; Postgres frees its locks with the session.
        }
    }
}
