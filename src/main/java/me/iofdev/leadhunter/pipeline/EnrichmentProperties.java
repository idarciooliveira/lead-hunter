package me.iofdev.leadhunter.pipeline;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param batch     qualified leads enriched per {@code campaign enrich} run. Small on purpose:
 *                  each lead costs crawl time and, with reviews, Apify credit and LLM calls.
 * @param maxReviews recent reviews fetched per place for the complaint classification
 */
@ConfigurationProperties("leadhunter.stage2")
public record EnrichmentProperties(
        @DefaultValue("25") int batch,
        @DefaultValue("10") int maxReviews) {
}
