package me.iofdev.leadhunter.pipeline;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import me.iofdev.leadhunter.campaign.Campaign;
import me.iofdev.leadhunter.pipeline.LeadRepository.ExclusionCount;
import me.iofdev.leadhunter.pipeline.LeadRepository.FunnelCounts;
import me.iofdev.leadhunter.pipeline.RunRepository.JobView;
import org.springframework.stereotype.Component;

/**
 * The pipeline of ADR 0006 as counts for one campaign: scraped, kept by the filter, qualified by the
 * cut and enriched, with what the scrape and the enrichment cost. Everything is read from the stored
 * leads and jobs, so it needs no storage of its own (ADR 0048).
 */
@Component
public class CampaignFunnel {

    private static final int TOP_EXCLUSIONS = 3;

    private final LeadRepository leads;
    private final RunRepository runs;

    public CampaignFunnel(LeadRepository leads, RunRepository runs) {
        this.leads = leads;
        this.runs = runs;
    }

    /**
     * The counts, or empty when the campaign never ran. Costs add up the parts of each job whose price
     * is known, so a job with one unpriced part still counts the others.
     */
    public Optional<Funnel> of(Campaign campaign) {
        List<JobView> jobs = runs.listJobs(campaign.id()).stream()
                .filter(j -> !RunRepository.KIND_DRY_RUN.equals(j.kind()))
                .toList();
        if (jobs.isEmpty()) {
            return Optional.empty();
        }
        FunnelCounts counts = leads.funnelCounts(campaign.id());
        return Optional.of(new Funnel(
                counts.total(),
                cost(jobs, RunRepository.KIND_SCRAPE),
                counts.total() - counts.excluded(),
                counts.excluded(),
                leads.topExclusions(campaign.id(), TOP_EXCLUSIONS),
                campaign.search().qualifyShare(),
                counts.qualified(),
                counts.enriched(),
                cost(jobs, RunRepository.KIND_ENRICH),
                jobs.stream().anyMatch(j -> RunRepository.KIND_ENRICH.equals(j.kind()) && "RUNNING".equals(j.status()))));
    }

    private static BigDecimal cost(List<JobView> jobs, String kind) {
        return jobs.stream()
                .filter(j -> kind.equals(j.kind()))
                .map(JobView::knownCostUsd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * {@code scraped} counts the distinct places the campaign found, so a place two searches return
     * counts once. {@code excludedBy} holds the most common reasons stage 1 dropped leads.
     */
    public record Funnel(
            int scraped,
            BigDecimal scrapeCostUsd,
            int kept,
            int excluded,
            List<ExclusionCount> excludedBy,
            double cutShare,
            int qualified,
            int enriched,
            BigDecimal enrichCostUsd,
            boolean enriching) {
    }
}
