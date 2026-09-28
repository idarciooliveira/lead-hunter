package me.iofdev.leadhunter.pipeline;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.iofdev.leadhunter.maps.GoogleMapsScraper;
import me.iofdev.leadhunter.maps.ScrapeException;
import me.iofdev.leadhunter.maps.ScrapeRequest;
import me.iofdev.leadhunter.maps.ScrapeResult;
import me.iofdev.leadhunter.maps.ScrapedPlace;

/** Returns canned places per location. A location with no canned result fails like a broken Apify run. */
public class FakeScraper implements GoogleMapsScraper {

    private final Map<String, List<ScrapedPlace>> byLocation = new HashMap<>();
    private final List<ScrapeRequest> requests = new ArrayList<>();
    private boolean ready = true;

    public void willReturn(String location, List<ScrapedPlace> places) {
        byLocation.put(location, places);
    }

    public void reset() {
        byLocation.clear();
        requests.clear();
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

    public List<ScrapeRequest> requests() {
        return requests;
    }

    @Override
    public ScrapeResult search(ScrapeRequest request) {
        requests.add(request);
        List<ScrapedPlace> places = byLocation.get(request.location());
        if (places == null) {
            throw new ScrapeException("Apify run fake-" + request.location() + " ended with status FAILED",
                    "fake-" + request.location());
        }
        return new ScrapeResult("fake-" + request.location(), "ds", new BigDecimal("0.10"), places);
    }
}
