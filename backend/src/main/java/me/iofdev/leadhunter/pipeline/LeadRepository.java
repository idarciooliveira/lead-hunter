package me.iofdev.leadhunter.pipeline;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import me.iofdev.leadhunter.auth.OrgId;
import me.iofdev.leadhunter.place.WebsiteKind;
import me.iofdev.leadhunter.scoring.Score;
import me.iofdev.leadhunter.scoring.ScoreItem;
import me.iofdev.leadhunter.scoring.Stage2Scorer;
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
                   p.website_kind, p.rating, p.reviews_count, p.maps_url, l.complaint_kinds, l.pitch
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
     *
     * <p>An enriched lead keeps its stage 2 items while it stays in the pool. A rescrape that excludes it,
     * or that moves its place to another website, drops the enrichment, so the lead goes back to the queue
     * when it qualifies again.
     */
    public boolean saveStage1(long campaignId, long placeId, long runId, LeadStage stage, Score score, String reason) {
        Optional<String> stored = enrichedBreakdown(campaignId, placeId);
        boolean keepStageTwo = stored.isPresent() && stage != LeadStage.EXCLUDED && !websiteChanged(placeId);
        Score combined = keepStageTwo ? withStageTwo(score, stored.get()) : score;
        return jdbc.sql("""
                        insert into lead (campaign_id, place_id, first_run_id, stage, score, score_breakdown, stage_reason)
                        values (:campaignId, :placeId, :runId, :stage, :score, cast(:breakdown as jsonb), :reason)
                        on conflict (campaign_id, place_id) do update
                            set stage = excluded.stage,
                                score = excluded.score,
                                score_breakdown = excluded.score_breakdown,
                                stage_reason = excluded.stage_reason,
                                enriched_at = case when :dropEnrichment then null else lead.enriched_at end,
                                complaint_kinds = case when :dropEnrichment
                                                       then cast('{}' as text[]) else lead.complaint_kinds end,
                                updated_at = now()
                            where lead.status = 'NEW' and lead.stage in ('EXCLUDED', 'BELOW_CUT', 'QUALIFIED')
                        returning (xmax = 0) as inserted
                        """)
                .param("campaignId", campaignId)
                .param("placeId", placeId)
                .param("runId", runId)
                .param("stage", stage.name())
                .param("score", combined.total())
                .param("breakdown", json.writeValueAsString(combined.items()))
                .param("reason", reason)
                .param("dropEnrichment", stored.isPresent() && !keepStageTwo)
                .query(Boolean.class)
                .optional()
                .orElse(false);
    }

    /** The stored breakdown of the lead when stage 2 ran on it, empty for a lead never enriched. */
    private Optional<String> enrichedBreakdown(long campaignId, long placeId) {
        return jdbc.sql("""
                        select score_breakdown from lead
                        where campaign_id = :campaignId and place_id = :placeId and enriched_at is not null
                        """)
                .param("campaignId", campaignId)
                .param("placeId", placeId)
                .query((rs, row) -> rs.getString("score_breakdown"))
                .optional();
    }

    /** The new stage 1 score with the stage 2 items the lead already had appended. */
    private Score withStageTwo(Score stageOne, String storedBreakdown) {
        List<ScoreItem> items = new ArrayList<>(stageOne.items());
        json.readValue(storedBreakdown, SCORE_ITEMS).stream()
                .filter(item -> Stage2Scorer.isStage2Code(item.code()))
                .forEach(items::add);
        return Score.of(items);
    }

    /**
     * True when the place now has another website than the one enrichment read. Enrichment crawls only an own
     * website, so a place with no crawl and no own website has nothing to compare, and is not changed.
     */
    private boolean websiteChanged(long placeId) {
        return jdbc.sql("""
                        select exists (
                            select 1
                            from place p
                            left join lateral (
                                select w.url from website_crawl w
                                where w.place_id = p.id
                                order by w.crawled_at desc, w.id desc
                                limit 1
                            ) c on true
                            where p.id = :placeId
                              and c.url is distinct from p.website
                              and (c.url is not null or p.website_kind = 'OWN')
                        )
                        """)
                .param("placeId", placeId)
                .query(Boolean.class)
                .single();
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

    /**
     * The contact queue for today: qualified leads nobody has worked yet, across every campaign of the organization,
     * best first. Same tie-breaks as {@link #list}.
     */
    public List<LeadView> today(OrgId orgId, int limit) {
        return jdbc.sql(SELECT_VIEW + """
                        where c.org_id = :orgId and l.stage = 'QUALIFIED' and l.status = 'NEW'
                        order by l.score desc, p.reviews_count desc, l.id
                        limit :limit
                        """)
                .param("orgId", orgId.value())
                .param("limit", limit)
                .query(this::mapView)
                .list();
    }

    /**
     * Every lead of the organization, non-excluded first by score, then reviews, then id, excluded last.
     * The rank is the position among the non-excluded leads and is null for the excluded.
     */
    public List<RankedLead> ranked(OrgId orgId, int limit) {
        return jdbc.sql("with v as (" + SELECT_VIEW + " where c.org_id = :orgId) "
                        + """
                        select v.*,
                               case when v.stage = 'EXCLUDED' then null
                                    else row_number() over (order by (v.stage = 'EXCLUDED'), v.score desc, v.reviews_count desc, v.id) end as rank
                        from v
                        order by (v.stage = 'EXCLUDED'), v.score desc, v.reviews_count desc, v.id
                        limit :limit
                        """)
                .param("orgId", orgId.value())
                .param("limit", limit)
                .query((rs, row) -> new RankedLead(mapView(rs, row), rs.getObject("rank") == null ? null : rs.getInt("rank")))
                .list();
    }

    public record RankedLead(LeadView lead, Integer rank) {
    }

    /** A lead of another organization is not found, the same as one that does not exist. */
    public Optional<LeadView> findById(OrgId orgId, long id) {
        return jdbc.sql(SELECT_VIEW + " where c.org_id = :orgId and l.id = :id")
                .param("orgId", orgId.value())
                .param("id", id)
                .query(this::mapView)
                .optional();
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

    /** Stores the pitch written for a lead and the model that wrote it. */
    public void savePitch(long leadId, String pitch, String model) {
        jdbc.sql("""
                        update lead
                        set pitch = :pitch, pitch_model = :model, updated_at = now()
                        where id = :id
                        """)
                .param("id", leadId)
                .param("pitch", pitch)
                .param("model", model)
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
    public LeadView updateOutcome(OrgId orgId, long id, LeadStatus status, LostReason lostReason, String note) {
        LeadView current = findById(orgId, id)
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
        return findById(orgId, id).orElseThrow();
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

    /**
     * Every lead of the campaign, how many stage 1 excluded, how many are qualified and how many of those were
     * enriched. A lead a later scrape moved below the cut keeps its {@code enriched_at}, so it only counts while
     * qualified. A lead a later scrape excluded loses it.
     */
    public FunnelCounts funnelCounts(long campaignId) {
        return jdbc.sql("""
                        select count(*) as total,
                               count(*) filter (where stage = 'EXCLUDED') as excluded,
                               count(*) filter (where stage = 'QUALIFIED') as qualified,
                               count(*) filter (where stage = 'QUALIFIED' and enriched_at is not null) as enriched
                        from lead where campaign_id = :campaignId
                        """)
                .param("campaignId", campaignId)
                .query((rs, row) -> new FunnelCounts(rs.getInt("total"), rs.getInt("excluded"),
                        rs.getInt("qualified"), rs.getInt("enriched")))
                .single();
    }

    public record FunnelCounts(int total, int excluded, int qualified, int enriched) {
    }

    /** Why stage 1 dropped leads, most common reason first. */
    public List<ExclusionCount> topExclusions(long campaignId, int limit) {
        return jdbc.sql("""
                        select stage_reason as reason, count(*) as leads
                        from lead
                        where campaign_id = :campaignId and stage = 'EXCLUDED' and stage_reason is not null
                        group by stage_reason
                        order by count(*) desc, stage_reason
                        limit :limit
                        """)
                .param("campaignId", campaignId)
                .param("limit", limit)
                .query((rs, row) -> new ExclusionCount(rs.getString("reason"), rs.getInt("leads")))
                .list();
    }

    public record ExclusionCount(String reason, int leads) {
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
                rs.getString("maps_url"),
                complaintKinds(rs),
                rs.getString("pitch"));
    }

    private static List<String> complaintKinds(ResultSet rs) throws SQLException {
        return List.of((String[]) rs.getArray("complaint_kinds").getArray());
    }
}
