package me.iofdev.leadhunter.maps;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import me.iofdev.leadhunter.place.PlaceReview;

/** Stage 2 review pass over already-qualified places: recent reviews per place URL. See ADR 0027. */
public interface ReviewFetcher {

    void checkReady();

    ReviewsResult fetchReviews(List<String> placeUrls, int maxReviews, String language);

    record ReviewsResult(String externalRunId, String datasetId, BigDecimal costUsd,
                           Map<String, List<PlaceReview>> reviewsByUrl) implements ExternalRunResult {
    }
}
