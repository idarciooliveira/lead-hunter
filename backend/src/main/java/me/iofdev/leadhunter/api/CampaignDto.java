package me.iofdev.leadhunter.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import me.iofdev.leadhunter.campaign.Campaign;
import me.iofdev.leadhunter.campaign.CampaignFile;
import me.iofdev.leadhunter.pipeline.CampaignFunnel.Funnel;

/**
 * A campaign as the web client reads it. Same validation and content as the CLI,
 * plus the qualified lead count and the newest real job (never a dry run), null
 * when the campaign never ran, so the UI can show its state without computing it.
 * {@code funnel} is the pipeline counts (ADR 0048); only the single-campaign endpoints fill it.
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
        RunDto latestRun,
        Funnel funnel) {

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
                latestRun,
                null);
    }

    CampaignDto withFunnel(Funnel funnel) {
        return new CampaignDto(id, slug, name, answers, search, createdAt, totalCostUsd, qualifiedCount, latestRun,
                funnel);
    }
}
