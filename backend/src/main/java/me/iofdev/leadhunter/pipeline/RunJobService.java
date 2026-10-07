package me.iofdev.leadhunter.pipeline;

import java.math.BigDecimal;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongFunction;

import me.iofdev.leadhunter.campaign.Campaign;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Runs the pipeline as jobs (ADR 0033, 0036): the CLI in the foreground, the
 * web client on a bounded executor it polls. Either way the job holds a parent
 * row and its lease from start to verdict, which is what keeps one job per
 * campaign. Budget and nothing-to-do rejections happen before any row is
 * written.
 */
@Service
public class RunJobService {

    private static final Consumer<String> SILENT = message -> {
    };

    public record ScrapePreview(SearchPlan plan, boolean overLimit, long dryRunId) {
    }

    public record EnrichPreview(int pending, int batch, int maxReviews, long dryRunId) {
    }

    /** How a job closes: {@code error} null means it succeeded with {@code done}. */
    private record Verdict(long done, String error) {
    }

    private final CampaignRunner scrape;
    private final EnrichmentRunner enrich;
    private final RunRepository runs;
    private final LeadRepository leads;
    private final BudgetService budget;
    private final EnrichmentProperties enrichment;
    private final TaskExecutor jobs;

    public RunJobService(CampaignRunner scrape, EnrichmentRunner enrich, RunRepository runs,
                         LeadRepository leads, BudgetService budget, EnrichmentProperties enrichment,
                         @Qualifier("runJobs") TaskExecutor jobs) {
        this.scrape = scrape;
        this.enrich = enrich;
        this.runs = runs;
        this.leads = leads;
        this.budget = budget;
        this.enrichment = enrichment;
        this.jobs = jobs;
    }

    /** A free estimate that mirrors {@code campaign run --dry-run} and costs nothing. */
    public ScrapePreview previewScrape(Campaign campaign) {
        SearchPlan plan = scrape.plan(campaign);
        long dryRunId = runs.recordDryRun(campaign.id(), plan.maxPlaces(), null);
        return new ScrapePreview(plan, scrape.overLimit(plan), dryRunId);
    }

    /** {@code campaign run}: the job runs on the calling thread and its summary comes back. */
    public RunSummary runScrape(Campaign campaign, boolean allowOverLimit, Consumer<String> progress) {
        SearchPlan plan = scrape.plan(campaign);
        scrape.checkOverLimit(plan, allowOverLimit);
        return runJob(begin(campaign, RunRepository.KIND_SCRAPE, null, plan.estimatedMaxUsd()),
                jobId -> scrape.run(campaign, allowOverLimit, progress, jobId), RunJobService::scrapeVerdict);
    }

    public long startScrape(Campaign campaign, boolean allowOverLimit) {
        SearchPlan plan = scrape.plan(campaign);
        scrape.checkOverLimit(plan, allowOverLimit);
        return submit(begin(campaign, RunRepository.KIND_SCRAPE, null, plan.estimatedMaxUsd()),
                jobId -> scrape.run(campaign, allowOverLimit, SILENT, jobId), RunJobService::scrapeVerdict);
    }

    /** A free estimate that mirrors {@code campaign enrich --dry-run}: only counts, never touches a lead. */
    public EnrichPreview previewEnrichment(Campaign campaign, int batchSize, int maxReviews) {
        int pending = leads.countUnenrichedQualified(campaign.id());
        long dryRunId = runs.recordDryRun(campaign.id(), Math.min(pending, batchSize), pending);
        return new EnrichPreview(pending, batchSize, maxReviews, dryRunId);
    }

    /** {@code campaign enrich}, after it checked there is something to enrich. */
    public EnrichmentSummary runEnrichment(Campaign campaign, int pending, int batchSize, int maxReviews,
                                           Consumer<String> progress) {
        int leadCount = Math.min(pending, batchSize);
        return runJob(begin(campaign, RunRepository.KIND_ENRICH, leadCount, enrichment.estimateJobUsd(leadCount, maxReviews)),
                jobId -> enrich.enrich(campaign, batchSize, maxReviews, progress, jobId), RunJobService::enrichVerdict);
    }

    public long startEnrichment(Campaign campaign, int batchSize, int maxReviews) {
        int pending = leads.countUnenrichedQualified(campaign.id());
        if (pending == 0) {
            throw new IllegalArgumentException("nothing to enrich: every qualified lead of '"
                    + campaign.slug() + "' is already enriched.");
        }
        int leadCount = Math.min(pending, batchSize);
        return submit(begin(campaign, RunRepository.KIND_ENRICH, leadCount, enrichment.estimateJobUsd(leadCount, maxReviews)),
                jobId -> enrich.enrich(campaign, batchSize, maxReviews, SILENT, jobId), RunJobService::enrichVerdict);
    }

    private static Verdict scrapeVerdict(RunSummary summary) {
        return summary.failedRuns() == summary.scraperRuns()
                ? new Verdict(summary.placesFound(), CampaignRunner.ALL_FAILED)
                : new Verdict(summary.placesFound(), null);
    }

    private static Verdict enrichVerdict(EnrichmentSummary summary) {
        return new Verdict(summary.enriched(), null);
    }

    /**
     * Opens the job's parent row and lease. Jobs whose process died are
     * failed first, so a killed run never blocks its campaign and its reserved
     * estimate does not count against the budget. The budget check comes next
     * and refuses before any row is written (ADR 0044). The unique index behind
     * {@link RunRepository#startJob} settles two starts that race.
     */
    private JobLease begin(Campaign campaign, String kind, Integer total, BigDecimal estimatedUsd) {
        runs.failAbandoned(campaign.id());
        budget.check(campaign.orgId(), estimatedUsd);
        try {
            return runs.startJob(campaign.id(), kind, total, estimatedUsd);
        } catch (DataIntegrityViolationException e) {
            throw new AlreadyRunningException("campaign '" + campaign.slug() + "' already has a running job");
        }
    }

    /** Runs the job on the executor. A full queue fails the job at once, so it never blocks its campaign. */
    private <T> long submit(JobLease lease, LongFunction<T> work, Function<T, Verdict> verdict) {
        try {
            jobs.execute(() -> {
                try {
                    runJob(lease, work, verdict);
                } catch (RuntimeException e) {
                    // Recorded on the job row, which is what the client polls.
                }
            });
        } catch (TaskRejectedException e) {
            runs.failJob(lease.jobId(), "too many jobs waiting");
            lease.close();
            throw e;
        }
        return lease.jobId();
    }

    /** Runs one job to its verdict and closes its row and lease, whatever the work throws. */
    private <T> T runJob(JobLease lease, LongFunction<T> work, Function<T, Verdict> verdict) {
        try (lease) {
            T result;
            try {
                result = work.apply(lease.jobId());
            } catch (Throwable e) {
                runs.failJob(lease.jobId(), e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
                throw e;
            }
            Verdict closing = verdict.apply(result);
            if (closing.error() == null) {
                runs.finishJob(lease.jobId(), closing.done());
            } else {
                runs.failJob(lease.jobId(), closing.error());
            }
            return result;
        }
    }
}
