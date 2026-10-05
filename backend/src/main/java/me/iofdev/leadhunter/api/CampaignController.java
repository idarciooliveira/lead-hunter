package me.iofdev.leadhunter.api;

import java.util.List;

import me.iofdev.leadhunter.campaign.CampaignRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read endpoints for campaigns. Controllers hold no business rules (ADR 0031). */
@RestController
@RequestMapping("/api/campaigns")
class CampaignController {

    private final CampaignRepository campaigns;

    CampaignController(CampaignRepository campaigns) {
        this.campaigns = campaigns;
    }

    @GetMapping
    List<CampaignDto> list() {
        return campaigns.findAll().stream().map(CampaignDto::from).toList();
    }

    @GetMapping("/{slug}")
    CampaignDto get(@PathVariable String slug) {
        return campaigns.findBySlug(slug)
                .map(CampaignDto::from)
                .orElseThrow(() -> new IllegalArgumentException("no campaign '" + slug + "'. Run: campaign list"));
    }
}
