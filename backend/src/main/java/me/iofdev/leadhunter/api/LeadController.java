package me.iofdev.leadhunter.api;

import java.util.List;
import java.util.Optional;

import me.iofdev.leadhunter.campaign.CampaignRepository;
import me.iofdev.leadhunter.pipeline.LeadRepository;
import me.iofdev.leadhunter.pipeline.LeadStage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    LeadController(CampaignRepository campaigns, LeadRepository leads) {
        this.campaigns = campaigns;
        this.leads = leads;
    }

    @GetMapping("/campaigns/{slug}/leads")
    List<LeadDto> list(
            @PathVariable String slug,
            @RequestParam(defaultValue = "QUALIFIED") String stage,
            @RequestParam(defaultValue = "" + DEFAULT_LIMIT) int limit) {
        long campaignId = campaigns.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("no campaign '" + slug + "'. Run: campaign list"))
                .id();
        Optional<LeadStage> filter = stageFilter(stage);
        return leads.list(campaignId, filter, Math.clamp(limit, 1, MAX_LIMIT)).stream()
                .map(LeadDto::from)
                .toList();
    }

    @GetMapping("/leads/{id}")
    LeadDto get(@PathVariable long id) {
        return leads.findById(id).map(LeadDto::from)
                .orElseThrow(() -> new IllegalArgumentException("no lead with id " + id));
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
