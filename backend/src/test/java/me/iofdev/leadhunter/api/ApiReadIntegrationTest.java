package me.iofdev.leadhunter.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import me.iofdev.leadhunter.PostgresTestSupport;
import me.iofdev.leadhunter.campaign.Campaign;
import me.iofdev.leadhunter.campaign.CampaignFileParser;
import me.iofdev.leadhunter.campaign.CampaignRepository;
import me.iofdev.leadhunter.company.CompanyProfile;
import me.iofdev.leadhunter.company.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * HTTP end-to-end coverage for the read API: every endpoint against real
 * Postgres, no Apify or LLM calls. Mirrors the CLI behaviour in ADR 0031.
 */
@EnabledIf("me.iofdev.leadhunter.PostgresTestSupport#databaseAvailable")
@SpringBootTest(properties = {"leadhunter.cli.enabled=false", "spring.main.web-application-type=servlet"})
@AutoConfigureMockMvc
@Import(ApiTestAuth.class)
class ApiReadIntegrationTest extends PostgresTestSupport {

    private static final String CAMPAIGN = """
            slug: clinicas-teste
            name: Clínicas teste
            answers: {sector: clínicas, problem: marcações só por telefone, service: Site}
            search:
              terms: [clínica]
              locations: [Talatona]
            """;

    @Autowired
    MockMvc mvc;
    @Autowired
    CampaignRepository campaigns;
    @Autowired
    CampaignFileParser parser;
    @Autowired
    CompanyRepository companies;

    Campaign campaign;
    long qualifiedLeadId;

    @BeforeEach
    void seed() {
        campaigns.save(ORG, parser.parse(CAMPAIGN));
        campaign = campaigns.findBySlug(ORG, "clinicas-teste").orElseThrow();

        long place = jdbc.sql("""
                        insert into place (google_place_id, name, category, address, neighborhood,
                            phone_e164, phone_mobile, website, website_kind, rating, reviews_count, maps_url, raw)
                        values ('p1', 'Clínica Sorriso', 'Clínica', 'Rua 1', 'Talatona',
                            '+244923456789', true, null, 'NONE', 4.3, 142, 'https://maps.example/p1', '{}')
                        returning id
                        """)
                .query(Long.class)
                .single();
        long other = jdbc.sql("""
                        insert into place (google_place_id, name, category, address, neighborhood,
                            phone_e164, phone_mobile, website, website_kind, rating, reviews_count, maps_url, raw)
                        values ('p2', 'Clínica Nova', 'Clínica', 'Rua 2', 'Maianga',
                            '+244923000111', false, 'https://nova.ao', 'OWN', 4.0, 2, 'https://maps.example/p2', '{}')
                        returning id
                        """)
                .query(Long.class)
                .single();
        qualifiedLeadId = jdbc.sql("""
                        insert into lead (campaign_id, place_id, stage, score, score_breakdown)
                        values (:campaignId, :placeId, 'QUALIFIED', 65,
                            cast(:breakdown as jsonb))
                        returning id
                        """)
                .param("campaignId", campaign.id())
                .param("placeId", place)
                .param("breakdown", "[{\"code\":\"MOBILE_PHONE\",\"points\":5,\"reason\":\"Telefone móvel\"}]")
                .query(Long.class)
                .single();
        jdbc.sql("""
                        insert into lead (campaign_id, place_id, stage, score, score_breakdown, stage_reason)
                        values (:campaignId, :placeId, 'BELOW_CUT', 20, cast('[]' as jsonb), 'Ranked 2 of 2')
                        """)
                .param("campaignId", campaign.id())
                .param("placeId", other)
                .update();

        jdbc.sql("""
                        insert into campaign_run (org_id, campaign_id, location, search_terms, max_places, status, places_found, cost_usd)
                        values (:orgId, :campaignId, 'Talatona', :terms, 40, 'SUCCEEDED', 8, 0.20)
                        """)
                .param("orgId", ORG.value())
                .param("campaignId", campaign.id())
                .param("terms", new String[]{"clínica"})
                .update();
        jdbc.sql("""
                        insert into llm_call (org_id, campaign_id, purpose, model, prompt_tokens, completion_tokens, cost_usd)
                        values ('test-org', :campaignId, 'review-analysis', 'google/gemma-4-26b-a4b-it', 100, 50, 0.01)
                        """)
                .param("campaignId", campaign.id())
                .update();
    }

