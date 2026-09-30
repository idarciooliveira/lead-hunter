package me.iofdev.leadhunter.pipeline;

import java.util.List;

import me.iofdev.leadhunter.place.WebsiteKind;
import me.iofdev.leadhunter.scoring.ScoreItem;

/** One qualified lead waiting for stage 2 enrichment, with what the runner needs to process it. */
public record EnrichmentTarget(long leadId, long placeId, String name, String website, WebsiteKind websiteKind,
                               String mapsUrl, int stage1Score, List<ScoreItem> stage1Breakdown) {
}
