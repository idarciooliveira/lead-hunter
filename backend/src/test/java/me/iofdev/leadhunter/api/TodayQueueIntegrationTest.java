package me.iofdev.leadhunter.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import me.iofdev.leadhunter.PostgresTestSupport;
import me.iofdev.leadhunter.campaign.Campaign;
import me.iofdev.leadhunter.campaign.CampaignFileParser;
import me.iofdev.leadhunter.campaign.CampaignRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** The today queue and the CSV export over HTTP (ADR 0041), on real Postgres. */
@EnabledIf("me.iofdev.leadhunter.PostgresTestSupport#databaseAvailable")
@SpringBootTest(properties = {"leadhunter.cli.enabled=false", "spring.main.web-application-type=servlet"})
@AutoConfigureMockMvc
class TodayQueueIntegrationTest extends PostgresTestSupport {

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

    @BeforeEach
    void seed() {
        campaigns.save(parser.parse(CAMPAIGN));
        Campaign campaign = campaigns.findBySlug("clinicas-teste").orElseThrow();
        lead(campaign, "a", "Baixa", 40, "QUALIFIED", "NEW");
        lead(campaign, "b", "Casa \"Boa\", Lda", 80, "QUALIFIED", "NEW");
        lead(campaign, "c", "Já contactada", 90, "QUALIFIED", "CONTACTED");
        lead(campaign, "d", "Abaixo do corte", 95, "BELOW_CUT", "NEW");
    }

    @Test
    void todayListsQualifiedUntouchedLeadsBestFirstAndHonoursLimit() throws Exception {
        mvc.perform(get("/api/leads/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Casa \"Boa\", Lda"))
                .andExpect(jsonPath("$[1].name").value("Baixa"));

        mvc.perform(get("/api/leads/today").param("limit", "1"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void csvDownloadsTheQualifiedLeadsWithQuoting() throws Exception {
        mvc.perform(get("/api/campaigns/clinicas-teste/leads.csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("clinicas-teste-leads.csv")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"Casa \"\"Boa\"\", Lda\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Já contactada")));

        mvc.perform(get("/api/campaigns/nada/leads.csv")).andExpect(status().isNotFound());
        mvc.perform(get("/api/campaigns/clinicas-teste/leads.csv").param("stage", "x"))
                .andExpect(status().isBadRequest());
    }

    private void lead(Campaign campaign, String gid, String name, int score, String stage, String status) {
        long place = jdbc.sql("""
                        insert into place (google_place_id, name, category, address, neighborhood,
                            phone_e164, phone_mobile, website, website_kind, rating, reviews_count, maps_url, raw)
                        values (:gid, :name, 'Clínica', 'Rua 1', 'Talatona',
                            '+244923456789', true, null, 'NONE', 4.3, 10, 'https://maps.example/p', '{}')
                        returning id
                        """)
                .param("gid", gid)
                .param("name", name)
                .query(Long.class)
                .single();
        jdbc.sql("""
                        insert into lead (campaign_id, place_id, stage, status, score, score_breakdown)
                        values (:campaignId, :placeId, :stage, :status, :score, cast('[]' as jsonb))
                        """)
                .param("campaignId", campaign.id())
                .param("placeId", place)
                .param("stage", stage)
                .param("status", status)
                .param("score", score)
                .update();
    }
}
