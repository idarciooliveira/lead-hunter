package me.iofdev.leadhunter.campaign;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.dataformat.yaml.YAMLMapper;

@Component
public class CampaignFileParser {

    private static final Pattern SLUG = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

    private final YAMLMapper yaml = YAMLMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
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

    private static void validate(CampaignFile file) {
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
            if (isBlank(answers.offer())) problems.add("answers.offer is required");
            if (isBlank(answers.buyers())) problems.add("answers.buyers is required");
            if (isBlank(answers.area())) problems.add("answers.area is required");
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
        }
        if (!problems.isEmpty()) {
            throw new InvalidCampaignException(problems);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
