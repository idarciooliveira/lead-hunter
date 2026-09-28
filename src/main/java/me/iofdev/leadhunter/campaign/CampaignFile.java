package me.iofdev.leadhunter.campaign;

import java.util.List;

/**
 * A campaign as written in YAML: the 10 onboarding answers plus the concrete search. See ADR 0013.
 */
public record CampaignFile(String slug, String name, Answers answers, Search search) {

    /** The 10 onboarding questions. Free text, used later for pitches. */
    public record Answers(
            String offer,
            String buyers,
            String area,
            List<String> referenceClients,
            String idealSize,
            String visiblePain,
            String exclusions,
            String decisionMaker,
            String proof,
            Integer weeklyCapacity) {

        public Answers {
            referenceClients = referenceClients == null ? List.of() : List.copyOf(referenceClients);
        }
    }

    /** What we send to the scraper and how stage 1 filters the results. */
    public record Search(
            List<String> terms,
            List<String> locations,
            Integer maxPlacesPerSearch,
            String language,
            List<String> targetKeywords,
            List<String> excludeKeywords,
            List<String> excludeNames,
            Double qualifyShare) {

        public static final int DEFAULT_MAX_PLACES_PER_SEARCH = 40;
        public static final String DEFAULT_LANGUAGE = "pt-PT";
        public static final double DEFAULT_QUALIFY_SHARE = 0.4;

        public Search {
            terms = terms == null ? List.of() : List.copyOf(terms);
            locations = locations == null ? List.of() : List.copyOf(locations);
            maxPlacesPerSearch = maxPlacesPerSearch == null ? DEFAULT_MAX_PLACES_PER_SEARCH : maxPlacesPerSearch;
            language = language == null || language.isBlank() ? DEFAULT_LANGUAGE : language;
            targetKeywords = targetKeywords == null ? List.of() : List.copyOf(targetKeywords);
            excludeKeywords = excludeKeywords == null ? List.of() : List.copyOf(excludeKeywords);
            excludeNames = excludeNames == null ? List.of() : List.copyOf(excludeNames);
            qualifyShare = qualifyShare == null ? DEFAULT_QUALIFY_SHARE : qualifyShare;
        }
    }
}
