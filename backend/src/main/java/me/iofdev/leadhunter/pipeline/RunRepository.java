package me.iofdev.leadhunter.pipeline;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import me.iofdev.leadhunter.maps.ExternalRunResult;
import org.springframework.jdbc.core.simple.JdbcClient;
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
     * One job as the UI polls it (ADR 0033): the parent row plus the live
     * sums of its children. While RUNNING, {@code done} is what the children
     * found so far; once finished it is the verdict the job stored.
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

    private static final String JOB_SELECT = """
            select p.id, c.slug as campaign_slug, p.kind, p.status, p.started_at,
                case when p.status = 'RUNNING' then coalesce(sum(ch.places_found), 0)
                     else coalesce(p.places_found, 0) end as done,
                p.total,
                coalesce(p.cost_usd, 0) + coalesce(sum(ch.cost_usd), 0) as cost_usd,
                p.error
            from campaign_run p
            join campaign c on c.id = p.campaign_id
            left join campaign_run ch on ch.parent_id = p.id
            """;

    private final JdbcClient jdbc;

    public RunRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
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
     * Opens the parent row for one UI-started job (ADR 0033). Parents span
     * locations, so they hold no location of their own. The partial unique
     * index rejects a second RUNNING job per campaign.
     */
    public long startJob(long campaignId, String kind, Integer total) {
        return jdbc.sql("""
                        insert into campaign_run (campaign_id, kind, status, total)
                        values (:campaignId, :kind, 'RUNNING', :total)
                        returning id
                        """)
                .param("campaignId", campaignId)
                .param("kind", kind)
                .param("total", total)
                .query(Long.class)
                .single();
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

    public void failJob(long jobId, String error) {
        jdbc.sql("""
                        update campaign_run
                        set status = 'FAILED', error = :error, finished_at = now()
                        where id = :id
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

    /** At most one run per campaign at a time, across the CLI and the API alike. */
    public boolean runningExists(long campaignId) {
        return jdbc.sql("""
                        select exists(select 1 from campaign_run
                                      where campaign_id = :campaignId
                                        and status = 'RUNNING' and parent_id is null)
                        """)
                .param("campaignId", campaignId)
                .query(Boolean.class)
                .single();
    }

    /** Marks jobs a restart interrupted as failed. Runs on boot in every mode (ADR 0033). */
    public int markInterrupted() {
        return jdbc.sql("""
                        update campaign_run
                        set status = 'FAILED', error = 'interrupted by restart', finished_at = now()
                        where status = 'RUNNING'
                        """)
                .update();
    }

    public Optional<JobView> findJob(long jobId) {
        return jdbc.sql(JOB_SELECT + "where p.id = :id group by p.id, c.slug")
                .param("id", jobId)
                .query((rs, row) -> new JobView(
                        rs.getLong("id"),
                        rs.getString("campaign_slug"),
                        rs.getString("kind"),
                        rs.getString("status"),
                        rs.getObject("started_at", OffsetDateTime.class),
                        rs.getLong("done"),
                        rs.getObject("total", Integer.class),
                        rs.getBigDecimal("cost_usd"),
                        rs.getString("error")))
                .optional();
    }

    /** Newest first: job parents plus the standalone rows the CLI wrote, never the children. */
    public List<JobView> listJobs(long campaignId) {
        return jdbc.sql(JOB_SELECT
                        + "where p.campaign_id = :campaignId and p.parent_id is null and p.kind <> 'REVIEWS' "
                        + "group by p.id, c.slug order by p.started_at desc, p.id desc")
                .param("campaignId", campaignId)
                .query((rs, row) -> new JobView(
                        rs.getLong("id"),
                        rs.getString("campaign_slug"),
                        rs.getString("kind"),
                        rs.getString("status"),
                        rs.getObject("started_at", OffsetDateTime.class),
                        rs.getLong("done"),
                        rs.getObject("total", Integer.class),
                        rs.getBigDecimal("cost_usd"),
                        rs.getString("error")))
                .list();
    }
}
