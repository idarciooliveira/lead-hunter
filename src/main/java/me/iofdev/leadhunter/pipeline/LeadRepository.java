package me.iofdev.leadhunter.pipeline;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import me.iofdev.leadhunter.place.WebsiteKind;
import me.iofdev.leadhunter.scoring.Score;
import me.iofdev.leadhunter.scoring.ScoreItem;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class LeadRepository {

    private static final TypeReference<List<ScoreItem>> SCORE_ITEMS = new TypeReference<>() {
    };

    private static final String SELECT_VIEW = """
            select l.id, c.slug, l.stage, l.status, l.score, l.score_breakdown, l.stage_reason,
                   p.name, p.category, p.address, p.neighborhood, p.phone_e164, p.phone_mobile, p.website,
                   p.website_kind, p.rating, p.reviews_count, p.maps_url
            from lead l
            join place p on p.id = l.place_id
            join campaign c on c.id = l.campaign_id
            """;

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public LeadRepository(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /**
     * Records the stage 1 result for a place in a campaign. A lead the user already worked on, or one
     * that moved past stage 1, keeps its state. Returns true when the lead is new to the campaign.
     */
    public boolean saveStage1(long campaignId, long placeId, long runId, LeadStage stage, Score score, String reason) {
        return jdbc.sql("""
                        insert into lead (campaign_id, place_id, first_run_id, stage, score, score_breakdown, stage_reason)
                        values (:campaignId, :placeId, :runId, :stage, :score, cast(:breakdown as jsonb), :reason)
                        on conflict (campaign_id, place_id) do update
                            set stage = excluded.stage,
                                score = excluded.score,
                                score_breakdown = excluded.score_breakdown,
                                stage_reason = excluded.stage_reason,
                                updated_at = now()
                            where lead.status = 'NEW' and lead.stage in ('EXCLUDED', 'BELOW_CUT', 'QUALIFIED')
                        returning (xmax = 0) as inserted
                        """)
                .param("campaignId", campaignId)
                .param("placeId", placeId)
                .param("runId", runId)
                .param("stage", stage.name())
                .param("score", score.total())
                .param("breakdown", json.writeValueAsString(score.items()))
                .param("reason", reason)
                .query(Boolean.class)
                .optional()
                .orElse(false);
    }

    /**
     * Ranks every untouched, non-excluded lead in the campaign and keeps the top share as QUALIFIED.
     * Ties go to the place with more reviews.
     */
    public void applyStage1Cut(long campaignId, double qualifyShare) {
        jdbc.sql("""
                        with ranked as (
                            select l.id,
                                   row_number() over (order by l.score desc, p.reviews_count desc, l.id) as position,
                                   count(*) over () as total
                            from lead l
                            join place p on p.id = l.place_id
                            where l.campaign_id = :campaignId
                              and l.status = 'NEW'
                              and l.stage in ('BELOW_CUT', 'QUALIFIED')
                        )
                        update lead
                        set stage = case when r.position <= ceil(r.total * cast(:share as numeric))
                                         then 'QUALIFIED' else 'BELOW_CUT' end,
                            stage_reason = case when r.position <= ceil(r.total * cast(:share as numeric))
                                                then null
                                                else 'Ranked ' || r.position || ' of ' || r.total
                                                     || ', below the top ' || round(cast(:share as numeric) * 100) || '% cut' end,
                            updated_at = now()
                        from ranked r
                        where lead.id = r.id
                        """)
                .param("campaignId", campaignId)
                .param("share", qualifyShare)
                .update();
    }

    public List<LeadView> list(long campaignId, Optional<LeadStage> stage, int limit) {
        return jdbc.sql(SELECT_VIEW + """
                        where l.campaign_id = :campaignId
                          and (cast(:stage as text) is null or l.stage = :stage)
                        order by case l.stage when 'QUALIFIED' then 0 when 'BELOW_CUT' then 1 else 2 end,
                                 l.score desc, p.reviews_count desc, l.id
                        limit :limit
                        """)
                .param("campaignId", campaignId)
                .param("stage", stage.map(Enum::name).orElse(null))
                .param("limit", limit)
                .query(this::mapView)
                .list();
    }

    public Optional<LeadView> findById(long id) {
        return jdbc.sql(SELECT_VIEW + " where l.id = :id").param("id", id).query(this::mapView).optional();
    }

    public StageCounts countByStage(long campaignId) {
        return jdbc.sql("""
                        select count(*) filter (where stage = 'QUALIFIED') as qualified,
                               count(*) filter (where stage = 'BELOW_CUT') as below_cut,
                               count(*) filter (where stage = 'EXCLUDED') as excluded
                        from lead where campaign_id = :campaignId
                        """)
                .param("campaignId", campaignId)
                .query((rs, row) -> new StageCounts(rs.getInt("qualified"), rs.getInt("below_cut"), rs.getInt("excluded")))
                .single();
    }

    public record StageCounts(int qualified, int belowCut, int excluded) {
    }

    private LeadView mapView(ResultSet rs, int row) throws SQLException {
        return new LeadView(
                rs.getLong("id"),
                rs.getString("slug"),
                LeadStage.valueOf(rs.getString("stage")),
                LeadStatus.valueOf(rs.getString("status")),
                rs.getInt("score"),
                json.readValue(rs.getString("score_breakdown"), SCORE_ITEMS),
                rs.getString("stage_reason"),
                rs.getString("name"),
                rs.getString("category"),
                rs.getString("address"),
                rs.getString("neighborhood"),
                rs.getString("phone_e164"),
                rs.getBoolean("phone_mobile"),
                rs.getString("website"),
                WebsiteKind.valueOf(rs.getString("website_kind")),
                rs.getBigDecimal("rating"),
                rs.getInt("reviews_count"),
                rs.getString("maps_url"));
    }
}
