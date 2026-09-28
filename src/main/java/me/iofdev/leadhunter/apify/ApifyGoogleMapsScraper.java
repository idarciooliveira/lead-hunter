package me.iofdev.leadhunter.apify;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import me.iofdev.leadhunter.maps.ApifyPlaceMapper;
import me.iofdev.leadhunter.maps.GoogleMapsScraper;
import me.iofdev.leadhunter.maps.ScrapeException;
import me.iofdev.leadhunter.maps.ScrapeRequest;
import me.iofdev.leadhunter.maps.ScrapeResult;
import me.iofdev.leadhunter.maps.ScrapedPlace;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Stage 1 discovery through Apify: basic place data only, no reviews and no images, to keep the
 * cost per place low. See ADR 0005 and ADR 0006.
 */
@Component
class ApifyGoogleMapsScraper implements GoogleMapsScraper {

    private final ApifyClient client;
    private final ApifyProperties properties;

    ApifyGoogleMapsScraper(ApifyClient client, ApifyProperties properties) {
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
    public ScrapeResult search(ScrapeRequest request) {
        checkReady();
        ApifyRun run = client.startRun(properties.actorId(), input(request));
        Instant deadline = Instant.now().plus(properties.maxRunTime());
        while (!run.finished()) {
            if (Instant.now().isAfter(deadline)) {
                throw new ScrapeException("Apify run " + run.id() + " still " + run.status()
                        + " after " + properties.maxRunTime().toMinutes() + " minutes", run.id());
            }
            run = client.getRun(run.id(), properties.pollWait());
        }
        if (!run.succeeded()) {
            throw new ScrapeException("Apify run " + run.id() + " ended with status " + run.status(), run.id());
        }
        JsonNode items = client.datasetItems(run.defaultDatasetId());
        List<ScrapedPlace> places = new ArrayList<>();
        if (items != null && items.isArray()) {
            for (JsonNode item : items) {
                ApifyPlaceMapper.map(item).ifPresent(places::add);
            }
        }
        return new ScrapeResult(run.id(), run.defaultDatasetId(), run.usageTotalUsd(), places);
    }

    static Map<String, Object> input(ScrapeRequest request) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("searchStringsArray", request.terms());
        input.put("locationQuery", request.location());
        input.put("maxCrawledPlacesPerSearch", request.maxPlacesPerSearch());
        input.put("language", request.language());
        input.put("skipClosedPlaces", true);
        input.put("maxReviews", 0);
        input.put("maxImages", 0);
        input.put("scrapeContacts", false);
        return input;
    }
}
