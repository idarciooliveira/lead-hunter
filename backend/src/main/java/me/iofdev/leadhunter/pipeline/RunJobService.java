package me.iofdev.leadhunter.pipeline;

import java.util.function.Consumer;

import me.iofdev.leadhunter.campaign.Campaign;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Starts pipeline runs as background jobs the web client polls (ADR 0033).
 * The runners do the work on a bounded executor; this service only opens the
 * parent row, submits the job, and closes the parent with the verdict. Budget
 * and nothing-to-do rejections happen before any row is written, like the CLI.
 */
@Service
public class RunJobService {

    private static final Consumer<String> SILENT = message -> {
    };

    public record ScrapePreview(SearchPlan plan, boolean overLimit, long dryRunId) {
    }

    public record EnrichPreview(int pending, int batch, int maxReviews, long dryRunId) {
    }

    private final CampaignRunner scrape;
    private final EnrichmentRunner enrich;
    private final RunRepository runs;
    private final LeadRepository leads;
    private final TaskExecutor jobs;

    public RunJobService(CampaignRunner scrape, EnrichmentRunner enrich, RunRepository runs,
                         LeadRepository leads, @Qualifier("runJobs") TaskExecutor jobs) {
        this.scrape = scrape;
        this.enrich = enrich;
        this.runs = runs;
        this.leads = leads;
        this.jobs = jobs;
    }

    /** A free estimate that mirrors {@code campaign run --dry-run} and costs nothing. */
    public ScrapePreview previewScrape(Campaign campaign) {
        SearchPlan plan = scrape.plan(campaign);
        long dryRunId = runs.recordDryRun(campaign.id(), plan.maxPlaces(), null);
        return new ScrapePreview(plan, scrape.overLimit(plan), dryRunId);
    }

    public long startScrape(Campaign campaign, boolean allowOverLimit) {
        checkIdle(campaign);
        SearchPlan plan = scrape.plan(campaign);
        scrape.checkOverLimit(plan, allowOverLimit);
        long jobId = begin(campaign, RunRepository.KIND_SCRAPE, null);
        jobs.execute(() -> {
            try {
                RunSummary summary = scrape.run(campaign, allowOverLimit, SILENT, jobId);
                if (summary.failedRuns() == summary.scraperRuns()) {
                    runs.failJob(jobId, CampaignRunner.ALL_FAILED);
                } else {
                    runs.finishJob(jobId, summary.placesFound());
                }
            } catch (RuntimeException e) {
                runs.failJob(jobId, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            }
        });
        return jobId;
    }

    /** A free estimate that mirrors {@code campaign enrich --dry-run}: only counts, never touches a lead. */
    public EnrichPreview previewEnrichment(Campaign campaign, int batchSize, int maxReviews) {
        int pending = leads.countUnenrichedQualified(campaign.id());
        long dryRunId = runs.recordDryRun(campaign.id(), Math.min(pending, batchSize), pending);
        return new EnrichPreview(pending, batchSize, maxReviews, dryRunId);
    }

    public long startEnrichment(Campaign campaign, int batchSize, int maxReviews) {
        checkIdle(campaign);
        int pending = leads.countUnenrichedQualified(campaign.id());
        if (pending == 0) {
            throw new IllegalArgumentException("nothing to enrich: every qualified lead of '"
                    + campaign.slug() + "' is already enriched.");
        }
        long jobId = begin(campaign, RunRepository.KIND_ENRICH, Math.min(pending, batchSize));
        jobs.execute(() -> {
            try {
                EnrichmentSummary summary = enrich.enrich(campaign, batchSize, maxReviews, SILENT, jobId);
                runs.finishJob(jobId, summary.enriched());
            } catch (RuntimeException e) {
                runs.failJob(jobId, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            }
        });
        return jobId;
    }

    /**
     * The friendly path of the one-job promise. The unique index behind
     * {@link #begin} is the backstop for two starts that race each other.
     */
    private void checkIdle(Campaign campaign) {
        if (runs.runningExists(campaign.id())) {
            throw new AlreadyRunningException("campaign '" + campaign.slug() + "' already has a running job");
        }
    }

    private long begin(Campaign campaign, String kind, Integer total) {
        checkIdle(campaign);
        try {
            return runs.startJob(campaign.id(), kind, total);
        } catch (DataIntegrityViolationException e) {
            // Lost a race with another start; the index kept the one-job promise.
            throw new AlreadyRunningException(
                    "campaign '" + campaign.slug() + "' already has a running job");
        }
    }
}
