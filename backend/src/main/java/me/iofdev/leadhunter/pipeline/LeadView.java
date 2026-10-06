package me.iofdev.leadhunter.pipeline;

import java.math.BigDecimal;
import java.util.List;

import me.iofdev.leadhunter.place.WebsiteKind;
import me.iofdev.leadhunter.scoring.ScoreItem;

/** A lead joined with its place, ready to print. */
public record LeadView(
        long id,
        String campaignSlug,
        LeadStage stage,
        LeadStatus status,
        LostReason lostReason,
        String outcomeNote,
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
        WebsiteKind websiteKind,
        BigDecimal rating,
        int reviewsCount,
        String mapsUrl,
        List<String> complaintKinds,
        String pitch) {

    public String whatsappLink() {
        return phoneMobile && phoneE164 != null ? "https://wa.me/" + phoneE164.substring(1) : null;
    }
}
