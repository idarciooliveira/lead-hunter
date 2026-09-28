package me.iofdev.leadhunter.maps;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        JsonNode value = node.get(field);
        return value != null && value.isNumber() ? value.intValue() : 0;
    }

    private static boolean bool(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isBoolean() && value.booleanValue();
    }
}
