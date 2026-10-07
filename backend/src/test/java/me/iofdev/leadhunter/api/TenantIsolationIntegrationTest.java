package me.iofdev.leadhunter.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import me.iofdev.leadhunter.PostgresTestSupport;
import me.iofdev.leadhunter.auth.OrgId;
import me.iofdev.leadhunter.campaign.CampaignFileParser;
import me.iofdev.leadhunter.campaign.CampaignRepository;
import me.iofdev.leadhunter.pipeline.RunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * ADR 0043: every endpoint called as one organization, against the ids and slugs of another, answers not found
 * (or shows nothing), and never changes the other organization's rows. The caller is the test organization;
 * the data belongs to {@code other-org}.
 */
@EnabledIf("me.iofdev.leadhunter.PostgresTestSupport#databaseAvailable")
@SpringBootTest(properties = {"leadhunter.cli.enabled=false", "spring.main.web-application-type=servlet"})
@AutoConfigureMockMvc
@Import(ApiTestAuth.class)
class TenantIsolationIntegrationTest extends PostgresTestSupport {

    private static final OrgId OTHER = new OrgId("other-org");

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
    RunRepository runs;

    private long leadId;
    private long runId;

    @BeforeEach
    void seedTheOtherOrganization() {
        jdbc.sql("insert into organization (id, name, slug) values ('other-org', 'Other', 'other')").update();
        jdbc.sql("insert into app_user (id, name, email) values ('other-user', 'Other', 'other@example.com')").update();
        jdbc.sql("insert into member (id, organization_id, user_id, role) values ('other-member', 'other-org', 'other-user', 'owner')")
                .update();
        jdbc.sql("insert into company (org_id, profile) values ('other-org', '{\"name\": \"Other\"}'::jsonb)").update();
        campaigns.save(OTHER, parser.parse(CAMPAIGN));
        long campaignId = campaigns.findBySlug(OTHER, "clinicas-teste").orElseThrow().id();
        long placeId = jdbc.sql("""
                        insert into place (google_place_id, name, category, address, neighborhood, phone_e164,
                            phone_mobile, website, website_kind, rating, reviews_count, maps_url, raw)
                        values ('p1', 'Clínica Sorriso', 'Clínica', 'Rua 1', 'Talatona', '+244923456789',
                            true, null, 'NONE', 4.3, 10, 'https://maps.example/p', '{}')
                        returning id
                        """)
                .query(Long.class).single();
        leadId = jdbc.sql("""
                        insert into lead (campaign_id, place_id, stage, score, status)
                        values (:campaignId, :placeId, 'QUALIFIED', 80, 'NEW') returning id
                        """)
                .param("campaignId", campaignId).param("placeId", placeId)
                .query(Long.class).single();
        runId = runs.start(campaignId, "Luanda", List.of("clínica"), 40);
        jdbc.sql("update campaign_run set status = 'SUCCEEDED', cost_usd = 5 where id = :id").param("id", runId).update();
        jdbc.sql("""
                        insert into llm_call (org_id, campaign_id, purpose, model, prompt_tokens, completion_tokens, cost_usd)
                        values ('other-org', :campaignId, 'pitch', 'm', 1, 1, 2)
                        """)
                .param("campaignId", campaignId).update();
    }

    @Test
    void campaignsOfAnotherOrganizationAreNotListedNorFound() throws Exception {
        mvc.perform(get("/api/campaigns")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/campaigns/clinicas-teste")).andExpect(status().isNotFound());
        mvc.perform(put("/api/campaigns/clinicas-teste").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/campaigns/clinicas-teste/runs")).andExpect(status().isNotFound());
        mvc.perform(post("/api/campaigns/clinicas-teste/runs").param("dryRun", "true")).andExpect(status().isNotFound());
        mvc.perform(post("/api/campaigns/clinicas-teste/enrichment").param("dryRun", "true"))
                .andExpect(status().isNotFound());
    }

    @Test
    void leadsOfAnotherOrganizationAreNotFoundAndStayUntouched() throws Exception {
        mvc.perform(get("/api/campaigns/clinicas-teste/leads")).andExpect(status().isNotFound());
        mvc.perform(get("/api/campaigns/clinicas-teste/leads.csv")).andExpect(status().isNotFound());
        mvc.perform(get("/api/leads/" + leadId)).andExpect(status().isNotFound());
        mvc.perform(patch("/api/leads/" + leadId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CONTACTED\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/leads/" + leadId + "/pitch")).andExpect(status().isNotFound());
        mvc.perform(get("/api/leads/today")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/leads")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

        org.assertj.core.api.Assertions.assertThat(
                        jdbc.sql("select status from lead where id = :id").param("id", leadId).query(String.class).single())
                .isEqualTo("NEW");
    }

    @Test
    void runsOfAnotherOrganizationAreNotFound() throws Exception {
        mvc.perform(get("/api/runs/" + runId)).andExpect(status().isNotFound());
    }

    @Test
    void theCompanyProfileIsPerOrganization() throws Exception {
        mvc.perform(get("/api/company")).andExpect(status().isNotFound());
    }

    @Test
    void usageCountsOnlyTheCallersOrganization() throws Exception {
        mvc.perform(get("/api/usage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsd").value(0))
                .andExpect(jsonPath("$.byCampaign.length()").value(0));
        mvc.perform(get("/api/usage").param("campaign", "clinicas-teste")).andExpect(status().isNotFound());
        mvc.perform(get("/api/usage/entries")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void aSlugIsFreeInEveryOrganization() throws Exception {
        jdbc.sql("insert into company (org_id, profile) values ('test-org', cast(:profile as jsonb))")
                .param("profile", """
                        {"name": "Mine", "intro": "Fazemos sites", "services": [{"name": "Site", "price": "1 Kz"}],
                         "area": ["Luanda"], "weeklyCapacity": 35}
                        """)
                .update();
        campaigns.save(ORG, parser.parse(CAMPAIGN.replace("Clínicas teste", "Minhas clínicas")));

        org.assertj.core.api.Assertions.assertThat(campaigns.findBySlug(ORG, "clinicas-teste").orElseThrow().name())
                .isEqualTo("Minhas clínicas");
        org.assertj.core.api.Assertions.assertThat(campaigns.findBySlug(OTHER, "clinicas-teste").orElseThrow().name())
                .isEqualTo("Clínicas teste");
    }

    @Test
    void aUserCannotActInAnOrganizationTheyDoNotBelongTo() throws Exception {
        mvc.perform(get("/api/campaigns").header("X-LeadHunter-Org", "other-org"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/campaigns").header("X-LeadHunter-Org", "nope"))
                .andExpect(status().isForbidden());
    }
}
