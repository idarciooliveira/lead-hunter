package me.iofdev.leadhunter.pipeline;

import java.math.BigDecimal;

public record RunSummary(
        int scraperRuns,
        int failedRuns,
        int placesFound,
        int newLeads,
        int excludedThisRun,
        LeadRepository.StageCounts totals,
        BigDecimal costUsd) {
}
