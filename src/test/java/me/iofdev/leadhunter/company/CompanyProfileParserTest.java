package me.iofdev.leadhunter.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class CompanyProfileParserTest {

    private final CompanyProfileParser parser = new CompanyProfileParser();

    @Test
    void parsesTheBundledTemplate() throws IOException {
        CompanyProfile profile = parser.parse(
                new ClassPathResource("company-template.yml").getContentAsString(StandardCharsets.UTF_8));

        assertThat(profile.services()).hasSize(3);
        assertThat(profile.service("landing PAGE")).isPresent();
        assertThat(profile.entryOffer()).isEqualTo("Landing page");
        assertThat(profile.cases().getFirst().mayName()).isFalse();
        assertThat(profile.cases().getFirst().label()).startsWith("clínica dentária, anonymous client: ");
        assertThat(profile.quarterTarget().revenueKz()).isEqualTo(3_000_000L);
        assertThat(CompanyProfileParser.warnings(profile)).isEmpty();
    }

    @Test
    void theCompanyFileInTheRepoIsValid() throws IOException {
        CompanyProfile profile = parser.parse(Files.readString(Path.of("campaigns", "company.yml")));

        assertThat(profile.clients()).extracting(CompanyProfile.Client::name)
                .containsExactly("ajabalg", "kintexia", "horizontetourangola");
    }

    @Test
    void appliesDefaults() {
        CompanyProfile profile = parser.parse("""
                name: X
                intro: Somos a X.
                services: [{name: Site, price: 300 mil Kz}]
                entryOffer: Site
                """);

        assertThat(profile.area()).containsExactly("Luanda");
        assertThat(profile.weeklyCapacity()).isEqualTo(35);
        assertThat(profile.clients()).isEmpty();
    }

    @Test
    void reportsEveryProblemAtOnce() {
        assertThatThrownBy(() -> parser.parse("""
                name: X
                services: [{name: Site}]
                entryOffer: App
                cases: [{sector: lojas, problem: sem site, result: ficaram contentes}]
                objections: [{objection: É caro}]
                weeklyCapacity: 0
                """))
                .isInstanceOfSatisfying(InvalidCompanyException.class, e -> assertThat(e.problems()).containsExactly(
                        "intro is required",
                        "services[0].price is required",
                        "entryOffer 'App' must be the name of one of the services",
                        "cases[0].result needs a number, like \"marcações passaram de 40 para 90 por mês\"",
                        "objections[0] needs both objection and answer",
                        "weeklyCapacity must be between 1 and 500"));
    }

    @Test
    void warnsAboutClientsWithoutAPhone() {
        CompanyProfile profile = parser.parse("""
                name: X
                intro: Somos a X.
                services: [{name: Site, price: 300 mil Kz}]
                entryOffer: Site
                clients: [{name: Kintexia}, {name: Ajabalg, phone: 923 111 222}]
                """);

        assertThat(CompanyProfileParser.warnings(profile)).singleElement().asString().endsWith(": Kintexia");
    }

    @Test
    void writesYamlThatParsesBackToTheSameProfile() throws IOException {
        CompanyProfile profile = parser.parse(
                new ClassPathResource("company-template.yml").getContentAsString(StandardCharsets.UTF_8));

        String yaml = parser.toYaml(profile);

        assertThat(yaml).doesNotContain("null").doesNotContain("---");
        assertThat(parser.parse(yaml)).isEqualTo(profile);
    }
}
