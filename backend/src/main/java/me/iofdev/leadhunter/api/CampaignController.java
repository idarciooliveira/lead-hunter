package me.iofdev.leadhunter.api;

import java.util.List;

import me.iofdev.leadhunter.campaign.Campaign;
import me.iofdev.leadhunter.campaign.CampaignRepository;
import me.iofdev.leadhunter.pipeline.LeadRepository;
import me.iofdev.leadhunter.pipeline.RunRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read endpoints for campaigns. Controllers hold no business rules (ADR 0031). */
@RestController
@RequestMapping("/api/campaigns")
class CampaignController {

    private final CampaignRepository campaigns;
    private final LeadRepository leads;
    private final RunRepository runs;

    CampaignController(CampaignRepository campaigns, LeadRepository leads, RunRepository runs) {
        this.campaigns = campaigns;
        this.leads = leads;
        this.runs = runs;
    }

    @GetMapping
    List<CampaignDto> list() {
        return campaigns.findAll().stream().map(this::dto).toList();
    }

    @GetMapping("/{slug}")
    CampaignDto get(@PathVariable String slug) {
        return campaigns.findBySlug(slug)
                .map(this::dto)
                .orElseThrow(() -> new IllegalArgumentException("no campaign '" + slug + "'. Run: campaign list"));
    }

    private CampaignDto dto(Campaign campaign) {
        return CampaignDto.from(
                campaign,
                leads.countByStage(campaign.id()).qualified(),
                runs.latestJob(campaign.id()).map(RunDto::from).orElse(null));
    }
}
