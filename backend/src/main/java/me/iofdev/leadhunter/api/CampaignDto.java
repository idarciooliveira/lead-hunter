package me.iofdev.leadhunter.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import me.iofdev.leadhunter.campaign.Campaign;
import me.iofdev.leadhunter.campaign.CampaignFile;

/** A campaign as the web client reads it. Same validation and content as the CLI. */
public record CampaignDto(
        long id,
        String slug,
        String name,
        CampaignFile.Answers answers,
        CampaignFile.Search search,
        OffsetDateTime createdAt,
        BigDecimal totalCostUsd) {

    static CampaignDto from(Campaign campaign) {
        return new CampaignDto(
                campaign.id(),
                campaign.slug(),
                campaign.name(),
                campaign.answers(),
                campaign.search(),
                campaign.createdAt(),
                campaign.totalCostUsd());
    }
}
