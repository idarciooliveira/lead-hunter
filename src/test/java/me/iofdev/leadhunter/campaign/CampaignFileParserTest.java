package me.iofdev.leadhunter.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class CampaignFileParserTest {

    private final CampaignFileParser parser = new CampaignFileParser();

    @Test
    void parsesTheBundledTemplate() throws IOException {
        CampaignFile file = parser.parse(new ClassPathResource("campaign-template.yml").getContentAsString(StandardCharsets.UTF_8));

        assertThat(file.slug()).isEqualTo("clinicas-luanda");
        assertThat(file.answers().referenceClients()).containsExactly("ajabalg", "kintexia", "horizontetourangola");
        assertThat(file.answers().weeklyCapacity()).isEqualTo(35);
        assertThat(file.search().terms()).contains("clínica dentária");
        assertThat(file.search().locations()).hasSize(4);
        assertThat(file.search().qualifyShare()).isEqualTo(0.4);
    }

    @Test
    void everyCampaignInTheRepoIsValid() throws IOException {
        try (Stream<Path> files = Files.list(Path.of("campaigns"))) {
            List<Path> yamls = files.filter(p -> p.toString().endsWith(".yml")).toList();
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
                answers: {offer: sites, buyers: lojas, area: Luanda}
                search: {terms: [loja], locations: [Luanda]}
                """);

        assertThat(file.search().maxPlacesPerSearch()).isEqualTo(40);
        assertThat(file.search().language()).isEqualTo("pt-PT");
        assertThat(file.search().excludeNames()).isEmpty();
    }

    @Test
    void reportsEveryProblemAtOnce() {
        assertThatThrownBy(() -> parser.parse("""
                slug: Bad Slug
                answers: {offer: sites}
                search: {terms: [], locations: [Luanda], maxPlacesPerSearch: 500, qualifyShare: 0}
                """))
                .isInstanceOfSatisfying(InvalidCampaignException.class, e -> assertThat(e.problems()).containsExactly(
                        "slug must be lowercase letters, digits and dashes, like clinicas-luanda",
                        "name is required",
                        "answers.buyers is required",
                        "answers.area is required",
                        "search.terms needs at least one term",
                        "search.maxPlacesPerSearch must be between 1 and 200",
                        "search.qualifyShare must be above 0 and at most 1"));
    }

    @Test
    void rejectsUnknownFieldsToCatchTypos() {
        assertThatThrownBy(() -> parser.parse("""
                slug: lojas
                name: Lojas
                answers: {offer: sites, buyers: lojas, area: Luanda}
                search: {terms: [loja], locations: [Luanda], maxPlaces: 10}
                """))
                .isInstanceOf(InvalidCampaignException.class)
                .hasMessageContaining("maxPlaces");
    }
}
