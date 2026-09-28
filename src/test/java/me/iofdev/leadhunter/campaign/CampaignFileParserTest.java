package me.iofdev.leadhunter.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import me.iofdev.leadhunter.scoring.MapsSignal;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class CampaignFileParserTest {

    private final CampaignFileParser parser = new CampaignFileParser();

    @Test
    void parsesTheBundledTemplate() throws IOException {
        CampaignFile file = parser.parse(new ClassPathResource("campaign-template.yml").getContentAsString(StandardCharsets.UTF_8));

        assertThat(file.slug()).isEqualTo("clinicas-luanda");
        assertThat(file.answers().sector()).isEqualTo("clínicas");
        assertThat(file.answers().service()).isEqualTo("Site");
        assertThat(file.answers().goal().endDate()).isEqualTo(LocalDate.of(2026, 11, 9));
        assertThat(file.answers().goal().stopRule().afterContacted()).isEqualTo(70);
        assertThat(file.search().wantedSignals()).containsExactly(MapsSignal.NO_WEBSITE, MapsSignal.SOCIAL_ONLY);
        assertThat(file.search().disqualifyingSignals()).containsExactly(MapsSignal.OWN_WEBSITE);
        assertThat(file.search().minReviews()).isEqualTo(5);
        assertThat(file.search().terms()).contains("clínica dentária");
        assertThat(file.search().locations()).hasSize(4);
        assertThat(file.search().qualifyShare()).isEqualTo(0.4);
    }

    @Test
    void everyCampaignInTheRepoIsValid() throws IOException {
        try (Stream<Path> files = Files.list(Path.of("campaigns"))) {
            List<Path> yamls = files.filter(p -> p.toString().endsWith(".yml"))
                    .filter(p -> !p.getFileName().toString().equals("company.yml"))
                    .toList();
            assertThat(yamls).isNotEmpty();
            for (Path yaml : yamls) {
                parser.parse(Files.readString(yaml));
            }
        }
    }

    @Test
    void appliesDefaults() {
        CampaignFile file = parser.parse("""
                slug: lojas
                name: Lojas
                answers: {sector: lojas, problem: vendem só no Instagram, service: Site}
                search: {terms: [loja], locations: [Luanda]}
                """);

        assertThat(file.search().maxPlacesPerSearch()).isEqualTo(40);
        assertThat(file.search().minReviews()).isZero();
        assertThat(file.search().wantedSignals()).isEmpty();
        assertThat(file.answers().objections()).isEmpty();
        assertThat(file.search().language()).isEqualTo("pt-PT");
        assertThat(file.search().excludeNames()).isEmpty();
    }

    @Test
    void reportsEveryProblemAtOnce() {
        assertThatThrownBy(() -> parser.parse("""
                slug: Bad Slug
                answers: {sector: lojas, goal: {leadsPerWeek: 0}}
                search:
                  terms: []
                  locations: [Luanda]
                  maxPlacesPerSearch: 500
                  qualifyShare: 0
                  minReviews: -1
                  wantedSignals: [LOW_RATING]
                  disqualifyingSignals: [NO_WEBSITE, SOCIAL_ONLY, OWN_WEBSITE, LOW_RATING]
                """))
                .isInstanceOfSatisfying(InvalidCampaignException.class, e -> assertThat(e.problems()).containsExactly(
                        "slug must be lowercase letters, digits and dashes, like clinicas-luanda",
                        "name is required",
                        "answers.problem is required",
                        "answers.service is required",
                        "answers.goal.leadsPerWeek must be at least 1",
                        "search.terms needs at least one term",
                        "search.maxPlacesPerSearch must be between 1 and 200",
                        "search.qualifyShare must be above 0 and at most 1",
                        "search.minReviews cannot be negative",
                        "search.disqualifyingSignals cannot hold NO_WEBSITE, SOCIAL_ONLY and OWN_WEBSITE together, "
                                + "every place has one of them",
                        "search signals cannot be both wanted and disqualifying: [LOW_RATING]"));
    }

    @Test
    void rejectsUnknownFieldsToCatchTypos() {
        assertThatThrownBy(() -> parser.parse("""
                slug: lojas
                name: Lojas
                answers: {sector: lojas, problem: vendem só no Instagram, service: Site}
                search: {terms: [loja], locations: [Luanda], maxPlaces: 10}
                """))
                .isInstanceOf(InvalidCampaignException.class)
                .hasMessageContaining("maxPlaces");
    }
}
