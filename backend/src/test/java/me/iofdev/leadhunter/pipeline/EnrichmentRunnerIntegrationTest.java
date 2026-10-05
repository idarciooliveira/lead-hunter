package me.iofdev.leadhunter.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.sun.net.httpserver.HttpServer;
import me.iofdev.leadhunter.PostgresTestSupport;
import me.iofdev.leadhunter.campaign.Campaign;
import me.iofdev.leadhunter.campaign.CampaignFileParser;
import me.iofdev.leadhunter.campaign.CampaignRepository;
import me.iofdev.leadhunter.llm.FakeLlmClient;
import me.iofdev.leadhunter.llm.LlmCallRepository;
import me.iofdev.leadhunter.llm.LlmClient;
import me.iofdev.leadhunter.llm.RecordingLlmClient;
import me.iofdev.leadhunter.maps.ScrapedPlace;
import me.iofdev.leadhunter.place.CrawlRepository;
import me.iofdev.leadhunter.place.PlaceReview;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@EnabledIf("me.iofdev.leadhunter.PostgresTestSupport#databaseAvailable")
@Import(EnrichmentRunnerIntegrationTest.Config.class)
class EnrichmentRunnerIntegrationTest extends PostgresTestSupport {

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        FakeScraper fakeScraper() {
            return new FakeScraper();
        }

        @Bean
        @Primary
        FakeReviewFetcher fakeReviews() {
            return new FakeReviewFetcher();
        }

        @Bean
        FakeLlmClient fakeLlm() {
            return new FakeLlmClient();
        }