    @Test
    void showsTheFunnelOnTheSingleCampaignOnly() throws Exception {
        long excludedPlace = jdbc.sql("""
                        insert into place (google_place_id, name, website_kind, reviews_count, raw)
                        values ('p3', 'Banco', 'NONE', 900, '{}') returning id
                        """)
                .query(Long.class).single();
        // Enriched, then a rerun excluded it: it keeps enriched_at but no longer counts as enriched.
        jdbc.sql("""
                        insert into lead (campaign_id, place_id, stage, score, score_breakdown, stage_reason, enriched_at)
                        values (:campaignId, :placeId, 'EXCLUDED', 0, cast('[]' as jsonb), 'Chain or client', now())
                        """)
                .param("campaignId", campaign.id())
                .param("placeId", excludedPlace)
                .update();
        jdbc.sql("update lead set enriched_at = now() where id = :id").param("id", qualifiedLeadId).update();

        // A failed job whose one part has a known cost and one part without: the known cost still counts.
        long failedJob = jdbc.sql("""
                        insert into campaign_run (org_id, campaign_id, status, cost_usd)
                        values (:orgId, :campaignId, 'FAILED', null) returning id
                        """)
                .param("orgId", ORG.value())
                .param("campaignId", campaign.id())
                .query(Long.class)
                .single();
        jdbc.sql("""
                        insert into campaign_run (org_id, campaign_id, parent_id, location, search_terms, max_places, status, cost_usd)
                        values (:orgId, :campaignId, :parentId, 'Kilamba', :terms, 40, 'SUCCEEDED', 0.15),
                               (:orgId, :campaignId, :parentId, 'Maianga', :terms, 40, 'FAILED', null)
                        """)
                .param("orgId", ORG.value())
                .param("campaignId", campaign.id())
                .param("parentId", failedJob)
                .param("terms", new String[]{"clínica"})
                .update();

        mvc.perform(get("/api/campaigns/clinicas-teste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.funnel.scraped").value(3))
                .andExpect(jsonPath("$.funnel.kept").value(2))
                .andExpect(jsonPath("$.funnel.excluded").value(1))
                .andExpect(jsonPath("$.funnel.excludedBy[0].reason").value("Chain or client"))
                .andExpect(jsonPath("$.funnel.excludedBy[0].leads").value(1))
                .andExpect(jsonPath("$.funnel.cutShare").value(0.4))
                .andExpect(jsonPath("$.funnel.qualified").value(1))
                .andExpect(jsonPath("$.funnel.enriched").value(1))
                .andExpect(jsonPath("$.funnel.scrapeCostUsd").value(0.35))
                .andExpect(jsonPath("$.funnel.enriching").value(false));

        mvc.perform(get("/api/campaigns"))
                .andExpect(jsonPath("$[0].funnel").value(nullValue()));
    }

