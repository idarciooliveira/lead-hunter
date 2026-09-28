package me.iofdev.leadhunter.campaign;

import java.time.LocalDate;
import java.util.List;

import me.iofdev.leadhunter.company.CompanyProfile;
import me.iofdev.leadhunter.scoring.MapsSignal;

/**
 * A campaign as written in YAML: the campaign questions plus the concrete search. Facts about the
 * company live in {@link CompanyProfile}. See ADR 0013 and ADR 0019.
 */
public record CampaignFile(String slug, String name, Answers answers, Search search) {

    /** The campaign questions. Used by the pitch and the funnel report. */
    public record Answers(
            String sector,
            String problem,
            String service,
            String hook,
            String whyNow,
            String phoneRoutine,
            List<CompanyProfile.Objection> objections,
            CompanyProfile.CaseStudy caseStudy,
            Goal goal,
            String tone) {

        public static final String DEFAULT_HOOK = "Uma análise gratuita do vosso perfil no Google Maps";
        public static final String DEFAULT_PHONE_ROUTINE = "A recepção atende; pedir o dono ou gerente.";
        public static final String DEFAULT_TONE = "Formal, em português (o senhor / a senhora)";

        public Answers {
            objections = objections == null ? List.of() : List.copyOf(objections);
        }
    }

    /** What the campaign must reach, by when, and when to give up on it. */
    public record Goal(Integer meetings, Integer wins, LocalDate endDate, Integer leadsPerWeek, StopRule stopRule) {
    }

    /** Stop or rethink the campaign when fewer than {@code minInterested} leads are interested after {@code afterContacted}. */
    public record StopRule(Integer minInterested, Integer afterContacted) {
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
            Double qualifyShare,
            List<MapsSignal> wantedSignals,
            List<MapsSignal> disqualifyingSignals,
            Integer minReviews) {

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
            wantedSignals = wantedSignals == null ? List.of() : List.copyOf(wantedSignals);
            disqualifyingSignals = disqualifyingSignals == null ? List.of() : List.copyOf(disqualifyingSignals);
            minReviews = minReviews == null ? 0 : minReviews;
        }
    }
}
