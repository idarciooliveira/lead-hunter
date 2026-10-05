package me.iofdev.leadhunter.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import me.iofdev.leadhunter.campaign.Campaign;
import me.iofdev.leadhunter.campaign.CampaignFile;

/**
 * A campaign as the web client reads it. Same validation and content as the CLI,
 * plus the qualified lead count and the newest real job (never a dry run), null
 * when the campaign never ran, so the UI can show its state without computing it.
 */
public record CampaignDto(
        long id,
        String slug,
        String name,
        CampaignFile.Answers answers,
        CampaignFile.Search search,
        OffsetDateTime createdAt,
        BigDecimal totalCostUsd,
        int qualifiedCount,
        RunDto latestRun) {

    static CampaignDto from(Campaign campaign, int qualifiedCount, RunDto latestRun) {
        return new CampaignDto(
                campaign.id(),
                campaign.slug(),
                campaign.name(),
                campaign.answers(),
                campaign.search(),
                campaign.createdAt(),
                campaign.totalCostUsd(),
                qualifiedCount,
                latestRun);
    }
}
