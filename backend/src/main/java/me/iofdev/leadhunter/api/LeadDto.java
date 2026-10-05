package me.iofdev.leadhunter.api;

import java.math.BigDecimal;
import java.util.List;

import me.iofdev.leadhunter.pipeline.LeadView;
import me.iofdev.leadhunter.scoring.ScoreItem;

/** A ranked lead as the web client reads it. Scores and reasons come from the API (ADR 0007). */
public record LeadDto(
        long id,
        String campaignSlug,
        String stage,
        String status,
        String lostReason,
        String note,
        int score,
        List<ScoreItem> breakdown,
        String stageReason,
        String name,
        String category,
        String address,
        String neighborhood,
        String phoneE164,
        boolean phoneMobile,
        String website,
        String websiteKind,
        BigDecimal rating,
        int reviewsCount,
        String mapsUrl,
        String whatsappLink) {

    static LeadDto from(LeadView lead) {
        return new LeadDto(
                lead.id(),
                lead.campaignSlug(),
                lead.stage().name(),
                lead.status().name(),
                lead.lostReason() == null ? null : lead.lostReason().name(),
                lead.outcomeNote(),
                lead.score(),
                lead.breakdown(),
                lead.stageReason(),
                lead.name(),
                lead.category(),
                lead.address(),
                lead.neighborhood(),
                lead.phoneE164(),
                lead.phoneMobile(),
                lead.website(),
                lead.websiteKind().name(),
                lead.rating(),
                lead.reviewsCount(),
                lead.mapsUrl(),
                lead.whatsappLink());
    }
}
