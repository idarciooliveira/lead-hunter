package me.iofdev.leadhunter.maps;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ApifyPlaceMapperTest {

    static List<ScrapedPlace> fixture() throws IOException {
        try (InputStream in = ApifyPlaceMapperTest.class.getResourceAsStream("/apify/places.json")) {
            JsonNode items = JsonMapper.builder().build().readTree(in);
            List<ScrapedPlace> places = new ArrayList<>();
            items.forEach(item -> ApifyPlaceMapper.map(item).ifPresent(places::add));
            return places;
        }
    }

    @Test
    void mapsCompleteItems() throws IOException {
        ScrapedPlace place = fixture().getFirst();

        assertThat(place.googlePlaceId()).isEqualTo("ChIJ-sorriso");
        assertThat(place.name()).isEqualTo("Clínica Sorriso Talatona");
        assertThat(place.category()).isEqualTo("Clínica dentária");
        assertThat(place.categories()).containsExactly("Clínica dentária", "Dentista");
        assertThat(place.phone()).isEqualTo("+244923456789");
        assertThat(place.website()).isNull();
        assertThat(place.rating()).isEqualByComparingTo(new BigDecimal("4.4"));
        assertThat(place.reviewsCount()).isEqualTo(142);
        assertThat(place.latitude()).isEqualTo(-8.9167);
        assertThat(place.neighborhood()).isEqualTo("Talatona");
        assertThat(place.rawJson()).contains("\"placeId\":\"ChIJ-sorriso\"");
    }

    @Test
    void fallsBackToFormattedPhoneAndToleratesMissingFields() throws IOException {
        ScrapedPlace place = fixture().get(1);

        assertThat(place.phone()).isEqualTo("222 123 456");
        assertThat(place.categories()).isEmpty();
        assertThat(place.latitude()).isNull();
        assertThat(place.permanentlyClosed()).isFalse();
    }

    @Test
    void skipsItemsWithoutIdOrTitle() throws IOException {
        assertThat(fixture()).hasSize(2);
    }
}
