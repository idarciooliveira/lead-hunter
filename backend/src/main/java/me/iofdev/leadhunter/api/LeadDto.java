package me.iofdev.leadhunter.api;

import java.math.BigDecimal;
import java.util.List;

import me.iofdev.leadhunter.pipeline.LeadView;
import me.iofdev.leadhunter.place.CrawlRepository.StoredCrawl;
import me.iofdev.leadhunter.scoring.ScoreItem;
import me.iofdev.leadhunter.scoring.Stage2Scorer;

/**
 * A ranked lead as the web client reads it. Scores and reasons come from the API (ADR 0007).
 * {@code breakdown} holds the stage 1 items and {@code stage2Breakdown} the stage 2 ones, null until
 * enrichment ran. {@code websiteCrawl} is the newest crawl of the current website, and only the single-lead
 * endpoints fill it.
 */
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
        List<String> complaintKinds,
        String pitch,
        String whatsappLink,
        List<ScoreItem> stage2Breakdown,
        StoredCrawl websiteCrawl) {

    static LeadDto from(LeadView lead) {
        List<ScoreItem> stage2 = lead.breakdown().stream().filter(i -> Stage2Scorer.isStage2Code(i.code())).toList();
        List<ScoreItem> stage1 = lead.breakdown().stream().filter(i -> !Stage2Scorer.isStage2Code(i.code())).toList();
        return new LeadDto(
                lead.id(),
                lead.campaignSlug(),
                lead.stage().name(),
                lead.status().name(),
                lead.lostReason() == null ? null : lead.lostReason().name(),
                lead.outcomeNote(),
                lead.score(),
                stage1,
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
                lead.complaintKinds(),
                lead.pitch(),
                lead.whatsappLink(),
                stage2.isEmpty() ? null : stage2,
                null);
    }

    LeadDto withCrawl(StoredCrawl crawl) {
        return new LeadDto(id, campaignSlug, stage, status, lostReason, note, score, breakdown, stageReason, name,
                category, address, neighborhood, phoneE164, phoneMobile, website, websiteKind, rating, reviewsCount,
                mapsUrl, complaintKinds, pitch, whatsappLink, stage2Breakdown, crawl);
    }
}
