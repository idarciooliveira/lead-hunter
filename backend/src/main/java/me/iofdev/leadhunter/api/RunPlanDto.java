package me.iofdev.leadhunter.api;

import java.math.BigDecimal;
import java.util.List;

import me.iofdev.leadhunter.maps.ScrapeRequest;
import me.iofdev.leadhunter.pipeline.RunJobService.EnrichPreview;
import me.iofdev.leadhunter.pipeline.RunJobService.ScrapePreview;

/** The free estimates behind the confirmation dialogs (ADR 0033). Nothing here spends a cent. */
public final class RunPlanDto {

    private RunPlanDto() {
    }

    public record ScrapeRequestDto(String location, List<String> terms, int maxPlaces) {
        static ScrapeRequestDto from(ScrapeRequest request) {
            return new ScrapeRequestDto(request.location(), request.terms(), request.maxPlaces());
        }
    }

    public record Scrape(List<ScrapeRequestDto> requests, int maxPlaces, BigDecimal estimatedMaxUsd,
                         boolean overLimit, long dryRunId) {
        static Scrape from(ScrapePreview preview) {
            return new Scrape(
                    preview.plan().requests().stream().map(ScrapeRequestDto::from).toList(),
                    preview.plan().maxPlaces(),
                    preview.plan().estimatedMaxUsd(),
                    preview.overLimit(),
                    preview.dryRunId());
        }
    }

    public record Enrich(int pending, int batch, int maxReviews, long dryRunId) {
        static Enrich from(EnrichPreview preview) {
            return new Enrich(preview.pending(), preview.batch(), preview.maxReviews(), preview.dryRunId());
        }
    }
}
