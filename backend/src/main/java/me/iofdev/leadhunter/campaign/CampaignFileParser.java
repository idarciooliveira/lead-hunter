package me.iofdev.leadhunter.campaign;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import me.iofdev.leadhunter.input.InvalidInputException;
import me.iofdev.leadhunter.input.YamlInput;
import me.iofdev.leadhunter.scoring.MapsSignal;
import org.springframework.stereotype.Component;

@Component
public class CampaignFileParser {

    private static final Pattern SLUG = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

    public CampaignFile parse(String content) {
        CampaignFile file = YamlInput.read(content, CampaignFile.class, "campaign file");
        validate(file);
        return file;
    }

    public static void validate(CampaignFile file) {
        List<String> problems = new ArrayList<>();
        if (file.slug() == null || !SLUG.matcher(file.slug()).matches()) {
            problems.add("slug must be lowercase letters, digits and dashes, like clinicas-luanda");
        }
        if (YamlInput.isBlank(file.name())) {
            problems.add("name is required");
        }
        CampaignFile.Answers answers = file.answers();
        if (answers == null) {
            problems.add("answers is required");
        } else {
            if (YamlInput.isBlank(answers.sector())) problems.add("answers.sector is required");
            if (YamlInput.isBlank(answers.problem())) problems.add("answers.problem is required");
            if (YamlInput.isBlank(answers.service())) problems.add("answers.service is required");
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
            throw new InvalidInputException("campaign file", problems);
        }
    }
}
