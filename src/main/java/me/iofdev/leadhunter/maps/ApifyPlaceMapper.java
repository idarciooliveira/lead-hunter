package me.iofdev.leadhunter.maps;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import me.iofdev.leadhunter.place.PlaceReview;
import tools.jackson.databind.JsonNode;

/**
 * Maps one item from the {@code compass/crawler-google-places} dataset. Fields are read leniently
 * because the actor's output can change without notice. See ADR 0005.
 */
public final class ApifyPlaceMapper {

    private ApifyPlaceMapper() {
    }

    public static Optional<ScrapedPlace> map(JsonNode item) {
        String placeId = text(item, "placeId");
        String title = text(item, "title");
        if (placeId == null || title == null) {
            return Optional.empty();
        }
        String phone = text(item, "phoneUnformatted");
        if (phone == null) {
            phone = text(item, "phone");
        }
        JsonNode location = item.path("location");
        return Optional.of(new ScrapedPlace(
                placeId,
                title,
                text(item, "categoryName"),
                texts(item.path("categories")),
                text(item, "address"),
                text(item, "neighborhood"),
                text(item, "city"),
                phone,
                text(item, "website"),
                decimal(item, "totalScore"),
                integer(item, "reviewsCount"),
                number(location, "lat"),
                number(location, "lng"),
                text(item, "url"),
                bool(item, "permanentlyClosed"),
                bool(item, "temporarilyClosed"),
                item.toString()));
    }

    /** Reviews of one dataset item. Lenient like {@link #map}: a review without text carries no signal. */
    public static List<PlaceReview> reviews(JsonNode item) {
        List<PlaceReview> reviews = new ArrayList<>();
        JsonNode array = item.path("reviews");
        if (!array.isArray()) {
            return reviews;
        }
        for (JsonNode review : array) {
            String text = text(review, "text");
            if (text == null) {
                text = text(review, "reviewText");
            }
            if (text == null) {
                continue;
            }
            Integer stars = integerOrNull(review, "stars");
            if (stars == null) {
                stars = integerOrNull(review, "rating");
            }
            String publishedAt = text(review, "publishedAtDate");
            if (publishedAt == null) {
                publishedAt = text(review, "publishedAt");
            }
            reviews.add(new PlaceReview(stars, text, publishedAt));
        }
        return reviews;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isString()) {
            return null;
        }
        String text = value.asString().trim();
        return text.isEmpty() ? null : text;
    }

    private static List<String> texts(JsonNode array) {
        List<String> values = new ArrayList<>();
        if (array.isArray()) {
            for (JsonNode value : array) {
                if (value.isString() && !value.asString().isBlank()) {
                    values.add(value.asString().trim());
                }
            }
        }
        return values;
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isNumber() ? value.decimalValue() : null;
    }

    private static Double number(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isNumber() ? value.doubleValue() : null;
    }

    private static int integer(JsonNode node, String field) {
        Integer value = integerOrNull(node, field);
        return value == null ? 0 : value;
    }

    private static Integer integerOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isNumber() ? value.intValue() : null;
    }

    private static boolean bool(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isBoolean() && value.booleanValue();
    }
}
