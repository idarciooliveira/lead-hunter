package me.iofdev.leadhunter.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import me.iofdev.leadhunter.company.CompanyProfile;
import me.iofdev.leadhunter.scoring.MapsSignal;
import org.junit.jupiter.api.Test;

class CampaignChecksTest {

    static final LocalDate TODAY = LocalDate.of(2026, 9, 28);

    static final CompanyProfile.CaseStudy DENTAL = new CompanyProfile.CaseStudy("clínica dentária", "Sorriso",
            "sem marcações online", "sistema de marcações", "de 40 para 90 marcações", true);

    static final CompanyProfile COMPANY = new CompanyProfile("X", "Somos a X.",
            List.of(new CompanyProfile.Service("Site", "300 mil Kz", null)), "Site", null, null, List.of(DENTAL),
            null, 35, null);

    static CampaignFile campaign(String slug, String sector, CompanyProfile.CaseStudy caseStudy, Integer leadsPerWeek,
                                 LocalDate endDate, List<MapsSignal> wanted, int minReviews) {
        return new CampaignFile(slug, slug,
                new CampaignFile.Answers(sector, "sem site", "Site", null, null, null, null, caseStudy,
                        new CampaignFile.Goal(3, 1, endDate, leadsPerWeek, null), null),
                new CampaignFile.Search(List.of("x"), List.of("Luanda"), null, null, null, null, null, null,
                        wanted, null, minReviews));
    }

    static Campaign saved(CampaignFile file) {
        return new Campaign(1, file.slug(), file.name(), file.answers(), file.search(), null, BigDecimal.ZERO, null);
    }

    @Test
    void freeCapacityCountsOnlyOtherRunningCampaigns() {
        List<Campaign> all = List.of(
                saved(campaign("a", "lojas", null, 20, TODAY.plusDays(1), List.of(), 0)),
                saved(campaign("b", "lojas", null, 10, TODAY.minusDays(1), List.of(), 0)),
                saved(campaign("c", "lojas", null, null, null, List.of(), 0)));

        assertThat(CampaignChecks.freeCapacity(COMPANY, all, null, TODAY)).isEqualTo(15);
        assertThat(CampaignChecks.freeCapacity(COMPANY, all, "a", TODAY)).isEqualTo(35);
    }

    @Test
    void rejectsAServiceTheCompanyDoesNotSell() {
        CampaignFile campaign = campaign("a", "lojas", null, 10, TODAY, List.of(), 0);
        CampaignFile app = new CampaignFile("a", "a", new CampaignFile.Answers("lojas", "p", "App", null, null, null,
                null, null, null, null), campaign.search());

        CampaignChecks.requireFits(campaign, COMPANY);
        assertThatThrownBy(() -> CampaignChecks.requireFits(app, COMPANY))
                .hasMessageContaining("answers.service 'App' is not one of the company's services: Site");
    }

    @Test
    void warnsAboutConflictsItCannotRuleOut() {
        List<Campaign> busy = List.of(saved(campaign("other", "lojas", null, 30, TODAY.plusWeeks(2), List.of(), 0)));
        CampaignFile campaign = campaign("new", "restaurantes", DENTAL, 10, TODAY.plusWeeks(6),
                List.of(MapsSignal.FEW_REVIEWS), 20);

        assertThat(CampaignChecks.warnings(campaign, COMPANY, busy, TODAY)).containsExactly(
                "'fewer than 20 reviews' is wanted, but places need at least 20 reviews, so no wanted place can qualify",
                "the case is from 'clínica dentária', not 'restaurantes'. The pitch will present it as a case from another sector",
                "this campaign plans 10 contacts a week, but only 5 of the company's 35 are free");
    }

    @Test
    void matchesSectorsIgnoringAccentsAndPlurals() {
        assertThat(CampaignChecks.sectorsMatch("clínicas", "clinica dentária")).isTrue();
        assertThat(CampaignChecks.sectorsMatch("Clínicas dentárias", "dentista")).isFalse();
        assertThat(CampaignChecks.sectorsMatch("escolas", "escola primária")).isTrue();
        assertThat(CampaignChecks.sectorsMatch("lojas", "restaurante")).isFalse();
    }
}
