package me.iofdev.leadhunter.api;

import java.util.List;
import java.util.Optional;

import me.iofdev.leadhunter.auth.OrgId;
import me.iofdev.leadhunter.company.CompanyRepository;
import me.iofdev.leadhunter.pipeline.LeadCsv;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.nio.charset.StandardCharsets;

import me.iofdev.leadhunter.campaign.CampaignRepository;
import me.iofdev.leadhunter.pipeline.LeadRepository;
import me.iofdev.leadhunter.pipeline.LeadStage;
import me.iofdev.leadhunter.pipeline.LeadView;
import me.iofdev.leadhunter.pipeline.LeadStatus;
import me.iofdev.leadhunter.pipeline.LostReason;
import me.iofdev.leadhunter.pipeline.PitchService;
import me.iofdev.leadhunter.place.CrawlRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ranked leads, best first. Same ordering and filters as {@code leads list}:
 * {@code stage} defaults to QUALIFIED, ALL removes the filter.
 */
@RestController
@RequestMapping("/api")
class LeadController {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 200;

    private final CampaignRepository campaigns;
    private final LeadRepository leads;
    private final PitchService pitches;
    private final CompanyRepository company;
    private final CrawlRepository crawls;

    LeadController(CampaignRepository campaigns, LeadRepository leads, PitchService pitches,
            CompanyRepository company, CrawlRepository crawls) {
        this.crawls = crawls;
        this.campaigns = campaigns;
        this.leads = leads;
        this.pitches = pitches;
        this.company = company;
    }

    /** Today's queue (ADR 0041), same as {@code leads today}. {@code limit} overrides the profile's daily size. */
    @GetMapping("/leads/today")
    List<LeadDto> today(OrgId org, @RequestParam(required = false) Integer limit) {
        int size = limit != null ? Math.clamp(limit, 1, MAX_LIMIT) : company.dailyQueueSize(org);
        return leads.today(org, size).stream().map(LeadDto::from).toList();
    }

    /** Every lead of the stage as a CSV file, in the order of {@code list} (ADR 0041). */
    @GetMapping(value = "/campaigns/{slug}/leads.csv", produces = "text/csv")
    ResponseEntity<byte[]> csv(OrgId org, @PathVariable String slug,
                          @RequestParam(defaultValue = "QUALIFIED") String stage) {
        long campaignId = campaigns.findBySlug(org, slug)
                .orElseThrow(() -> new IllegalArgumentException("no campaign '" + slug + "'. Run: campaign list"))
                .id();
        String body = LeadCsv.of(leads.list(campaignId, stageFilter(stage), Integer.MAX_VALUE));
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(slug + "-leads.csv").build().toString())
                .body(body.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/campaigns/{slug}/leads")
    List<LeadDto> list(
            OrgId org,
            @PathVariable String slug,
            @RequestParam(defaultValue = "QUALIFIED") String stage,
            @RequestParam(defaultValue = "" + DEFAULT_LIMIT) int limit) {
        long campaignId = campaigns.findBySlug(org, slug)
                .orElseThrow(() -> new IllegalArgumentException("no campaign '" + slug + "'. Run: campaign list"))
                .id();
        Optional<LeadStage> filter = stageFilter(stage);
        return leads.list(campaignId, filter, Math.clamp(limit, 1, MAX_LIMIT)).stream()
                .map(LeadDto::from)
                .toList();
    }

    @GetMapping("/leads/{id}")
    LeadDto get(OrgId org, @PathVariable long id) {
        return leads.findById(org, id).map(this::card)
                .orElseThrow(() -> new IllegalArgumentException("no lead with id " + id));
    }

    /**
     * Marks a contact outcome (ADR 0012, 0020). Same rules as {@code leads mark}:
     * LOST needs one of the five lost reasons, a lost reason needs LOST, and a
     * worked lead can never go back to NEW.
     */
    @PatchMapping("/leads/{id}")
    LeadDto mark(OrgId org, @PathVariable long id, @RequestBody MarkLeadRequest request) {
        if (request == null || request.status() == null) {
            throw new IllegalArgumentException("status is required: "
                    + "NEW, CONTACTED, NO_ANSWER, INTERESTED, MEETING, PROPOSAL_SENT, WON or LOST");
        }
        LeadStatus status = parseStatus(request.status());
        LostReason lostReason = request.lostReason() == null ? null : parseLostReason(request.lostReason());
        return card(leads.updateOutcome(org, id, status, lostReason, request.note()));
    }

    /** Writes a new pitch for the lead and replaces the old one (ADR 0040), like {@code leads pitch}. */
    @PostMapping("/leads/{id}/pitch")
    LeadDto pitch(OrgId org, @PathVariable long id) {
        return card(pitches.regenerate(org, id));
    }

    /** The single-lead shape: the list fields plus the newest website crawl. */
    private LeadDto card(LeadView lead) {
        return LeadDto.from(lead).withCrawl(crawls.latestForLead(lead.id()).orElse(null));
    }

    record MarkLeadRequest(String status, String lostReason, String note) {
    }

    private static LeadStatus parseStatus(String status) {
        try {
            return LeadStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown status '" + status + "'. Use NEW, CONTACTED, NO_ANSWER, "
                    + "INTERESTED, MEETING, PROPOSAL_SENT, WON or LOST");
        }
    }

    private static LostReason parseLostReason(String lostReason) {
        try {
            return LostReason.valueOf(lostReason.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown lost reason '" + lostReason + "'. Use NO_BUDGET, "
                    + "WRONG_PERSON, HAS_SUPPLIER, NOT_INTERESTED or NOT_NOW");
        }
    }

    private static Optional<LeadStage> stageFilter(String stage) {
        if (stage.equalsIgnoreCase("ALL")) {
            return Optional.empty();
        }
        try {
            return Optional.of(LeadStage.valueOf(stage.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown stage '" + stage + "'. Use QUALIFIED, BELOW_CUT, EXCLUDED or ALL");
        }
    }
}
