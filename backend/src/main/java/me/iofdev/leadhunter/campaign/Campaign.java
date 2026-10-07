package me.iofdev.leadhunter.campaign;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import me.iofdev.leadhunter.auth.OrgId;

public record Campaign(
        long id,
        String slug,
        String name,
        CampaignFile.Answers answers,
        CampaignFile.Search search,
        OffsetDateTime createdAt,
        BigDecimal totalCostUsd,
        OrgId orgId) {
}
