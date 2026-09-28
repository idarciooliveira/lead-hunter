package me.iofdev.leadhunter.company;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import me.iofdev.leadhunter.place.PhoneNumber;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

@Component
public class CompanyProfileParser {

    private final YAMLMapper yaml = YAMLMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
            .enable(YAMLWriteFeature.MINIMIZE_QUOTES)
            .changeDefaultPropertyInclusion(inclusion -> inclusion.withValueInclusion(JsonInclude.Include.NON_NULL))
            .build();

    public CompanyProfile parse(String content) {
        CompanyProfile profile;
        try {
            profile = yaml.readValue(content, CompanyProfile.class);
        } catch (JacksonException e) {
            throw new InvalidCompanyException(List.of(e.getOriginalMessage()));
        }
        if (profile == null) {
            throw new InvalidCompanyException(List.of("file is empty"));
        }
        validate(profile);
        return profile;
    }

    /** Writes the profile back to YAML that {@link #parse} accepts. */
    public String toYaml(CompanyProfile profile) {
        return "# Company profile. See docs/adr/0019-company-profile.md.\n"
                + "# Edit it, then save changes with `company update -f <this file>`.\n"
                + yaml.writeValueAsString(profile);
    }

    public static void validate(CompanyProfile profile) {
        List<String> problems = new ArrayList<>();
        if (isBlank(profile.name())) problems.add("name is required");
        if (isBlank(profile.intro())) problems.add("intro is required");
        if (profile.services().isEmpty()) problems.add("services needs at least one service");
        for (int i = 0; i < profile.services().size(); i++) {
            CompanyProfile.Service service = profile.services().get(i);
            if (isBlank(service.name())) problems.add("services[" + i + "].name is required");
            if (isBlank(service.price())) problems.add("services[" + i + "].price is required");
        }
        if (isBlank(profile.entryOffer())) {
            problems.add("entryOffer is required");
        } else if (!profile.services().isEmpty() && profile.service(profile.entryOffer()).isEmpty()) {
            problems.add("entryOffer '" + profile.entryOffer() + "' must be the name of one of the services");
        }
        for (int i = 0; i < profile.clients().size(); i++) {
            if (isBlank(profile.clients().get(i).name())) problems.add("clients[" + i + "].name is required");
        }
        for (int i = 0; i < profile.cases().size(); i++) {
            CompanyProfile.CaseStudy c = profile.cases().get(i);
            if (isBlank(c.sector())) problems.add("cases[" + i + "].sector is required");
            if (isBlank(c.problem())) problems.add("cases[" + i + "].problem is required");
            if (!hasNumber(c.result())) {
                problems.add("cases[" + i + "].result needs a number, like \"marcações passaram de 40 para 90 por mês\"");
            }
        }
        for (int i = 0; i < profile.objections().size(); i++) {
            CompanyProfile.Objection o = profile.objections().get(i);
            if (isBlank(o.objection()) || isBlank(o.answer())) {
                problems.add("objections[" + i + "] needs both objection and answer");
            }
        }
        if (profile.weeklyCapacity() < 1 || profile.weeklyCapacity() > 500) {
            problems.add("weeklyCapacity must be between 1 and 500");
        }
        if (!problems.isEmpty()) {
            throw new InvalidCompanyException(problems);
        }
    }

    /** Things that are allowed but likely to cause trouble later. */
    public static List<String> warnings(CompanyProfile profile) {
        List<String> warnings = new ArrayList<>();
        List<String> withoutPhone = profile.clients().stream()
                .filter(client -> PhoneNumber.parse(client.phone()).isEmpty())
                .map(CompanyProfile.Client::name)
                .toList();
        if (!withoutPhone.isEmpty()) {
            warnings.add("clients without a valid phone are matched by name only, which misses name variants on "
                    + "Google Maps: " + String.join(", ", withoutPhone));
        }
        return warnings;
    }

    static boolean hasNumber(String value) {
        return value != null && value.chars().anyMatch(Character::isDigit);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
