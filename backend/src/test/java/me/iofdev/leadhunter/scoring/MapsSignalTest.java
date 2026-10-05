package me.iofdev.leadhunter.scoring;

import static me.iofdev.leadhunter.scoring.TestPlaces.place;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import me.iofdev.leadhunter.maps.ScrapedPlace;
import me.iofdev.leadhunter.place.WebsiteKind;
import org.junit.jupiter.api.Test;

class MapsSignalTest {

    private static ScrapedPlace rated(BigDecimal rating, int reviews) {
        return new ScrapedPlace("id", "Loja", "Loja", List.of(), null, null, null, "923456789", null, rating, reviews,
                null, null, null, false, false, "{}");
    }

    @Test
    void everyPlaceHasExactlyOneWebsiteSignal() {
        for (WebsiteKind kind : WebsiteKind.values()) {
            assertThat(MapsSignal.WEBSITE_SIGNALS)
                    .filteredOn(signal -> signal.matches(place("Loja", "Loja", "923456789", null, 10), kind))
                    .hasSize(1);
        }
    }

    @Test
    void countsZeroReviewsAsFewAndUsesTheScorerThresholds() {
        assertThat(MapsSignal.FEW_REVIEWS.matches(rated(null, 0), WebsiteKind.NONE)).isTrue();
        assertThat(MapsSignal.FEW_REVIEWS.matches(rated(null, 20), WebsiteKind.NONE)).isFalse();
        assertThat(MapsSignal.MANY_REVIEWS.matches(rated(null, 301), WebsiteKind.NONE)).isTrue();
        assertThat(MapsSignal.MANY_REVIEWS.matches(rated(null, 300), WebsiteKind.NONE)).isFalse();
    }

    @Test
    void treatsAMissingRatingAsUnknownNotLow() {
        assertThat(MapsSignal.LOW_RATING.matches(rated(null, 0), WebsiteKind.NONE)).isFalse();
        assertThat(MapsSignal.LOW_RATING.matches(rated(new BigDecimal("3.9"), 0), WebsiteKind.NONE)).isFalse();
        assertThat(MapsSignal.LOW_RATING.matches(rated(new BigDecimal("3.9"), 12), WebsiteKind.NONE)).isTrue();
        assertThat(MapsSignal.LOW_RATING.matches(rated(new BigDecimal("4.0"), 12), WebsiteKind.NONE)).isFalse();
    }
}
