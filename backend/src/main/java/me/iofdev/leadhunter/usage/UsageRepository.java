package me.iofdev.leadhunter.usage;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class UsageRepository {

    private final JdbcClient jdbc;

    public UsageRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public UsageReport report(UsageFilter filter) {
        return new UsageReport(apify(filter), llm(filter), byCampaign(filter), llmWithoutCampaign(filter));
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
                            from campaign_run r join campaign c on c.id = r.campaign_id
                            where %s and r.location is not null
                            union all
                            select 'llm', l.created_at, c.slug, l.model || ', ' || l.purpose, l.cost_usd
                            from llm_call l left join campaign c on c.id = l.campaign_id
                            where %s
                        ) e
                        order by at desc
                        limit :limit
                        """.formatted(where("r.started_at", "r.campaign_id"), where("l.created_at", "l.campaign_id")))
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

    private UsageReport.Apify apify(UsageFilter filter) {
        return jdbc.sql("""
                        select count(*) as runs,
                               count(*) filter (where status = 'FAILED') as failed,
                               count(*) filter (where cost_usd is null) as unpriced,
                               coalesce(sum(places_found), 0) as places,
                               coalesce(sum(cost_usd), 0) as cost
                        from campaign_run
                        where %s and location is not null
                        """.formatted(where("started_at", "campaign_id")))
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
                        """.formatted(where("created_at", "campaign_id")))
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
                        """.formatted(where("created_at", "campaign_id")))
                .params(params(filter))
                .query((rs, row) -> new UsageReport.Llm(
                        rs.getInt("calls"), rs.getInt("unpriced"),
                        rs.getLong("prompt_tokens"), rs.getLong("completion_tokens"),
                        rs.getBigDecimal("cost"), models))
                .single();
    }

    private List<UsageReport.CampaignSpend> byCampaign(UsageFilter filter) {
        return jdbc.sql("""
                        select c.slug, coalesce(a.cost, 0) as apify_cost, coalesce(l.cost, 0) as llm_cost
                        from campaign c
                        left join (select campaign_id, sum(cost_usd) as cost from campaign_run
                                   where %s group by campaign_id) a on a.campaign_id = c.id
                        left join (select campaign_id, sum(cost_usd) as cost from llm_call
                                   where %s group by campaign_id) l on l.campaign_id = c.id
                        where (a.campaign_id is not null or l.campaign_id is not null)
                          and (cast(:campaignId as bigint) is null or c.id = cast(:campaignId as bigint))
                        order by coalesce(a.cost, 0) + coalesce(l.cost, 0) desc, c.slug
                        """.formatted(where("started_at", "campaign_id"), where("created_at", "campaign_id")))
                .params(params(filter))
                .query((rs, row) -> new UsageReport.CampaignSpend(
                        rs.getString("slug"), rs.getBigDecimal("apify_cost"), rs.getBigDecimal("llm_cost")))
                .list();
    }

    private BigDecimal llmWithoutCampaign(UsageFilter filter) {
        if (filter.campaignId() != null) {
            return BigDecimal.ZERO;
        }
        return jdbc.sql("select coalesce(sum(cost_usd), 0) from llm_call where campaign_id is null and "
                        + where("created_at", "campaign_id"))
                .params(params(filter))
                .query(BigDecimal.class)
                .single();
    }

    /** The conditions every query shares: the time window on {@code timeColumn} and the campaign on {@code campaignColumn}. */
    private static String where(String timeColumn, String campaignColumn) {
        return "(cast(:from as timestamptz) is null or " + timeColumn + " >= cast(:from as timestamptz))"
                + " and (cast(:to as timestamptz) is null or " + timeColumn + " < cast(:to as timestamptz))"
                + " and (cast(:campaignId as bigint) is null or " + campaignColumn + " = cast(:campaignId as bigint))";
    }

    private static Map<String, Object> params(UsageFilter filter) {
        Map<String, Object> params = new HashMap<>();
        params.put("from", filter.from());
        params.put("to", filter.to());
        params.put("campaignId", filter.campaignId());
        return params;
    }
}
