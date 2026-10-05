package me.iofdev.leadhunter.pipeline;

import me.iofdev.leadhunter.pipeline.LeadRepository.StageCounts;

/** What one {@code campaign enrich} run did. */
public record EnrichmentSummary(int considered, int enriched, StageCounts totals) {
}
