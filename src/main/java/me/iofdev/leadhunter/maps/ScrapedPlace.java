package me.iofdev.leadhunter.maps;

import java.math.BigDecimal;
import java.util.List;

/** One Google Maps place as returned by the scraper, with the raw item kept for later extraction. */
public record ScrapedPlace(
        String googlePlaceId,
        String name,
        String category,
        List<String> categories,
        String address,
        String neighborhood,
        String city,
        String phone,
        String website,
        BigDecimal rating,
        int reviewsCount,
        Double latitude,
        Double longitude,
        String mapsUrl,
        boolean permanentlyClosed,
        boolean temporarilyClosed,
        String rawJson) {

    public ScrapedPlace {
        categories = categories == null ? List.of() : List.copyOf(categories);
    }
}
