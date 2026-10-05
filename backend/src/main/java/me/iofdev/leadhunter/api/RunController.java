package me.iofdev.leadhunter.api;

import java.util.List;

import me.iofdev.leadhunter.campaign.Campaign;
import me.iofdev.leadhunter.campaign.CampaignRepository;
import me.iofdev.leadhunter.pipeline.EnrichmentProperties;
import me.iofdev.leadhunter.pipeline.RunJobService;
import me.iofdev.leadhunter.pipeline.RunRepository;
import me.iofdev.leadhunter.pipeline.RunRepository.JobView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Starts runs from the UI as background jobs the client polls (ADR 0033).
 * Same runners and rules as the CLI; the controller only maps HTTP to jobs.
 */
@RestController
@RequestMapping("/api")
class RunController {

    private final CampaignRepository campaigns;
    private final RunRepository runs;
    private final RunJobService jobs;
    private final EnrichmentProperties enrichment;

    RunController(CampaignRepository campaigns, RunRepository runs, RunJobService jobs,
                  EnrichmentProperties enrichment) {
        this.campaigns = campaigns;
        this.runs = runs;
        this.jobs = jobs;
        this.enrichment = enrichment;
    }

    @GetMapping("/campaigns/{slug}/runs")
    List<RunDto> history(@PathVariable String slug) {
        return runs.listJobs(requireCampaign(slug).id()).stream()
                .map(RunDto::from)
                .toList();
    }

    @GetMapping("/runs/{id}")
    RunDto get(@PathVariable long id) {
        return runs.findJob(id)
                .map(RunDto::from)
                .orElseThrow(() -> new IllegalArgumentException("no run with id " + id));
    }

    /**
     * Starts a scrape job: 202 with the job, which the client polls. With
     * {@code dryRun} it returns the free estimate instead, like
     * {@code campaign run --dry-run}. {@code allowOverLimit} is the UI's
     * explicit opt-in past the campaign limit.
     */
    @PostMapping("/campaigns/{slug}/runs")
    ResponseEntity<?> startScrape(
            @PathVariable String slug,
            @RequestParam(defaultValue = "false") boolean dryRun,
            @RequestParam(defaultValue = "false") boolean allowOverLimit) {
        Campaign campaign = requireCampaign(slug);
        if (dryRun) {
            return ResponseEntity.ok(RunPlanDto.Scrape.from(jobs.previewScrape(campaign)));
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(RunDto.from(job(jobs.startScrape(campaign, allowOverLimit))));
    }

    /**
     * Starts an enrichment job, same shape as a scrape: 202 with the job, or
     * the free estimate with {@code dryRun}. Batch and review counts default
     * like {@code campaign enrich}.
     */
    @PostMapping("/campaigns/{slug}/enrichment")
    ResponseEntity<?> startEnrichment(
            @PathVariable String slug,
            @RequestParam(required = false) Integer batchSize,
            @RequestParam(required = false) Integer maxReviews,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        Campaign campaign = requireCampaign(slug);
        int batch = batchSize == null ? enrichment.batch() : batchSize;
        int reviews = maxReviews == null ? enrichment.maxReviews() : maxReviews;
        if (batch < 1) {
            throw new IllegalArgumentException("batchSize must be at least 1");
        }
        if (reviews < 0) {
            throw new IllegalArgumentException("maxReviews must be at least 0");
        }
        if (dryRun) {
            return ResponseEntity.ok(RunPlanDto.Enrich.from(jobs.previewEnrichment(campaign, batch, reviews)));
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(RunDto.from(job(jobs.startEnrichment(campaign, batch, reviews))));
    }

    private Campaign requireCampaign(String slug) {
        return campaigns.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("no campaign '" + slug + "'. Run: campaign list"));
    }

    private JobView job(long jobId) {
        return runs.findJob(jobId).orElseThrow(() -> new IllegalStateException("job " + jobId + " just started is gone"));
    }
}
