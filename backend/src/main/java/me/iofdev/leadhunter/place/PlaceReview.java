package me.iofdev.leadhunter.place;

/** One Google Maps review scraped for an enriched place. */
public record PlaceReview(Integer star, String text, String publishedAt) {
}
