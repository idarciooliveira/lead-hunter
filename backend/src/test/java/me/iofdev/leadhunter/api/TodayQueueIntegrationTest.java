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
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** The today queue and the CSV export over HTTP (ADR 0041), on real Postgres. */
@EnabledIf("me.iofdev.leadhunter.PostgresTestSupport#databaseAvailable")
@SpringBootTest(properties = {"leadhunter.cli.enabled=false", "spring.main.web-application-type=servlet"})
@AutoConfigureMockMvc
@Import(ApiTestAuth.class)
class TodayQueueIntegrationTest extends PostgresTestSupport {

    private static final String CAMPAIGN = """
            slug: clinicas-teste
            name: Clínicas teste
            answers: {sector: clínicas, problem: marcações só por telefone, service: Site}
            search:
              terms: [clínica]
              locations: [Talatona]
            """;

    private static final String OUTRA_CAMPANHA = """
            slug: outra-campanha
            name: Outra campanha
            answers: {sector: oficinas, problem: sem agenda, service: Site}
            search:
              terms: [oficina]
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
        campaigns.save(ORG, parser.parse(CAMPAIGN));
        Campaign campaign = campaigns.findBySlug(ORG, "clinicas-teste").orElseThrow();
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
    void allLeadsComeRankedWithTheExcludedLast() throws Exception {
        lead(campaigns.findBySlug(ORG, "clinicas-teste").orElseThrow(), "e", "Fora", 0, "EXCLUDED", "NEW");

        mvc.perform(get("/api/leads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].name").value("Abaixo do corte"))
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].breakdown").isArray())
                .andExpect(jsonPath("$[1].rank").value(2))
                .andExpect(jsonPath("$[3].name").value("Baixa"))
                .andExpect(jsonPath("$[3].rank").value(4))
                .andExpect(jsonPath("$[4].name").value("Fora"))
                .andExpect(jsonPath("$[4].rank").isEmpty());

        mvc.perform(get("/api/leads").param("limit", "2"))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void allLeadsRankAcrossCampaignsAndPastTheOldPerCampaignCap() throws Exception {
        campaigns.save(ORG, parser.parse(OUTRA_CAMPANHA));
        Campaign outra = campaigns.findBySlug(ORG, "outra-campanha").orElseThrow();
        Campaign teste = campaigns.findBySlug(ORG, "clinicas-teste").orElseThrow();
        // 250 leads in one campaign, above the old cap of 200 per campaign: even scores 1002..1500, no ties.
        for (int i = 1; i <= 250; i++) {
            lead(teste, "massa-" + i, "Massa " + i, 1000 + 2 * i, "QUALIFIED", "NEW");
        }
        // Scores interleave with the first campaign: 200 leads of the big one score above 1101.
        lead(outra, "entre", "Entre", 1101, "QUALIFIED", "NEW");
        lead(outra, "topo", "Topo de outra", 99, "QUALIFIED", "NEW");
        lead(outra, "fora-outra", "Fora de outra", 2000, "EXCLUDED", "NEW");

        mvc.perform(get("/api/leads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(257))
                .andExpect(jsonPath("$[0].name").value("Massa 250"))
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[200].name").value("Entre"))
                .andExpect(jsonPath("$[200].rank").value(201))
                .andExpect(jsonPath("$[249].name").value("Massa 2"))
                .andExpect(jsonPath("$[249].rank").value(250))
                .andExpect(jsonPath("$[250].name").value("Massa 1"))
                .andExpect(jsonPath("$[250].rank").value(251))
                .andExpect(jsonPath("$[251].name").value("Topo de outra"))
                .andExpect(jsonPath("$[251].rank").value(252))
                .andExpect(jsonPath("$[255].name").value("Baixa"))
                .andExpect(jsonPath("$[255].rank").value(256))
                .andExpect(jsonPath("$[256].name").value("Fora de outra"))
                .andExpect(jsonPath("$[256].rank").isEmpty());

        mvc.perform(get("/api/leads").param("limit", "300"))
                .andExpect(jsonPath("$.length()").value(257));
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
