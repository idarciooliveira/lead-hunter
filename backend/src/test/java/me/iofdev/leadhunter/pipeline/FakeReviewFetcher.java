package me.iofdev.leadhunter.pipeline;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import me.iofdev.leadhunter.maps.ReviewFetcher;
import me.iofdev.leadhunter.place.PlaceReview;

/** Returns canned reviews per place URL. URLs with no canned reviews come back empty. */
public class FakeReviewFetcher implements ReviewFetcher {

    private final Map<String, List<PlaceReview>> byUrl = new HashMap<>();
    private boolean ready = true;
    private BigDecimal cost = new BigDecimal("0.11");

    public void willReturn(String placeUrl, List<PlaceReview> reviews) {
        byUrl.put(placeUrl, reviews);
    }

    public void reset() {
        byUrl.clear();
        ready = true;
    }

    public void notReady() {
        ready = false;
    }

    @Override
    public void checkReady() {
        if (!ready) {
            throw new IllegalStateException("APIFY_TOKEN is not set");
        }
    }

    @Override
    public ReviewsResult fetchReviews(List<String> placeUrls, int maxReviews, String language) {
        Map<String, List<PlaceReview>> out = new LinkedHashMap<>();
        for (String url : placeUrls) {
            out.put(url, byUrl.getOrDefault(url, List.of()));
        }
        return new ReviewsResult("fake-reviews", "ds-fake", cost, out);
    }
}
