package me.iofdev.leadhunter.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Optional;

import me.iofdev.leadhunter.company.CompanyProfile;
import me.iofdev.leadhunter.company.CompanyProfileParser;
import org.junit.jupiter.api.Test;

class CompanyWizardTest {

    /** One line per prompt. Empty lines accept defaults or end lists. */
    static final String ANSWERS = String.join("\n",
            "Exemplo Software",                      // C1 name
            "Somos a Exemplo, fazemos sites.",       // C2 intro
            "Landing page | 150 mil Kz | 1 semana",  // C3 services
            "Site",                                  //    refused: no price
            "Site | 400 mil Kz", "",
            "2",                                     // C4 entry offer -> Site
            "",                                      // C5 area -> Luanda
            "Kintexia | 923 111 222", "Ajabalg", "", // C6 clients
            "y",                                     // C7 first case
            "clínica dentária", "", "Marcações por telefone", "Marcações online",
            "ficaram contentes", "de 40 para 90 por mês", // result without a number, asked again
            "y",                                     // C7 second case, the last one the wizard asks
            "escolas", "Colégio A", "Inscrições em papel", "Inscrições online", "300 inscrições online", "y",
            "Começamos pela landing page", "", "", "", // C8 answers to the common objections
            "Não confio em empresas novas | Mostramos os casos", "",
            "40",                                    // C9 capacity
            "4", "3.000.000",                        // C10 target
            "");

    private final StringWriter output = new StringWriter();

    private CompanyProfile run(String input, Optional<CompanyProfile> existing) {
        Prompter prompter = new Prompter(new BufferedReader(new StringReader(input)), new PrintWriter(output, true),
                "input ended");
        return new CompanyWizard(prompter, existing).run();
    }

    @Test
    void buildsAValidProfile() {
        CompanyProfile profile = run(ANSWERS, Optional.empty());

        CompanyProfileParser.validate(profile);
        assertThat(profile.services()).extracting(CompanyProfile.Service::name).containsExactly("Landing page", "Site");
        assertThat(profile.services().getFirst().deliveryTime()).isEqualTo("1 semana");
        assertThat(profile.services().get(1).deliveryTime()).isNull();
        assertThat(profile.entryOffer()).isEqualTo("Site");
        assertThat(profile.area()).containsExactly("Luanda");
        assertThat(profile.clients()).containsExactly(
                new CompanyProfile.Client("Kintexia", "923 111 222"), new CompanyProfile.Client("Ajabalg", null));
        assertThat(profile.cases()).hasSize(2);
        assertThat(profile.cases().getFirst().client()).isEqualTo("anonymous");
        assertThat(profile.cases().getFirst().mayName()).isFalse();
        assertThat(profile.cases().getFirst().result()).isEqualTo("de 40 para 90 por mês");
        assertThat(profile.cases().get(1).mayName()).isTrue();
        assertThat(profile.objections()).extracting(CompanyProfile.Objection::objection)
                .containsExactly("É caro", "Não confio em empresas novas");
        assertThat(profile.weeklyCapacity()).isEqualTo(40);
        assertThat(profile.quarterTarget()).isEqualTo(new CompanyProfile.QuarterTarget(4, 3_000_000L));

        assertThat(output.toString())
                .contains("Not added. Use: name | price range in Kz | delivery time (optional)")
                .contains("1 client(s) without a phone")
                .contains("A result without a number is not proof.");
    }

    @Test
    void runningAgainKeepsEveryAnswerOnEnter() {
        CompanyProfile first = run(ANSWERS, Optional.empty());

        // name, intro, services, entry offer, area, clients, keep cases, 4 objections, extra objections,
        // capacity, keep target.
        CompanyProfile again = run("\n".repeat(16), Optional.of(first));

        assertThat(again).isEqualTo(first);
        assertThat(output.toString()).contains("Update the company profile.");
    }
}
