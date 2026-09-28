package me.iofdev.leadhunter.scoring;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import me.iofdev.leadhunter.campaign.CampaignFile;
import me.iofdev.leadhunter.maps.ScrapedPlace;
import me.iofdev.leadhunter.place.PhoneNumber;

/**
 * Hard filters from ADR 0003 and ADR 0007. An excluded place never reaches the list, whatever its score.
 */
public final class Exclusions {

    /** Organizations too big or the wrong kind of buyer for a small software factory. Matched as whole words. */
    static final List<String> DEFAULT_KEYWORDS = List.of(
            "banco", "bank", "ministerio", "ministry", "governo", "government", "administracao municipal",
            "embaixada", "embassy", "consulado", "unitel", "africell", "movicel", "angola telecom",
            "sonangol", "shoprite", "kero", "candando");

    private Exclusions() {
    }

    public static Optional<String> check(ScrapedPlace place, Optional<PhoneNumber> phone, CampaignFile.Search search) {
        if (place.permanentlyClosed()) {
            return Optional.of("Permanently closed");
        }
        if (place.temporarilyClosed()) {
            return Optional.of("Temporarily closed");
        }
        if (phone.isEmpty()) {
            return Optional.of("No phone number");
        }
        String normalizedName = Text.normalize(place.name());
        for (String name : search.excludeNames()) {
            String needle = Text.normalize(name);
            if (!needle.isEmpty() && normalizedName.contains(needle)) {
                return Optional.of("Excluded name '" + name + "'");
            }
        }
        String haystack = String.join(" | ", Stream.concat(
                Stream.of(place.name(), place.category()),
                place.categories().stream()).filter(v -> v != null).toList());
        for (String keyword : Stream.concat(DEFAULT_KEYWORDS.stream(), search.excludeKeywords().stream()).toList()) {
            if (Text.containsWord(haystack, keyword)) {
                return Optional.of("Matches exclusion keyword '" + keyword + "'");
            }
        }
        return Optional.empty();
    }
}
