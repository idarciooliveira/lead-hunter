package me.iofdev.leadhunter.apify;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import me.iofdev.leadhunter.maps.ApifyPlaceMapper;
import me.iofdev.leadhunter.maps.ReviewFetcher;
import me.iofdev.leadhunter.place.PlaceReview;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Stage 2 reviews through the same Google Maps actor: one run over the qualified places' Maps URLs,
 * asking only for reviews. See ADR 0005 and ADR 0027.
 */
@Component
public class ApifyReviewFetcher implements ReviewFetcher {

    private final ApifyClient client;
    private final ApifyProperties properties;

    ApifyReviewFetcher(ApifyClient client, ApifyProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public void checkReady() {
        if (!properties.hasToken()) {
            throw new IllegalStateException("APIFY_TOKEN is not set. Get a token at console.apify.com and export it");
        }
    }

    @Override
    public ReviewsResult fetchReviews(List<String> placeUrls, int maxReviews, String language) {
        checkReady();
        ApifyRun run = ApifyRuns.awaitFinished(client, properties,
                client.startRun(properties.actorId(), input(placeUrls, maxReviews, language)));
        Map<String, List<PlaceReview>> reviewsByUrl = new LinkedHashMap<>();
        JsonNode items = client.datasetItems(run.defaultDatasetId());
        if (items != null && items.isArray()) {
            for (JsonNode item : items) {
                String url = item.path("url").asString(null);
                if (url != null && placeUrls.contains(url)) {
                    reviewsByUrl.computeIfAbsent(url, key -> new ArrayList<>())
                            .addAll(ApifyPlaceMapper.reviews(item));
                }
            }
        }
        return new ReviewsResult(run.id(), run.defaultDatasetId(), run.usageTotalUsd(), reviewsByUrl);
    }

    static Map<String, Object> input(List<String> placeUrls, int maxReviews, String language) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("startUrls", placeUrls.stream().map(url -> Map.of("url", url)).toList());
        input.put("maxReviews", maxReviews);
        input.put("maxImages", 0);
        input.put("scrapeContacts", false);
        input.put("language", language);
        return input;
    }
}
