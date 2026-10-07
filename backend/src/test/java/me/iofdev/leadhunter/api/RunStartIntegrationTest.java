package me.iofdev.leadhunter.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import com.jayway.jsonpath.JsonPath;
import me.iofdev.leadhunter.PostgresTestSupport;
import me.iofdev.leadhunter.campaign.Campaign;
import me.iofdev.leadhunter.campaign.CampaignFileParser;
import me.iofdev.leadhunter.campaign.CampaignRepository;
import me.iofdev.leadhunter.maps.ScrapedPlace;
import me.iofdev.leadhunter.pipeline.BudgetService;
import me.iofdev.leadhunter.pipeline.CampaignRunner;
import me.iofdev.leadhunter.pipeline.EnrichmentProperties;
import me.iofdev.leadhunter.pipeline.EnrichmentRunner;
import me.iofdev.leadhunter.pipeline.FakeScraper;
import me.iofdev.leadhunter.pipeline.JobLease;
import me.iofdev.leadhunter.pipeline.LeadRepository;
import me.iofdev.leadhunter.pipeline.RunJobService;
import me.iofdev.leadhunter.pipeline.RunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Runs from the UI (ADR 0033): {@code POST /api/campaigns/{slug}/runs} and
 * {@code /enrichment} open a background job and return 202 with its id, and
 * {@code GET /api/runs/{id}} reports status, counts and cost until it lands.
 * The scraper is fake, so nothing here spends Apify or LLM credit.
 */
