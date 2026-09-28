package me.iofdev.leadhunter.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import me.iofdev.leadhunter.campaign.CampaignFile;
import me.iofdev.leadhunter.campaign.CampaignFileParser;
import org.junit.jupiter.api.Test;

class CampaignWizardTest {

    /** One line per prompt. Empty lines accept defaults or end lists. */
    static final String ANSWERS = String.join("\n",
            "Escolas em Luanda",     // name
            "",                      // slug -> escolas-em-luanda
            "Sites e apps para PMEs", // 1 offer
            "Escolas e colégios",    // 2 buyers
            "",                      // 3 area -> Luanda
            "Kintexia", "ajabalg", "", // 4 reference clients
            "",                      // 5 ideal size
            "Sem site, inscrições por telefone", // 6 visible pain
            "",                      // 7 exclusions
            "",                      // 8 decision maker -> default
            "",                      // 9 proof
            "muitos", "40",          // 10 capacity: invalid, then valid
            "",                      // terms: required, asked again
            "escola", "colégio", "", // terms
            "",                      // locations -> Luanda, Angola
            "",                      // max places -> 40
            "",                      // target keywords -> terms
            "",                      // exclude names -> reference clients
            "");

    private final StringWriter output = new StringWriter();

    private CampaignWizard wizard(String input) {
        return new CampaignWizard(new BufferedReader(new StringReader(input)), new PrintWriter(output, true));
    }

    @Test
    void buildsACampaignFromAnswersAndDefaults() {
        CampaignFile file = wizard(ANSWERS).run();

        assertThat(file.slug()).isEqualTo("escolas-em-luanda");
        assertThat(file.name()).isEqualTo("Escolas em Luanda");
        assertThat(file.answers().offer()).isEqualTo("Sites e apps para PMEs");
        assertThat(file.answers().area()).isEqualTo("Luanda");
        assertThat(file.answers().referenceClients()).containsExactly("Kintexia", "ajabalg");
        assertThat(file.answers().idealSize()).isNull();
        assertThat(file.answers().decisionMaker()).startsWith("Owner or manager");
        assertThat(file.answers().weeklyCapacity()).isEqualTo(40);
        assertThat(file.search().terms()).containsExactly("escola", "colégio");
        assertThat(file.search().locations()).containsExactly("Luanda, Angola");
        assertThat(file.search().maxPlacesPerSearch()).isEqualTo(40);
        assertThat(file.search().targetKeywords()).containsExactly("escola", "colégio");
        assertThat(file.search().excludeNames()).containsExactly("Kintexia", "ajabalg");

        assertThat(output.toString())
                .contains("Enter a whole number from 1 to 500.")
                .contains("Add at least one.");
    }

    @Test
    void writesYamlThatParsesBackToTheSameCampaign() {
        CampaignFileParser parser = new CampaignFileParser();
        CampaignFile file = wizard(ANSWERS).run();

        String yaml = parser.toYaml(file);

        assertThat(yaml).startsWith("# Created with `campaign new`").doesNotContain("---").doesNotContain("null");
        assertThat(parser.parse(yaml)).isEqualTo(file);
    }

    @Test
    void failsClearlyWhenInputEndsEarly() {
        assertThatThrownBy(() -> wizard("Escolas\n").run())
                .hasMessageContaining("input ended before the campaign was complete");
    }

    @Test
    void confirmsOnlyOnYes() {
        assertThat(wizard("y\n").confirm("Replace?")).isTrue();
        assertThat(wizard("sim\n").confirm("Replace?")).isTrue();
        assertThat(wizard("\n").confirm("Replace?")).isFalse();
        assertThat(wizard("n\n").confirm("Replace?")).isFalse();
    }

    @Test
    void slugifiesNames() {
        assertThat(CampaignWizard.slugify("Clínicas em Luanda!")).isEqualTo("clinicas-em-luanda");
        assertThat(CampaignWizard.slugify("  Lojas -- Benguela 2026 ")).isEqualTo("lojas-benguela-2026");
        assertThat(CampaignWizard.slugify("???")).isNull();
    }
}
