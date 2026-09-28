package me.iofdev.leadhunter.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import me.iofdev.leadhunter.PostgresTestSupport;
import me.iofdev.leadhunter.campaign.Campaign;
import me.iofdev.leadhunter.campaign.CampaignFileParser;
import me.iofdev.leadhunter.campaign.CampaignRepository;
import me.iofdev.leadhunter.company.CompanyProfile;
import me.iofdev.leadhunter.company.CompanyRepository;
import me.iofdev.leadhunter.maps.ScrapedPlace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@EnabledIf("me.iofdev.leadhunter.PostgresTestSupport#databaseAvailable")
@Import(CampaignRunnerIntegrationTest.Config.class)
class CampaignRunnerIntegrationTest extends PostgresTestSupport {

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        FakeScraper fakeScraper() {
            return new FakeScraper();
        }
    }

    static final String CAMPAIGN = """
            slug: clinicas-teste
            name: Clínicas teste
            answers: {sector: clínicas, problem: marcações só por telefone, service: Site}
            search:
              terms: [clínica, centro médico]
              locations: [Talatona, Maianga, Viana]
              maxPlacesPerSearch: 20
              targetKeywords: [clínica]
              excludeNames: [Kintexia]
              qualifyShare: 0.5
            """;

    static ScrapedPlace place(String id, String name, String phone, String website, int reviews) {
        return new ScrapedPlace(id, name, "Clínica", List.of("Clínica"), "Rua 1", "Talatona", "Luanda", phone,
                website, new BigDecimal("4.3"), reviews, -8.9, 13.2, "https://maps.google.com/?cid=" + id,
                false, false, "{\"placeId\":\"" + id + "\"}");
    }

    static final ScrapedPlace SORRISO = place("p1", "Clínica Sorriso", "+244923456789", null, 142);
    static final ScrapedPlace SOCIAL = place("p2", "Clínica Vida", "222123456", "https://facebook.com/vida", 35);
    static final ScrapedPlace TINY = place("p3", "Clínica Nova", "923000111", "https://nova.ao", 2);
    static final ScrapedPlace BANK = place("p4", "Banco Económico", "923000222", null, 400);
    static final ScrapedPlace NO_PHONE = place("p5", "Clínica Sem Telefone", null, null, 80);
    static final ScrapedPlace CLIENT = place("p6", "Kintexia Clínica", "923000333", null, 60);
    static final ScrapedPlace OWN_SITE = place("p7", "Clínica Girassol", "923000444", "https://girassol.ao", 50);

    @Autowired
    CampaignRunner runner;
    @Autowired
    CampaignRepository campaigns;
    @Autowired
    CampaignFileParser parser;
    @Autowired
    LeadRepository leads;
    @Autowired
    FakeScraper scraper;
    @Autowired
    CompanyRepository companies;

    Campaign campaign;
    List<String> progress = new ArrayList<>();

    @BeforeEach
    void setUp() {
        scraper.reset();
        scraper.willReturn("Talatona", List.of(SORRISO, SOCIAL, TINY, BANK, NO_PHONE, CLIENT));
        scraper.willReturn("Maianga", List.of(SORRISO, OWN_SITE));
        // Viana has no canned result, so its run fails.
        assertThat(campaigns.save(parser.parse(CAMPAIGN))).isTrue();
        campaign = campaigns.findBySlug("clinicas-teste").orElseThrow();
    }

    @Test
    void runsStageOneEndToEnd() {
        RunSummary summary = runner.run(campaign, false, progress::add);

        assertThat(summary.scraperRuns()).isEqualTo(3);
        assertThat(summary.failedRuns()).isEqualTo(1);
        assertThat(summary.placesFound()).isEqualTo(8);
        assertThat(summary.newLeads()).isEqualTo(7);
        assertThat(summary.excludedThisRun()).isEqualTo(3);
        assertThat(summary.totals()).isEqualTo(new LeadRepository.StageCounts(2, 2, 3));
        assertThat(summary.costUsd()).isEqualByComparingTo("0.20");
        assertThat(progress).anyMatch(line -> line.contains("failed"));

        List<LeadView> qualified = leads.list(campaign.id(), Optional.of(LeadStage.QUALIFIED), 10);
        assertThat(qualified).extracting(LeadView::name).containsExactly("Clínica Sorriso", "Clínica Vida");
        LeadView best = qualified.getFirst();
        assertThat(best.score()).isEqualTo(65);
        assertThat(best.breakdown()).extracting("code")
                .containsExactly("NO_WEBSITE_ACTIVE", "REVIEWS_SWEET_SPOT", "MOBILE_PHONE", "TARGET_SECTOR");
        assertThat(best.whatsappLink()).isEqualTo("https://wa.me/244923456789");

        List<LeadView> belowCut = leads.list(campaign.id(), Optional.of(LeadStage.BELOW_CUT), 10);
        assertThat(belowCut).extracting(LeadView::name).containsExactly("Clínica Girassol", "Clínica Nova");
        assertThat(belowCut.getFirst().stageReason()).isEqualTo("Ranked 3 of 4, below the top 50% cut");

        List<LeadView> excluded = leads.list(campaign.id(), Optional.of(LeadStage.EXCLUDED), 10);
        assertThat(excluded).extracting(LeadView::stageReason).containsExactlyInAnyOrder(
                "Matches exclusion keyword 'banco'", "No phone number", "Excluded name 'Kintexia'");
    }

    @Test
    void recordsEveryScraperRunAndItsCost() {
        runner.run(campaign, false, progress::add);

        assertThat(jdbc.sql("select location, status, external_run_id from campaign_run order by id")
                .query((rs, row) -> rs.getString(1) + ":" + rs.getString(2) + ":" + rs.getString(3)).list())
                .containsExactly("Talatona:SUCCEEDED:fake-Talatona", "Maianga:SUCCEEDED:fake-Maianga",
                        "Viana:FAILED:fake-Viana");
        assertThat(campaigns.findBySlug("clinicas-teste").orElseThrow().totalCostUsd()).isEqualByComparingTo("0.20");
    }

    @Test
    void rerunDeduplicatesAndKeepsLeadsTheUserAlreadyWorked() {
        runner.run(campaign, false, progress::add);
        long sorrisoId = leads.list(campaign.id(), Optional.of(LeadStage.QUALIFIED), 1).getFirst().id();
        jdbc.sql("update lead set status = 'CONTACTED' where id = :id").param("id", sorrisoId).update();

        RunSummary second = runner.run(campaign, false, progress::add);

        assertThat(second.newLeads()).isZero();
        assertThat(jdbc.sql("select count(*) from place").query(Long.class).single()).isEqualTo(7);
        assertThat(jdbc.sql("select count(*) from lead").query(Long.class).single()).isEqualTo(7);
        LeadView sorriso = leads.findById(sorrisoId).orElseThrow();
        assertThat(sorriso.status()).isEqualTo(LeadStatus.CONTACTED);
        assertThat(sorriso.stage()).isEqualTo(LeadStage.QUALIFIED);
        // The cut now ranks only the 3 untouched leads, so the top 50% is 2 of them.
        assertThat(second.totals()).isEqualTo(new LeadRepository.StageCounts(3, 1, 3));
    }

    @Test
    void stopsBeforeRecordingRunsWhenTheScraperIsNotReady() {
        scraper.notReady();

        assertThatThrownBy(() -> runner.run(campaign, false, progress::add)).hasMessageContaining("APIFY_TOKEN");
        assertThat(jdbc.sql("select count(*) from campaign_run").query(Long.class).single()).isZero();
    }

    @Test
    void excludesCompanyClientsByPhoneAndAppliesCampaignFilters() {
        companies.save(new CompanyProfile("X", "Somos a X.", List.of(new CompanyProfile.Service("Site", "1 Kz", null)),
                "Site", null, List.of(new CompanyProfile.Client("Horizonte", "+244 923 000 444")), null, null, null, null));
        campaigns.save(parser.parse(CAMPAIGN.replace("qualifyShare: 0.5", """
                qualifyShare: 0.5
                  minReviews: 5
                  disqualifyingSignals: [SOCIAL_ONLY]""")));
        Campaign filtered = campaigns.findBySlug("clinicas-teste").orElseThrow();

        runner.run(filtered, false, progress::add);

        assertThat(leads.list(filtered.id(), Optional.of(LeadStage.EXCLUDED), 10)).extracting(LeadView::stageReason)
                .contains("Current client 'Horizonte'", "Only 2 reviews, the campaign needs at least 5",
                        "Disqualifying signal: only a social page");
        assertThat(leads.list(filtered.id(), Optional.of(LeadStage.QUALIFIED), 10)).extracting(LeadView::name)
                .containsExactly("Clínica Sorriso");
    }

    @Test
    void readsCampaignsSavedBeforeTheCompanyProfile() {
        jdbc.sql("""
                insert into campaign (slug, name, answers, search) values ('antiga', 'Antiga',
                  '{"offer": "sites", "buyers": "lojas", "area": "Luanda", "referenceClients": ["a"],
                    "proof": "TODO", "weeklyCapacity": 35}',
                  '{"terms": ["loja"], "locations": ["Luanda"], "maxPlacesPerSearch": 10}')
                """).update();

        Campaign old = campaigns.findBySlug("antiga").orElseThrow();

        assertThat(old.answers().sector()).isNull();
        assertThat(old.search().minReviews()).isZero();
        assertThat(old.search().disqualifyingSignals()).isEmpty();
    }

    @Test
    void savingTheSameSlugUpdatesTheCampaign() {
        assertThat(campaigns.save(parser.parse(CAMPAIGN.replace("Clínicas teste", "Clínicas renomeadas")))).isFalse();
        assertThat(campaigns.findBySlug("clinicas-teste").orElseThrow().name()).isEqualTo("Clínicas renomeadas");
    }

    @Test
    void refusesRunsAboveTheBudgetLimit() {
        campaigns.save(parser.parse(CAMPAIGN.replace("maxPlacesPerSearch: 20", "maxPlacesPerSearch: 150")));
        Campaign big = campaigns.findBySlug("clinicas-teste").orElseThrow();

        assertThat(runner.plan(big).maxPlaces()).isEqualTo(900);
        assertThatThrownBy(() -> runner.run(big, false, progress::add))
                .isInstanceOf(BudgetExceededException.class)
                .hasMessageContaining("900");
        assertThat(scraper.requests()).isEmpty();
    }
}