@EnabledIf("me.iofdev.leadhunter.PostgresTestSupport#databaseAvailable")
@SpringBootTest(properties = {"leadhunter.cli.enabled=false", "spring.main.web-application-type=servlet"})
@AutoConfigureMockMvc
@Import({RunStartIntegrationTest.Config.class, ApiTestAuth.class})
class RunStartIntegrationTest extends PostgresTestSupport {

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        FakeScraper fakeScraper() {
            return new FakeScraper();
        }
    }

    private static final String CAMPAIGN = """
            slug: clinicas-teste
            name: Clínicas teste
            answers: {sector: clínicas, problem: marcações só por telefone, service: Site}
            search:
              terms: [clínica]
              locations: [Talatona]
              maxPlacesPerSearch: 20
            """;

    private static final String BIG_CAMPAIGN = """
            slug: clinicas-grande
            name: Clínicas grande
            answers: {sector: clínicas, problem: marcações só por telefone, service: Site}
            search:
              terms: [clínica]
              locations: [Talatona, Maianga, Viana, Zango]
              maxPlacesPerSearch: 200
            """;

    @Autowired
    MockMvc mvc;
    @Autowired
    CampaignRepository campaigns;
    @Autowired
    CampaignFileParser parser;
    @Autowired
    RunRepository runs;
    @Autowired
    FakeScraper scraper;
    @Autowired
    InterruptedRunRecovery recovery;
    @Autowired
    CampaignRunner scrapeRunner;
    @Autowired
    EnrichmentRunner enrichRunner;
    @Autowired
    LeadRepository leads;
    @Autowired
    BudgetService budget;
    @Autowired
    EnrichmentProperties enrichmentProperties;

    Campaign campaign;

    @BeforeEach
    void seed() {
        scraper.reset();
        scraper.willReturn("Talatona", List.of(
                place("p1", "Clínica Sorriso", "+244923456789", 142),
                place("p2", "Clínica Vida", "222123456", 35)));
        campaigns.save(ORG, parser.parse(CAMPAIGN));
        campaign = campaigns.findBySlug(ORG, "clinicas-teste").orElseThrow();
    }

    @Test
    void dryRunReturnsThePlanAndStoresAFreeRow() throws Exception {
        mvc.perform(post("/api/campaigns/{slug}/runs", campaign.slug()).param("dryRun", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxPlaces").value(20))
                .andExpect(jsonPath("$.overLimit").value(false))
                .andExpect(jsonPath("$.requests[0].location").value("Talatona"));

        mvc.perform(get("/api/campaigns/{slug}/runs", campaign.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].kind").value("DRY_RUN"))
                .andExpect(jsonPath("$[0].status").value("DONE"))
                .andExpect(jsonPath("$[0].done").value(20))
                .andExpect(jsonPath("$[0].costUsd").value(0));
    }

    @Test
    void startRunsInBackgroundAndPollingSeesItFinish() throws Exception {
        MvcResult started = mvc.perform(post("/api/campaigns/{slug}/runs", campaign.slug()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andReturn();
        long jobId = startedJobId(started);

        String finished = pollUntilDone(jobId);
        assertThatStatus(finished, "DONE");
        mvc.perform(get("/api/runs/{id}", jobId))
                .andExpect(jsonPath("$.kind").value("SCRAPE"))
                .andExpect(jsonPath("$.done").value(2))
                .andExpect(jsonPath("$.costUsd").value(0.10));
    }

    @Test
    void secondStartWhileRunningReturns409() throws Exception {
        seedQualifiedLead();
        try (JobLease running = runs.startJob(campaign.id(), RunRepository.KIND_SCRAPE, null, null)) {
            mvc.perform(post("/api/campaigns/{slug}/runs", campaign.slug()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", containsString("already has a running job")));

            mvc.perform(post("/api/campaigns/{slug}/enrichment", campaign.slug()))
                    .andExpect(status().isConflict());
        }
    }

    @Test
    void aJobWhoseProcessDiedDoesNotBlockTheNextStart() throws Exception {
        long dead = startAndAbandon();

        long jobId = startedJobId(mvc.perform(post("/api/campaigns/{slug}/runs", campaign.slug()))
                .andExpect(status().isAccepted())
                .andReturn());

        assertThatStatus(pollUntilDone(jobId), "DONE");
        mvc.perform(get("/api/runs/{id}", dead))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.error").value("interrupted before it finished"));
    }

    @Test
    void aFullQueueFailsTheJobItOpened() {
        RunJobService full = new RunJobService(scrapeRunner, enrichRunner, runs, leads, budget, enrichmentProperties, task -> {
            throw new TaskRejectedException("queue full");
        });

        assertThatThrownBy(() -> full.startScrape(campaign, false)).isInstanceOf(TaskRejectedException.class);

        RunRepository.JobView job = runs.listJobs(campaign.id()).getFirst();
        assertThat(job.status()).isEqualTo("FAILED");
        assertThat(job.error()).isEqualTo("too many jobs waiting");
    }

    @Test
    void anErrorStillClosesTheJob() {
        RunJobService inline = new RunJobService(scrapeRunner, enrichRunner, runs, leads, budget, enrichmentProperties, Runnable::run);
        scraper.crashWith(new OutOfMemoryError("Java heap space"));

        assertThatThrownBy(() -> inline.startScrape(campaign, false)).isInstanceOf(OutOfMemoryError.class);

        RunRepository.JobView job = runs.listJobs(campaign.id()).getFirst();
        assertThat(job.status()).isEqualTo("FAILED");
        assertThat(job.error()).isEqualTo("Java heap space");
    }

    @Test
    void aRunThatWouldPassTheOrganizationBudgetIsABadRequestAndWritesNothing() throws Exception {
        jdbc.sql("update organization set monthly_budget_usd = 0.01 where id = :id").param("id", ORG.value()).update();

        mvc.perform(post("/api/campaigns/{slug}/runs", campaign.slug()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("monthly budget of this organization")));

        assertThat(runs.listJobs(campaign.id())).isEmpty();
    }

    @Test
    void overLimitWithoutFlagIsABadRequest() throws Exception {
        campaigns.save(ORG, parser.parse(BIG_CAMPAIGN));

        mvc.perform(post("/api/campaigns/{slug}/runs", "clinicas-grande").param("dryRun", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overLimit").value(true));

        mvc.perform(post("/api/campaigns/{slug}/runs", "clinicas-grande"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("above the limit")));
    }

    @Test
    void enrichDryRunCountsWithoutTouching() throws Exception {
        seedQualifiedLead();

        mvc.perform(post("/api/campaigns/{slug}/enrichment", campaign.slug()).param("dryRun", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pending").value(1))
                .andExpect(jsonPath("$.batch").value(25));
    }

    @Test
    void enrichWithNothingWaitingIsABadRequest() throws Exception {
        mvc.perform(post("/api/campaigns/{slug}/enrichment", campaign.slug()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("nothing to enrich")));
    }

    @Test
    void unknownCampaignAndRunAre404() throws Exception {
        mvc.perform(get("/api/campaigns/{slug}/runs", "nope"))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/runs/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("no run with id")));
    }

    @Test
    void restartFailsOnlyJobsWhoseProcessIsGone() throws Exception {
        campaigns.save(ORG, parser.parse(BIG_CAMPAIGN));
        long other = campaigns.findBySlug(ORG, "clinicas-grande").orElseThrow().id();
        long dead = startAndAbandon();

        try (JobLease live = runs.startJob(other, RunRepository.KIND_SCRAPE, null, null)) {
            recovery.run(new DefaultApplicationArguments());

            mvc.perform(get("/api/runs/{id}", dead))
                    .andExpect(jsonPath("$.status").value("FAILED"))
                    .andExpect(jsonPath("$.error").value("interrupted before it finished"));
            mvc.perform(get("/api/runs/{id}", live.jobId()))
                    .andExpect(jsonPath("$.status").value("RUNNING"));
        }
    }

    /** A job whose process died: its row says RUNNING, but nothing holds its lease. */
    private long startAndAbandon() {
        try (JobLease lease = runs.startJob(campaign.id(), RunRepository.KIND_SCRAPE, null, null)) {
            return lease.jobId();
        }
    }

    private static long startedJobId(MvcResult started) throws Exception {
        return ((Number) JsonPath.read(started.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private void seedQualifiedLead() {
        long place = jdbc.sql("""
                        insert into place (google_place_id, name, category, address, neighborhood,
                            phone_e164, phone_mobile, website, website_kind, rating, reviews_count, maps_url, raw)
                        values ('p9', 'Clínica Girassol', 'Clínica', 'Rua 9', 'Talatona',
                            '+244923000444', true, null, 'NONE', 4.1, 50, 'https://maps.example/p9', '{}')
                        returning id
                        """)
                .query(Long.class)
                .single();
        jdbc.sql("""
                        insert into lead (campaign_id, place_id, stage, score, score_breakdown)
                        values (:campaignId, :placeId, 'QUALIFIED', 65, cast('[]' as jsonb))
                        """)
                .param("campaignId", campaign.id())
                .param("placeId", place)
                .update();
    }

    private String pollUntilDone(long jobId) throws Exception {
        long deadline = System.currentTimeMillis() + 15_000;
        while (true) {
            MvcResult result = mvc.perform(get("/api/runs/{id}", jobId))
                    .andExpect(status().isOk())
                    .andReturn();
            String body = result.getResponse().getContentAsString();
            String jobStatus = JsonPath.read(body, "$.status");
            if (!jobStatus.equals("RUNNING")) {
                return body;
            }
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException("job " + jobId + " never finished");
            }
            Thread.sleep(100);
        }
    }

    private static void assertThatStatus(String body, String expected) {
        assertThat(JsonPath.<String>read(body, "$.status")).isEqualTo(expected);
    }

    /** Same shape as the canned places the pipeline tests build. */
    private static ScrapedPlace place(String id, String name, String phone, int reviews) {
        return new ScrapedPlace(id, name, "Clínica", List.of("Clínica"), "Rua 1", "Talatona", "Luanda", phone,
                null, new BigDecimal("4.3"), reviews, -8.9, 13.2, "https://maps.google.com/?cid=" + id,
                false, false, "{\"placeId\":\"" + id + "\"}");
    }
}
