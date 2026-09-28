package me.iofdev.leadhunter.maps;

public interface GoogleMapsScraper {

    /** Fails fast when the scraper cannot run at all, such as a missing API token. */
    default void checkReady() {
    }

    ScrapeResult search(ScrapeRequest request);
}
