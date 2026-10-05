package me.iofdev.leadhunter.campaign;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonInclude;
import me.iofdev.leadhunter.scoring.MapsSignal;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

@Component
public class CampaignFileParser {

    private static final Pattern SLUG = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

    private final YAMLMapper yaml = YAMLMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
            .enable(YAMLWriteFeature.MINIMIZE_QUOTES)
            .changeDefaultPropertyInclusion(inclusion -> inclusion.withValueInclusion(JsonInclude.Include.NON_NULL))
            .build();

    public CampaignFile parse(String content) {
        CampaignFile file;
        try {
            file = yaml.readValue(content, CampaignFile.class);
        } catch (JacksonException e) {
            throw new InvalidCampaignException(List.of(e.getOriginalMessage()));
        }
        if (file == null) {
            throw new InvalidCampaignException(List.of("file is empty"));
        }
        validate(file);
        return file;
    }

    /** Writes a campaign back to YAML that {@link #parse} accepts, so wizard campaigns can live in git. */
    public String toYaml(CampaignFile file) {
        return "# Created with `campaign new`. Edit it, then save changes with `campaign create -f <this file>`.\n"
                + yaml.writeValueAsString(file);
    }

    public static void validate(CampaignFile file) {
        List<String> problems = new ArrayList<>();
        if (file.slug() == null || !SLUG.matcher(file.slug()).matches()) {
            problems.add("slug must be lowercase letters, digits and dashes, like clinicas-luanda");
        }
        if (isBlank(file.name())) {
            problems.add("name is required");
        }
        CampaignFile.Answers answers = file.answers();
        if (answers == null) {
            problems.add("answers is required");
        } else {
            if (isBlank(answers.sector())) problems.add("answers.sector is required");
            if (isBlank(answers.problem())) problems.add("answers.problem is required");
            if (isBlank(answers.service())) problems.add("answers.service is required");
            CampaignFile.Goal goal = answers.goal();
            if (goal != null) {
                if (goal.leadsPerWeek() != null && goal.leadsPerWeek() < 1) {
                    problems.add("answers.goal.leadsPerWeek must be at least 1");
                }
                if (goal.meetings() != null && goal.meetings() < 0 || goal.wins() != null && goal.wins() < 0) {
                    problems.add("answers.goal meetings and wins cannot be negative");
                }
            }
        }
        CampaignFile.Search search = file.search();
        if (search == null) {
            problems.add("search is required");
        } else {
            if (search.terms().isEmpty()) problems.add("search.terms needs at least one term");
            if (search.locations().isEmpty()) problems.add("search.locations needs at least one location");
            if (search.maxPlacesPerSearch() < 1 || search.maxPlacesPerSearch() > 200) {
                problems.add("search.maxPlacesPerSearch must be between 1 and 200");
            }
            if (search.qualifyShare() <= 0 || search.qualifyShare() > 1) {
                problems.add("search.qualifyShare must be above 0 and at most 1");
            }
            if (search.minReviews() < 0) {
                problems.add("search.minReviews cannot be negative");
            }
            if (search.disqualifyingSignals().containsAll(MapsSignal.WEBSITE_SIGNALS)) {
                problems.add("search.disqualifyingSignals cannot hold NO_WEBSITE, SOCIAL_ONLY and OWN_WEBSITE together, "
                        + "every place has one of them");
            }
            List<MapsSignal> both = search.wantedSignals().stream()
                    .filter(search.disqualifyingSignals()::contains).toList();
            if (!both.isEmpty()) {
                problems.add("search signals cannot be both wanted and disqualifying: " + both);
            }
        }
        if (!problems.isEmpty()) {
            throw new InvalidCampaignException(problems);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
