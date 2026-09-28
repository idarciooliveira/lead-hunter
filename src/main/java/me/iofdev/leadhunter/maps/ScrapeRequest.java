package me.iofdev.leadhunter.maps;

import java.util.List;

/** One scraper run: every search term, in one location. */
public record ScrapeRequest(List<String> terms, String location, int maxPlacesPerSearch, String language) {

    public int maxPlaces() {
        return terms.size() * maxPlacesPerSearch;
    }
}
