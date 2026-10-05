package me.iofdev.leadhunter.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.List;

import me.iofdev.leadhunter.campaign.CampaignFile;
import me.iofdev.leadhunter.campaign.CampaignFileParser;
import me.iofdev.leadhunter.company.CompanyProfile;
import me.iofdev.leadhunter.scoring.MapsSignal;
import org.junit.jupiter.api.Test;

class CampaignWizardTest {

    static final LocalDate TODAY = LocalDate.of(2026, 9, 28);

    static final CompanyProfile.CaseStudy DENTAL = new CompanyProfile.CaseStudy("clínica dentária", "Sorriso",
            "sem marcações online", "sistema de marcações", "de 40 para 90 marcações", true);

    static final CompanyProfile COMPANY = new CompanyProfile("Exemplo", "Somos a Exemplo.",
            List.of(new CompanyProfile.Service("Landing page", "150 mil Kz", null),
                    new CompanyProfile.Service("Site", "400 mil Kz", null)),
            "Site", List.of("Luanda"), null, List.of(DENTAL), null, 35, null);

    /** One line per prompt. Empty lines accept defaults or end lists. */
    static final String ANSWERS = String.join("\n",
            "Escolas em Luanda",          // name
            "",                           // slug -> escolas-em-luanda
            "", "escolas",                // 1 sector: required, asked again
            "Os pais só se inscrevem por telefone", // 2 problem
            "",                           // 3 service -> the entry offer, Site
            "",                           // 4 hook -> default
            "",                           // 5 why now -> none
            "d", "d", "d", "", "", "",    // 6 signals: all website signals disqualifying, asked again
            "w", "w", "d", "x", "", "", "", // 6 signals: x is refused and asked again
            "5",                          // 7 min reviews
            "",                           // 8 phone routine -> default
            "Já temos página no Facebook | O site aparece no Google", "sem resposta", "", // 9 objections
            "1",                          // 10 case: dental, from another sector
            "",                           // 11 tone -> default
            "", "",                       // goal: meetings, wins
            "2020-01-01", "",             // goal: end date in the past, then the default
            "", "", "",                   // goal: leads per week, stop rule N and M
            "", "escola", "colégio", "",  // terms: required, asked again
            "",                           // locations -> from the company area
            "",                           // max places -> 40
            "",                           // target keywords -> terms
            "agência", "",                // words to exclude
            "",                           // extra names
            "");

    private final StringWriter output = new StringWriter();

    private CampaignWizard wizard(String input, int freeCapacity) {
        Prompter prompter = new Prompter(new BufferedReader(new StringReader(input)), new PrintWriter(output, true),
                "input ended before the campaign was complete");
        return new CampaignWizard(prompter, COMPANY, slug -> freeCapacity, TODAY);
    }

    @Test
    void buildsACampaignFromAnswersDefaultsAndTheCompany() {
        CampaignFile file = wizard(ANSWERS, 25).run();

        assertThat(file.slug()).isEqualTo("escolas-em-luanda");
        CampaignFile.Answers answers = file.answers();
        assertThat(answers.sector()).isEqualTo("escolas");
        assertThat(answers.service()).isEqualTo("Site");
        assertThat(answers.hook()).isEqualTo(CampaignFile.Answers.DEFAULT_HOOK);
        assertThat(answers.whyNow()).isNull();
        assertThat(answers.phoneRoutine()).isEqualTo(CampaignFile.Answers.DEFAULT_PHONE_ROUTINE);
        assertThat(answers.objections()).singleElement()
                .isEqualTo(new CompanyProfile.Objection("Já temos página no Facebook", "O site aparece no Google"));
        assertThat(answers.caseStudy()).isEqualTo(DENTAL);
        assertThat(answers.goal()).isEqualTo(new CampaignFile.Goal(3, 1, LocalDate.of(2026, 11, 9), 25,
                new CampaignFile.StopRule(2, 50)));
        assertThat(answers.tone()).isEqualTo(CampaignFile.Answers.DEFAULT_TONE);

        CampaignFile.Search search = file.search();
        assertThat(search.wantedSignals()).containsExactly(MapsSignal.NO_WEBSITE, MapsSignal.SOCIAL_ONLY);
        assertThat(search.disqualifyingSignals()).containsExactly(MapsSignal.OWN_WEBSITE);
        assertThat(search.minReviews()).isEqualTo(5);
        assertThat(search.terms()).containsExactly("escola", "colégio");
        assertThat(search.locations()).containsExactly("Luanda, Angola");
        assertThat(search.targetKeywords()).containsExactly("escola", "colégio");
        assertThat(search.excludeKeywords()).containsExactly("agência");
        assertThat(search.excludeNames()).isEmpty();

        assertThat(output.toString())
                .contains("Required.")
                .contains("excludes everything. Choose again.")
                .contains("Type one of w, d, or press Enter.")
                .contains("Not added. Use: objection | answer")
                .contains("Warning: the case is from 'clínica dentária', not 'escolas'")
                .contains("The date cannot be before 2026-09-28.")
                .contains("25 of 35 free")
                .contains("Add at least one.");
    }

    @Test
    void buildsACampaignThatPassesValidation() {
        CampaignFile file = wizard(ANSWERS, 25).run();

        CampaignFileParser.validate(file);

        assertThat(output.toString()).contains("Required.");
    }

    @Test
    void refusesToStartWhenOtherCampaignsUseTheWholeCapacity() {
        assertThatThrownBy(() -> wizard(ANSWERS, 0).run())
                .hasMessageContaining("other running campaigns already use all 35 contacts a week");
    }

    @Test
    void failsClearlyWhenInputEndsEarly() {
        assertThatThrownBy(() -> wizard("Escolas\n", 35).run())
                .hasMessageContaining("input ended before the campaign was complete");
    }

    @Test
    void confirmsOnlyOnYes() {
        assertThat(wizard("y\n", 35).confirm("Replace?")).isTrue();
        assertThat(wizard("sim\n", 35).confirm("Replace?")).isTrue();
        assertThat(wizard("\n", 35).confirm("Replace?")).isFalse();
        assertThat(wizard("n\n", 35).confirm("Replace?")).isFalse();
    }

    @Test
    void slugifiesNames() {
        assertThat(CampaignWizard.slugify("Clínicas em Luanda!")).isEqualTo("clinicas-em-luanda");
        assertThat(CampaignWizard.slugify("  Lojas -- Benguela 2026 ")).isEqualTo("lojas-benguela-2026");
        assertThat(CampaignWizard.slugify("???")).isNull();
    }
}
