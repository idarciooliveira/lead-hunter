package me.iofdev.leadhunter.maps;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import me.iofdev.leadhunter.place.PlaceReview;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ApifyPlaceMapperReviewsTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static JsonNode item(String reviews) {
        return JSON.readTree("{\"url\": \"https://maps.google.com/?cid=p1\", \"reviews\": " + reviews + "}");
    }

    @Test
    void readsStarsTextAndDateLeniently() {
        List<PlaceReview> reviews = ApifyPlaceMapper.reviews(item("""
                [{"stars": 2, "text": " Ninguém atende ", "publishedAtDate": "2026-08-01"},
                 {"rating": 5, "reviewText": "Bom", "publishedAt": "2026-08-02"},
                 {"stars": 1, "publishedAtDate": "2026-08-03"}]"""));

        assertThat(reviews).containsExactly(
                new PlaceReview(2, "Ninguém atende", "2026-08-01"),
                new PlaceReview(5, "Bom", "2026-08-02"));
    }

    @Test
    void missingReviewsArrayMeansNoReviews() {
        assertThat(ApifyPlaceMapper.reviews(JSON.readTree("{\"url\": \"x\"}"))).isEmpty();
    }
}
