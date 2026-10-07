package me.iofdev.leadhunter.api;

import java.time.LocalDate;
import java.util.List;

import me.iofdev.leadhunter.auth.OrgId;
import me.iofdev.leadhunter.campaign.Campaign;
import me.iofdev.leadhunter.campaign.CampaignChecks;
import me.iofdev.leadhunter.campaign.CampaignFile;
import me.iofdev.leadhunter.campaign.CampaignFileParser;
import me.iofdev.leadhunter.campaign.CampaignRepository;
import me.iofdev.leadhunter.company.CompanyProfile;
import me.iofdev.leadhunter.company.CompanyRepository;
import me.iofdev.leadhunter.pipeline.LeadRepository;
import me.iofdev.leadhunter.pipeline.RunRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read and write endpoints for campaigns. Controllers hold no business rules (ADR 0031). */
@RestController
@RequestMapping("/api/campaigns")
class CampaignController {

    private final CampaignRepository campaigns;
    private final LeadRepository leads;
    private final RunRepository runs;
    private final CompanyRepository company;

    CampaignController(CampaignRepository campaigns, LeadRepository leads, RunRepository runs,
                       CompanyRepository company) {
        this.campaigns = campaigns;
        this.leads = leads;
        this.runs = runs;
        this.company = company;
    }

    @GetMapping
    List<CampaignDto> list(OrgId org) {
        return campaigns.findAll(org).stream().map(this::dto).toList();
    }

    @GetMapping("/{slug}")
    CampaignDto get(OrgId org, @PathVariable String slug) {
        return campaigns.findBySlug(org, slug)
                .map(this::dto)
                .orElseThrow(() -> new IllegalArgumentException("no campaign '" + slug + "'. Run: campaign list"));
    }

    /** Creates a campaign, like {@code campaign create -f}. An existing slug is a 409; use PUT to change it. */
    @PostMapping
    ResponseEntity<SaveResult<CampaignDto>> create(OrgId org, @RequestBody CampaignFile body) {
        CampaignFileParser.validate(body);
        if (campaigns.findBySlug(org, body.slug()).isPresent()) {
            throw new CampaignExistsException("campaign '" + body.slug() + "' already exists. Update it instead");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(save(org, body));
    }

    /** Replaces a campaign's answers and search. The slug in the path wins; a different slug in the body is a 400. */
    @PutMapping("/{slug}")
    SaveResult<CampaignDto> update(OrgId org, @PathVariable String slug, @RequestBody CampaignFile body) {
        campaigns.findBySlug(org, slug)
                .orElseThrow(() -> new IllegalArgumentException("no campaign '" + slug + "'. Run: campaign list"));
        if (body.slug() != null && !body.slug().equals(slug)) {
            throw new IllegalArgumentException("slug cannot change: '" + slug + "' in the path, '" + body.slug()
                    + "' in the body");
        }
        CampaignFile file = new CampaignFile(slug, body.name(), body.answers(), body.search());
        CampaignFileParser.validate(file);
        return save(org, file);
    }

    private SaveResult<CampaignDto> save(OrgId org, CampaignFile file) {
        CompanyProfile profile = company.find(org).orElseThrow(() -> new IllegalArgumentException(
                "no company profile yet. Save it first"));
        CampaignChecks.requireFits(file, profile);
        List<String> warnings =
                CampaignChecks.warnings(file, profile, campaigns.findAll(org), LocalDate.now());
        campaigns.save(org, file);
        return new SaveResult<>(get(org, file.slug()), warnings);
    }

    private CampaignDto dto(Campaign campaign) {
        return CampaignDto.from(
                campaign,
                leads.countByStage(campaign.id()).qualified(),
                runs.latestJob(campaign.id()).map(RunDto::from).orElse(null));
    }
}