        /** Recorded like the real client, so jobs see their LLM spend. */
        @Bean
        @Primary
        LlmClient recordingFakeLlm(FakeLlmClient fake, LlmCallRepository calls) {
            return new RecordingLlmClient(fake, calls);
        }
    }

    static final String CAMPAIGN = """
            slug: clinicas-enriquecer
            name: Clínicas enriquecer
            answers: {sector: clínicas, problem: marcações só por telefone, service: Site}
            search:
              terms: [clínica]
              locations: [Talatona]
              maxPlacesPerSearch: 20
              targetKeywords: [clínica]
              qualifyShare: 1.0
            """;

    @Autowired
    CampaignRunner stage1;
    @Autowired
    EnrichmentRunner enrichment;
    @Autowired
    CampaignRepository campaigns;
    @Autowired
    CampaignFileParser parser;
    @Autowired
    LeadRepository leads;
    @Autowired
    CrawlRepository crawls;
    @Autowired
    FakeScraper scraper;
    @Autowired
    FakeReviewFetcher reviewFetcher;
    @Autowired
    FakeLlmClient llm;
    @Autowired
    RunJobService jobs;
    @Autowired
    RunRepository runs;

    private HttpServer site;
    private final List<String> progress = new ArrayList<>();
    private Campaign campaign;

    private static ScrapedPlace place(String id, String name, String phone, String website, int reviews) {
        return new ScrapedPlace(id, name, "Clínica", List.of("Clínica"), "Rua 1", "Talatona", "Luanda", phone,
                website, new BigDecimal("4.3"), reviews, -8.9, 13.2, "https://maps.google.com/?cid=" + id,
                false, false, "{\"placeId\":\"" + id + "\"}");
    }

    @BeforeEach
    void setUp() throws Exception {
        site = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        byte[] body = """
                <html><head><meta name="viewport" content="width=device-width"></head>
                <body><footer>Copyright 2026</footer></body></html>
                """.getBytes(StandardCharsets.UTF_8);
        site.createContext("/", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            try (var out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        site.start();

        scraper.reset();
        reviewFetcher.reset();
        llm.answer = "{\"complaints\": [\"contact\", \"waiting\"]}";
        String siteUrl = "http://localhost:" + site.getAddress().getPort() + "/";
        scraper.willReturn("Talatona", List.of(
                place("q1", "Clínica Girassol", "+244923000444", siteUrl, 50),
                place("q2", "Clínica Sorriso", "+244923456789", null, 142)));
        reviewFetcher.willReturn("https://maps.google.com/?cid=q1",
                List.of(new PlaceReview(2, "Ninguém atende o telefone", "2026-08-01")));

        assertThat(campaigns.save(parser.parse(CAMPAIGN))).isTrue();
        campaign = campaigns.findBySlug("clinicas-enriquecer").orElseThrow();
        stage1.run(campaign, false, progress::add);
        progress.clear();
    }

    @AfterEach
    void stopSite() {
        site.stop(0);
    }

    @Test
    void anEnrichmentJobCountsEnrichedLeadsAndItsLlmSpend() {
        jobs.runEnrichment(campaign, 2, 10, 10, progress::add);

        RunRepository.JobView job = runs.listJobs(campaign.id()).getFirst();
        assertThat(job.kind()).isEqualTo(RunRepository.KIND_ENRICH);
        assertThat(job.status()).isEqualTo("SUCCEEDED");
        assertThat(job.done()).isEqualTo(2);
        assertThat(job.total()).isEqualTo(2);
        BigDecimal llmSpend = jdbc.sql("select sum(cost_usd) from llm_call").query(BigDecimal.class).single();
        assertThat(llmSpend).isPositive();
        assertThat(job.costUsd()).isEqualByComparingTo(new BigDecimal("0.11").add(llmSpend));
    }

    @Test
    void crawlsWebsitesFetchesReviewsAndRescores() {
        EnrichmentSummary summary = enrichment.enrich(campaign, 10, 10, progress::add);

        assertThat(summary.considered()).isEqualTo(2);
        assertThat(summary.enriched()).isEqualTo(2);

        // The localhost site is plain HTTP, so it takes NO_HTTPS, plus the review complaints.
        LeadView girassol = leads.list(campaign.id(), Optional.of(LeadStage.QUALIFIED), 10).getFirst();
        assertThat(girassol.name()).isEqualTo("Clínica Girassol");
        assertThat(girassol.score()).isEqualTo(35 + 25 + 20);
        assertThat(girassol.breakdown()).extracting("code").containsExactly(
                "REVIEWS_SWEET_SPOT", "MOBILE_PHONE", "TARGET_SECTOR", "NO_HTTPS", "REVIEW_COMPLAINTS");

        // No website to crawl: the stage 1 score stands, with no phantom broken-site points,
        // and the zero-point marker proves stage 2 ran (ADR 0028).
        LeadView sorriso = leads.list(campaign.id(), Optional.of(LeadStage.QUALIFIED), 10).get(1);
        assertThat(sorriso.name()).isEqualTo("Clínica Sorriso");
        assertThat(sorriso.score()).isEqualTo(65);
        assertThat(sorriso.breakdown()).extracting("code").endsWith("STAGE2_NO_ISSUES");

        assertThat(jdbc.sql("select count(*) from website_crawl").query(Long.class).single()).isEqualTo(1);
        assertThat(jdbc.sql("select reachable from website_crawl").query(Boolean.class).single()).isTrue();
        assertThat(jdbc.sql("select complaint_kinds, enriched_at is not null from lead order by score desc")
                .query((rs, row) -> rs.getString(1) + ":" + rs.getBoolean(2)).list())
                .containsExactly("{contact,waiting}:true", "{}:true");

        assertThat(jdbc.sql("select location, status, cost_usd from campaign_run order by id")
                .query((rs, row) -> rs.getString(1) + ":" + rs.getString(2) + ":" + rs.getBigDecimal(3)).list())
                .containsExactly("Talatona:SUCCEEDED:0.1000", "reviews:SUCCEEDED:0.1100");
        assertThat(progress).anyMatch(line -> line.contains("Clínica Girassol: 35 -> 80"));

        EnrichmentSummary second = enrichment.enrich(campaign, 10, 10, progress::add);

        assertThat(second.considered()).isZero();
        assertThat(second.enriched()).isZero();
    }

    @Test
    void complaintReviewsAddTheTwentyPointReviewComplaintsRule() {
        // Only Sorriso gets reviews, so the fake LLM's last call is its classification and
        // Girassol keeps no complaint points.
        reviewFetcher.willReturn("https://maps.google.com/?cid=q1", List.of());
        reviewFetcher.willReturn("https://maps.google.com/?cid=q2", List.of(
                new PlaceReview(1, "Ninguém atende o telefone", "2026-08-02"),
                new PlaceReview(1, "Impossível marcar consulta", "2026-08-03"),
                new PlaceReview(2, "Duas horas de espera", "2026-08-04")));
        llm.answer = "{\"complaints\": [\"contact\", \"booking\", \"waiting\"]}";

        enrichment.enrich(campaign, 10, 10, progress::add);

        // Sorriso has no website to crawl, so stage 2 can only add the +20 complaint rule.
        LeadView sorriso = leads.list(campaign.id(), Optional.of(LeadStage.QUALIFIED), 10).stream()
                .filter(lead -> lead.name().equals("Clínica Sorriso")).findFirst().orElseThrow();
        assertThat(sorriso.score()).isEqualTo(65 + 20);
        assertThat(sorriso.breakdown()).extracting("code").endsWith("REVIEW_COMPLAINTS");
        assertThat(jdbc.sql("select score, complaint_kinds from lead where id = :id")
                .param("id", sorriso.id())
                .query((rs, row) -> rs.getInt(1) + ":" + rs.getString(2)).single())
                .isEqualTo("85:{booking,contact,waiting}");
        assertThat(progress).anyMatch(line -> line.contains("Clínica Sorriso: 65 -> 85 (+20 REVIEW_COMPLAINTS)"));

        assertThat(llm.last.purpose()).isEqualTo("review-analysis");
        assertThat(llm.last.campaignId()).isEqualTo(campaign.id());
        assertThat(llm.last.user()).contains("Impossível marcar consulta");

        // Girassol got no reviews, so no complaint points and no phantom kinds; its site still
        // earns NO_HTTPS, which suppresses the no-issues marker (ADR 0028).
        LeadView girassol = leads.list(campaign.id(), Optional.of(LeadStage.QUALIFIED), 10).stream()
                .filter(lead -> lead.name().equals("Clínica Girassol")).findFirst().orElseThrow();
        assertThat(girassol.score()).isEqualTo(35 + 25);
        assertThat(girassol.breakdown()).extracting("code").containsExactly(
                "REVIEWS_SWEET_SPOT", "MOBILE_PHONE", "TARGET_SECTOR", "NO_HTTPS");
        assertThat(jdbc.sql("select complaint_kinds from lead where id = :id")
                .param("id", girassol.id()).query(String.class).single())
                .isEqualTo("{}");
    }

    @Test
    void skipsReviewsWithoutATokenButStillCrawls() {
        reviewFetcher.notReady();

        EnrichmentSummary summary = enrichment.enrich(campaign, 10, 10, progress::add);

        assertThat(summary.enriched()).isEqualTo(2);
        assertThat(jdbc.sql("select count(*) from campaign_run where location = 'reviews'").query(Long.class).single())
                .isZero();
        assertThat(progress).anyMatch(line -> line.contains("Skipping reviews"));
        LeadView girassol = leads.list(campaign.id(), Optional.of(LeadStage.QUALIFIED), 10).stream()
                .filter(lead -> lead.name().equals("Clínica Girassol")).findFirst().orElseThrow();
        assertThat(girassol.score()).isEqualTo(35 + 25);

        // Sorriso has no site to crawl and no complaint classes, so its clean stage-2 run shows
        // the zero-point marker in both the breakdown and the progress line.
        LeadView sorriso = leads.list(campaign.id(), Optional.of(LeadStage.QUALIFIED), 10).stream()
                .filter(lead -> lead.name().equals("Clínica Sorriso")).findFirst().orElseThrow();
        assertThat(sorriso.score()).isEqualTo(65);
        assertThat(sorriso.breakdown()).extracting("code").endsWith("STAGE2_NO_ISSUES");
        assertThat(progress).anyMatch(line -> line.contains("+0 STAGE2_NO_ISSUES"));
    }
}
