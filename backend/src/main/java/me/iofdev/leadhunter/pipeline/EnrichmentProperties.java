package me.iofdev.leadhunter.pipeline;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param batch                  qualified leads enriched per {@code campaign enrich} run. Small on purpose:
 *                               each lead costs crawl time and, with reviews, Apify credit and LLM calls.
 * @param maxReviews             recent reviews fetched per place for the complaint classification
 * @param estimatedUsdPerReview  what the reviews actor charges per review; only the budget check reads it (ADR 0044)
 * @param estimatedLlmUsdPerLead what one lead costs in LLM calls, the complaint classification plus the pitch;
 *                               only the budget check reads it
 */
@ConfigurationProperties("leadhunter.stage2")
public record EnrichmentProperties(
        @DefaultValue("25") int batch,
        @DefaultValue("10") int maxReviews,
        @DefaultValue("0.0005") BigDecimal estimatedUsdPerReview,
        @DefaultValue("0.002") BigDecimal estimatedLlmUsdPerLead) {

    /** A zero or negative estimate would let a chargeable enrichment through the budget check. */
    public EnrichmentProperties {
        requirePositive("estimated-usd-per-review", estimatedUsdPerReview);
        requirePositive("estimated-llm-usd-per-lead", estimatedLlmUsdPerLead);
    }

    private static void requirePositive(String name, BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException("leadhunter.stage2." + name + " must be more than 0, got " + value);
        }
    }

    /** The Apify cost of one review batch over {@code places} places. */
    public BigDecimal estimateReviewsUsd(int places, int maxReviews) {
        return estimatedUsdPerReview.multiply(BigDecimal.valueOf((long) places * maxReviews));
    }

    /** The most one enrichment job over {@code leads} leads can cost: reviews plus LLM calls. */
    public BigDecimal estimateJobUsd(int leads, int maxReviews) {
        return estimateReviewsUsd(leads, maxReviews).add(estimatedLlmUsdPerLead.multiply(BigDecimal.valueOf(leads)));
    }
}
