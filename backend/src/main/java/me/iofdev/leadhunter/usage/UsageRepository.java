package me.iofdev.leadhunter.usage;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import me.iofdev.leadhunter.auth.OrgId;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class UsageRepository {

    private final JdbcClient jdbc;

    public UsageRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public UsageReport report(UsageFilter filter) {
        return new UsageReport(apify(filter), llm(filter), byCampaign(filter));
    }

    /**
     * The newest runs and calls first, both kinds together. Job parents (ADR
     * 0033) are not runs themselves and never hold a location, so the run
     * queries count only rows with one: the per-location searches and the
     * review batches, whether or not the job ever reached them.
     */
    public List<UsageReport.Entry> entries(UsageFilter filter, int limit) {
        return jdbc.sql("""
                        select * from (
                            select 'apify' as kind, r.started_at as at, c.slug, r.location || ', ' || r.status as label,
                                   r.cost_usd as cost
                            from campaign_run r left join campaign c on c.id = r.campaign_id
                            where %s and r.location is not null
                            union all
                            select 'llm', l.created_at, c.slug, l.model || ', ' || l.purpose, l.cost_usd
                            from llm_call l left join campaign c on c.id = l.campaign_id
                            where %s
                        ) e
                        order by at desc
                        limit :limit
                        """.formatted(runWhere("r.started_at", "r.campaign_id", "r.org_id"), llmWhere("l.created_at", "l.campaign_id", "l.org_id")))
                .params(params(filter))
                .param("limit", limit)
                .query((rs, row) -> new UsageReport.Entry(
                        rs.getString("kind"),
                        rs.getObject("at", OffsetDateTime.class),
                        rs.getString("slug"),
                        rs.getString("label"),
                        rs.getBigDecimal("cost")))
                .list();
    }

    /** The organization's own monthly budget, empty when the operator never set one. */
    public Optional<BigDecimal> orgBudget(OrgId orgId) {
        return jdbc.sql("select monthly_budget_usd from organization where id = :id")
                .param("id", orgId.value())
                .query(BigDecimal.class)
                .optional();
    }

    /** A null {@code budgetUsd} goes back to the default from {@code leadhunter.usage.monthly-budget-usd}. */
    public void setOrgBudget(OrgId orgId, BigDecimal budgetUsd) {
        jdbc.sql("update organization set monthly_budget_usd = :budget where id = :id")
                .param("id", orgId.value())
                .param("budget", budgetUsd)
                .update();
    }

    /**
     * What a month has spent or is about to spend, for the budget check (ADR 0044). An {@code orgId} of null
     * counts every organization. Three parts:
     * <ul>
     * <li>every run with a location (a scraper run, a review batch or a pre-job row) at its cost, or at the
     * estimate stored when it started when the cost is missing;</li>
     * <li>every LLM call at its cost;</li>
     * <li>for each job still running, whatever month it started in, the part of its estimate that its own
     * runs do not cover yet. A job that started last month can still start runs this month (ADR 0046). A
     * job's LLM calls come in as they finish, so a running enrichment counts them twice until it ends. That
     * errs toward refusing, never toward overspending.</li>
     * </ul>
     */
    public BigDecimal committed(OrgId orgId, OffsetDateTime from, OffsetDateTime to) {
        return jdbc.sql("""
                        select
                            coalesce((select sum(coalesce(r.cost_usd, r.estimated_usd, 0)) from campaign_run r
                                      where r.location is not null and %1$s), 0)
                          + coalesce((select sum(l.cost_usd) from llm_call l where %2$s), 0)
                          + coalesce((select sum(greatest(p.estimated_usd - coalesce(
                                          (select sum(coalesce(c.cost_usd, c.estimated_usd, 0))
                                           from campaign_run c where c.parent_id = p.id), 0), 0))
                                      from campaign_run p
                                      where p.parent_id is null and p.location is null and p.status = 'RUNNING'
                                        and p.estimated_usd is not null and %3$s), 0)
                        """.formatted(committedWhere("r.started_at", "r.org_id"),
                        committedWhere("l.created_at", "l.org_id"), orgWhere("p.org_id")))
                .param("orgId", orgId == null ? null : orgId.value())
                .param("from", from)
                .param("to", to)
                .query(BigDecimal.class)
                .single();
    }

    private static String committedWhere(String timeColumn, String orgColumn) {
        return timeColumn + " >= cast(:from as timestamptz) and " + timeColumn + " < cast(:to as timestamptz)"
                + " and " + orgWhere(orgColumn);
    }

    private static String orgWhere(String orgColumn) {
        return "(cast(:orgId as text) is null or " + orgColumn + " = cast(:orgId as text))";
    }

    private UsageReport.Apify apify(UsageFilter filter) {
        return jdbc.sql("""
                        select count(*) as runs,
                               count(*) filter (where status = 'FAILED') as failed,
                               count(*) filter (where cost_usd is null) as unpriced,
                               coalesce(sum(places_found), 0) as places,
                               coalesce(sum(cost_usd), 0) as cost
                        from campaign_run
                        where %s and location is not null
                        """.formatted(runWhere("started_at", "campaign_id", "org_id")))
                .params(params(filter))
                .query((rs, row) -> new UsageReport.Apify(
                        rs.getInt("runs"), rs.getInt("failed"), rs.getInt("unpriced"),
                        rs.getLong("places"), rs.getBigDecimal("cost")))
                .single();
    }

    private UsageReport.Llm llm(UsageFilter filter) {
        List<UsageReport.ModelSpend> models = jdbc.sql("""
                        select model, count(*) as calls, coalesce(sum(cost_usd), 0) as cost
                        from llm_call
                        where %s
                        group by model
                        order by sum(cost_usd) desc nulls last, model
                        """.formatted(llmWhere("created_at", "campaign_id", "org_id")))
                .params(params(filter))
                .query((rs, row) -> new UsageReport.ModelSpend(
                        rs.getString("model"), rs.getInt("calls"), rs.getBigDecimal("cost")))
                .list();
        return jdbc.sql("""
                        select count(*) as calls,
                               count(*) filter (where cost_usd is null) as unpriced,
                               coalesce(sum(prompt_tokens), 0) as prompt_tokens,
                               coalesce(sum(completion_tokens), 0) as completion_tokens,
                               coalesce(sum(cost_usd), 0) as cost
                        from llm_call
                        where %s
                        """.formatted(llmWhere("created_at", "campaign_id", "org_id")))
                .params(params(filter))
                .query((rs, row) -> new UsageReport.Llm(
                        rs.getInt("calls"), rs.getInt("unpriced"),
                        rs.getLong("prompt_tokens"), rs.getLong("completion_tokens"),
                        rs.getBigDecimal("cost"), models))
                .single();
    }

    /**
     * Spend per campaign. Runs of a deleted campaign and LLM calls made outside one share a row whose slug is
     * null, so the rows always add up to the total (ADR 0046). That row only shows when it cost something.
     */
    private List<UsageReport.CampaignSpend> byCampaign(UsageFilter filter) {
        return jdbc.sql("""
                        select c.slug, coalesce(sum(s.apify), 0) as apify_cost, coalesce(sum(s.llm), 0) as llm_cost
                        from (
                            select campaign_id, cost_usd as apify, null::numeric as llm from campaign_run
                            where %s and location is not null
                            union all
                            select campaign_id, null, cost_usd from llm_call
                            where %s
                        ) s
                        left join campaign c on c.id = s.campaign_id
                        group by s.campaign_id, c.slug
                        having s.campaign_id is not null or coalesce(sum(s.apify), 0) + coalesce(sum(s.llm), 0) > 0
                        order by coalesce(sum(s.apify), 0) + coalesce(sum(s.llm), 0) desc, c.slug nulls last
                        """.formatted(runWhere("started_at", "campaign_id", "org_id"), llmWhere("created_at", "campaign_id", "org_id")))
                .params(params(filter))
                .query((rs, row) -> new UsageReport.CampaignSpend(
                        rs.getString("slug"), rs.getBigDecimal("apify_cost"), rs.getBigDecimal("llm_cost")))
                .list();
    }

    /** Runs carry their organization, because a deleted campaign leaves its runs behind (ADR 0044). */
    private static String runWhere(String timeColumn, String campaignColumn, String orgColumn) {
        return where(timeColumn, campaignColumn) + " and " + orgColumn + " = :orgId";
    }

    /** LLM calls carry their organization, because some belong to no campaign. */
    private static String llmWhere(String timeColumn, String campaignColumn, String orgColumn) {
        return where(timeColumn, campaignColumn) + " and " + orgColumn + " = :orgId";
    }

    /** The time window on {@code timeColumn} and the campaign on {@code campaignColumn}, shared by every query. */
    private static String where(String timeColumn, String campaignColumn) {
        return "(cast(:from as timestamptz) is null or " + timeColumn + " >= cast(:from as timestamptz))"
                + " and (cast(:to as timestamptz) is null or " + timeColumn + " < cast(:to as timestamptz))"
                + " and (cast(:campaignId as bigint) is null or " + campaignColumn + " = cast(:campaignId as bigint))";
    }

    private static Map<String, Object> params(UsageFilter filter) {
        Map<String, Object> params = new HashMap<>();
        params.put("orgId", filter.orgId().value());
        params.put("from", filter.from());
        params.put("to", filter.to());
        params.put("campaignId", filter.campaignId());
        return params;
    }
}
