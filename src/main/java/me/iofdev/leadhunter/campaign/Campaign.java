package me.iofdev.leadhunter.campaign;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record Campaign(
        long id,
        String slug,
        String name,
        CampaignFile.Answers answers,
        CampaignFile.Search search,
        OffsetDateTime createdAt,
        BigDecimal totalCostUsd) {
}
