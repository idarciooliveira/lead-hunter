package me.iofdev.leadhunter.pipeline;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
            select l.id, c.slug, l.stage, l.status, l.lost_reason, l.outcome_note, l.score, l.score_breakdown, l.stage_reason,
                   p.name, p.category, p.address, p.neighborhood, p.phone_e164, p.phone_mobile, p.website,
                   p.website_kind, p.rating, p.reviews_count, p.maps_url
            from lead l
            join place p on p.id = l.place_id
            join campaign c on c.id = l.campaign_id
            """;

    private static final String UNENRICHED_QUALIFIED = """
            where l.campaign_id = :campaignId
              and l.status = 'NEW'
              and l.stage = 'QUALIFIED'
              and l.enriched_at is null
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

    /**
     * Qualified leads this campaign has not enriched yet, best first, up to the batch size.
     * Already-enriched leads are never returned, so re-running enrichment is safe.
     */
    public List<EnrichmentTarget> unenrichedQualified(long campaignId, int limit) {
        return jdbc.sql("""
                        select l.id, l.place_id, l.score, l.score_breakdown,
                               p.name, p.website, p.website_kind, p.maps_url
                        from lead l
                        join place p on p.id = l.place_id
                        """
                        + UNENRICHED_QUALIFIED
                        + """
                        order by l.score desc, p.reviews_count desc, l.id
                        limit :limit
                        """)
                .param("campaignId", campaignId)
                .param("limit", limit)
                .query((rs, row) -> new EnrichmentTarget(
                        rs.getLong("id"),
                        rs.getLong("place_id"),
                        rs.getString("name"),
                        rs.getString("website"),
                        WebsiteKind.valueOf(rs.getString("website_kind")),
                        rs.getString("maps_url"),
                        rs.getInt("score"),
                        json.readValue(rs.getString("score_breakdown"), SCORE_ITEMS)))
                .list();
    }

    public int countUnenrichedQualified(long campaignId) {
        return jdbc.sql("""
                        select count(*)
                        from lead l
                        """
                        + UNENRICHED_QUALIFIED)
                .param("campaignId", campaignId)
                .query(Integer.class)
                .single();
    }

    /** Stores the combined stage 1 + stage 2 score and marks the lead enriched. */
    public void saveStage2(long leadId, Score score, Set<String> complaintKinds) {
        jdbc.sql("""
                        update lead
                        set score = :score,
                            score_breakdown = cast(:breakdown as jsonb),
                            complaint_kinds = :complaints,
                            enriched_at = now(),
                            updated_at = now()
                        where id = :id
                        """)
                .param("id", leadId)
                .param("score", score.total())
                .param("breakdown", json.writeValueAsString(score.items()))
                .param("complaints", complaintKinds.stream().sorted().toArray(String[]::new))
                .update();
    }

    /**
     * Marks a contact outcome. The CLI and the API are both thin adapters over this
     * method, so the rules can never drift. See ADR 0012 and ADR 0020.
     *
     * <ul>
     *   <li>LOST needs one of the five lost reasons, including NOT_NOW.</li>
     *   <li>A lost reason needs status LOST; any other status must drop it.</li>
     *   <li>A lead that was already worked can never go back to NEW, which would hide
     *       the contact history. Every other transition is allowed, so a NOT_NOW lead
     *       can come back into the queue later.</li>
     * </ul>
     *
     * @throws IllegalArgumentException with a {@code no lead with id <id>} message for
     *         an unknown id (the API maps it to 404) and any other message for a bad
     *         transition or reason (the API maps it to 400, the CLI prints {@code error:}).
     */
    public LeadView updateOutcome(long id, LeadStatus status, LostReason lostReason, String note) {
        LeadView current = findById(id)
                .orElseThrow(() -> new IllegalArgumentException("no lead with id " + id));
        requireOutcome(current, status, lostReason);
        String storedNote = note == null || note.isBlank() ? null : note;
        jdbc.sql("""
                        update lead
                        set status = :status,
                            lost_reason = :lostReason,
                            outcome_note = :note,
                            updated_at = now()
                        where id = :id
                        """)
                .param("id", id)
                .param("status", status.name())
                .param("lostReason", status == LeadStatus.LOST ? lostReason.name() : null)
                .param("note", storedNote)
                .update();
        return findById(id).orElseThrow();
    }

    private static void requireOutcome(LeadView current, LeadStatus status, LostReason lostReason) {
        if (status == null) {
            throw new IllegalArgumentException("status is required: "
                    + "NEW, CONTACTED, NO_ANSWER, INTERESTED, MEETING, PROPOSAL_SENT, WON or LOST");
        }
        if (status == LeadStatus.LOST && lostReason == null) {
            throw new IllegalArgumentException("marking lead " + current.id() + " LOST needs a lost reason: "
                    + "--lost-reason NO_BUDGET, WRONG_PERSON, HAS_SUPPLIER, NOT_INTERESTED or NOT_NOW");
        }
        if (status != LeadStatus.LOST && lostReason != null) {
            throw new IllegalArgumentException("a lost reason needs status LOST, got " + status
                    + ". Drop --lost-reason or mark the lead LOST");
        }
        if (status == LeadStatus.NEW && current.status() != LeadStatus.NEW) {
            throw new IllegalArgumentException("lead " + current.id() + " is already " + current.status()
                    + "; marking it NEW again would hide the contact history");
        }
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
        String lostReason = rs.getString("lost_reason");
        return new LeadView(
                rs.getLong("id"),
                rs.getString("slug"),
                LeadStage.valueOf(rs.getString("stage")),
                LeadStatus.valueOf(rs.getString("status")),
                lostReason == null ? null : LostReason.valueOf(lostReason),
                rs.getString("outcome_note"),
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