    @Test
    void healthIsOpen() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void listsAndShowsCampaigns() throws Exception {
        mvc.perform(get("/api/campaigns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("clinicas-teste"))
                .andExpect(jsonPath("$[0].answers.sector").value("clínicas"))
                .andExpect(jsonPath("$[0].qualifiedCount").value(1))
                .andExpect(jsonPath("$[0].latestRun.kind").value("SCRAPE"))
                .andExpect(jsonPath("$[0].latestRun.status").value("DONE"));

        mvc.perform(get("/api/campaigns/clinicas-teste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Clínicas teste"))
                .andExpect(jsonPath("$.search.terms[0]").value("clínica"));

        // A dry run is no real work, so a campaign with only dry runs never ran.
        campaigns.save(ORG, parser.parse(CAMPAIGN.replace("clinicas-teste", "so-dry-run")));
        jdbc.sql("insert into campaign_run (org_id, campaign_id, kind, status, places_found) values (:org, :id, 'DRY_RUN', 'SUCCEEDED', 40)")
                .param("org", ORG.value())
                .param("id", campaigns.findBySlug(ORG, "so-dry-run").orElseThrow().id())
                .update();
        mvc.perform(get("/api/campaigns/so-dry-run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qualifiedCount").value(0))
                .andExpect(jsonPath("$.latestRun").value(nullValue()))
                .andExpect(jsonPath("$.funnel").value(nullValue()));

        mvc.perform(get("/api/campaigns/desconhecida"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("no campaign 'desconhecida'")));
    }

    @Test
    void listsLeadsWithTheSameDefaultsAsTheCli() throws Exception {
        // Default stage is QUALIFIED, like `leads list`.
        mvc.perform(get("/api/campaigns/clinicas-teste/leads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Clínica Sorriso"))
                .andExpect(jsonPath("$[0].score").value(65))
                .andExpect(jsonPath("$[0].breakdown[0].code").value("MOBILE_PHONE"))
                .andExpect(jsonPath("$[0].whatsappLink").value("https://wa.me/244923456789"));

        mvc.perform(get("/api/campaigns/clinicas-teste/leads").param("stage", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mvc.perform(get("/api/campaigns/clinicas-teste/leads").param("stage", "BELOW_CUT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stageReason").value("Ranked 2 of 2"));

        mvc.perform(get("/api/campaigns/clinicas-teste/leads").param("stage", "BOGUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("unknown stage")));

        mvc.perform(get("/api/campaigns/clinicas-teste/leads").param("limit", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        mvc.perform(get("/api/campaigns/desconhecida/leads"))
                .andExpect(status().isNotFound());
    }

    @Test
    void showsStageTwoAndTheCrawlOnlyOnTheSingleLead() throws Exception {
        mvc.perform(get("/api/leads/{id}", qualifiedLeadId))
                .andExpect(jsonPath("$.stage2Breakdown").isEmpty())
                .andExpect(jsonPath("$.websiteCrawl").isEmpty());

        jdbc.sql("""
                        update lead set complaint_kinds = '{contact}', enriched_at = now(), score = 90,
                            score_breakdown = cast(:breakdown as jsonb)
                        where id = :id
                        """)
                .param("id", qualifiedLeadId)
                .param("breakdown", """
                        [{"code":"MOBILE_PHONE","points":5,"reason":"Telefone móvel"},
                         {"code":"NO_HTTPS","points":25,"reason":"Website is plain HTTP, not HTTPS"}]""")
                .update();
        long placeId = jdbc.sql("select place_id from lead where id = :id").param("id", qualifiedLeadId)
                .query(Long.class).single();
        jdbc.sql("""
                        insert into website_crawl (place_id, url, reachable, https, mobile_friendly, stale, http_status)
                        values (:placeId, 'http://old.example', true, false, true, null, 200)
                        """)
                .param("placeId", placeId)
                .update();

        mvc.perform(get("/api/leads/{id}", qualifiedLeadId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.breakdown.length()").value(1))
                .andExpect(jsonPath("$.breakdown[0].code").value("MOBILE_PHONE"))
                .andExpect(jsonPath("$.stage2Breakdown[0].code").value("NO_HTTPS"))
                .andExpect(jsonPath("$.websiteCrawl.url").value("http://old.example"))
                .andExpect(jsonPath("$.websiteCrawl.https").value(false))
                .andExpect(jsonPath("$.websiteCrawl.mobileFriendly").value(true))
                .andExpect(jsonPath("$.websiteCrawl.stale").isEmpty());

        mvc.perform(get("/api/campaigns/clinicas-teste/leads"))
                .andExpect(jsonPath("$[0].stage2Breakdown[0].code").value("NO_HTTPS"))
                .andExpect(jsonPath("$[0].websiteCrawl").isEmpty());
    }

    @Test
    void showsOneLead() throws Exception {
        mvc.perform(get("/api/leads/{id}", qualifiedLeadId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaignSlug").value("clinicas-teste"))
                .andExpect(jsonPath("$.stage").value("QUALIFIED"));

        mvc.perform(get("/api/leads/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("no lead with id")));

        mvc.perform(get("/api/leads/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void showsTheCompanyProfile() throws Exception {
        companies.save(ORG, new CompanyProfile(
                "Exemplo Software",
                "Fazemos sites",
                List.of(new CompanyProfile.Service("Site", "400 mil Kz", "2 semanas")),
                "Site",
                List.of("Luanda"),
                List.of(),
                List.of(),
                List.of(),
                40,
                null));

        mvc.perform(get("/api/company"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Exemplo Software"))
                .andExpect(jsonPath("$.services[0].price").value("400 mil Kz"));
    }

    @Test
    void missingCompanyProfileIsANotFound() throws Exception {
        mvc.perform(get("/api/company"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("no company profile yet")));
    }

    @Test
    void reportsUsage() throws Exception {
        mvc.perform(get("/api/usage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apify.costUsd").value(0.20))
                .andExpect(jsonPath("$.llm.calls").value(1))
                .andExpect(jsonPath("$.totalUsd").value(0.21))
                .andExpect(jsonPath("$.committedUsd").isNumber())
                .andExpect(jsonPath("$.byCampaign[0].slug").value("clinicas-teste"));

        mvc.perform(get("/api/usage/entries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mvc.perform(get("/api/usage").param("month", "2026-13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("--month must look like")));

        mvc.perform(get("/api/usage").param("campaign", "desconhecida"))
                .andExpect(status().isNotFound());
    }
}
