package me.iofdev.leadhunter.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Outcome writes (ADR 0012, 0020): {@code PATCH /api/leads/{id}} shares
 * {@code LeadRepository.updateOutcome} with {@code leads mark}, so the rules can
 * never drift. Every endpoint runs against real Postgres.
 */
@EnabledIf("me.iofdev.leadhunter.PostgresTestSupport#databaseAvailable")
@SpringBootTest(properties = {"leadhunter.cli.enabled=false", "spring.main.web-application-type=servlet"})
@AutoConfigureMockMvc
@Import(ApiTestAuth.class)
class LeadOutcomeIntegrationTest extends PostgresTestSupport {

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

    long leadId;

    @BeforeEach
    void seed() {
        campaigns.save(parser.parse(CAMPAIGN));
        Campaign campaign = campaigns.findBySlug("clinicas-teste").orElseThrow();
        long place = jdbc.sql("""
                        insert into place (google_place_id, name, category, address, neighborhood,
                            phone_e164, phone_mobile, website, website_kind, rating, reviews_count, maps_url, raw)
                        values ('p1', 'Clínica Sorriso', 'Clínica', 'Rua 1', 'Talatona',
                            '+244923456789', true, null, 'NONE', 4.3, 142, 'https://maps.example/p1', '{}')
                        returning id
                        """)
                .query(Long.class)
                .single();
        leadId = jdbc.sql("""
                        insert into lead (campaign_id, place_id, stage, score, score_breakdown)
                        values (:campaignId, :placeId, 'QUALIFIED', 65, cast('[]' as jsonb))
                        returning id
                        """)
                .param("campaignId", campaign.id())
                .param("placeId", place)
                .query(Long.class)
                .single();
    }

    @Test
    void marksContactedWithANote() throws Exception {
        mvc.perform(patch("/api/leads/{id}", leadId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONTACTED\",\"note\":\"ligou, pediu proposta\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONTACTED"))
                .andExpect(jsonPath("$.note").value("ligou, pediu proposta"))
                .andExpect(jsonPath("$.lostReason").doesNotExist());

        mvc.perform(get("/api/leads/{id}", leadId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONTACTED"));
    }

    @Test
    void marksLostWithANotNowReason() throws Exception {
        mvc.perform(patch("/api/leads/{id}", leadId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"LOST\",\"lostReason\":\"NOT_NOW\",\"note\":\"falar em marco\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOST"))
                .andExpect(jsonPath("$.lostReason").value("NOT_NOW"));
    }

    @Test
    void lostWithoutAReasonIsABadRequest() throws Exception {
        mvc.perform(patch("/api/leads/{id}", leadId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"LOST\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("needs a lost reason")));
    }

    @Test
    void lostReasonWithoutLostIsABadRequest() throws Exception {
        mvc.perform(patch("/api/leads/{id}", leadId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONTACTED\",\"lostReason\":\"NO_BUDGET\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("needs status LOST")));
    }

    @Test
    void workedLeadCannotGoBackToNew() throws Exception {
        mvc.perform(patch("/api/leads/{id}", leadId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONTACTED\"}"))
                .andExpect(status().isOk());

        mvc.perform(patch("/api/leads/{id}", leadId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"NEW\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("would hide the contact history")));
    }

    @Test
    void unknownStatusAndUnknownReasonAreBadRequests() throws Exception {
        mvc.perform(patch("/api/leads/{id}", leadId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CALLED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("unknown status")));

        mvc.perform(patch("/api/leads/{id}", leadId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"LOST\",\"lostReason\":\"LATER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("unknown lost reason")));

        mvc.perform(patch("/api/leads/{id}", leadId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("status is required")));
    }

    @Test
    void malformedJsonIsABadRequest() throws Exception {
        mvc.perform(patch("/api/leads/{id}", leadId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("invalid JSON body"));
    }

    @Test
    void unknownLeadIsANotFound() throws Exception {
        mvc.perform(patch("/api/leads/{id}", 999999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONTACTED\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("no lead with id")));
    }

    @Test
    void theLeadCardCarriesItsPitchAndComplaints() throws Exception {
        jdbc.sql("update lead set pitch = 'Bom dia, podemos falar?', complaint_kinds = '{contact}' where id = :id")
                .param("id", leadId).update();

        mvc.perform(get("/api/leads/{id}", leadId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pitch").value("Bom dia, podemos falar?"))
                .andExpect(jsonPath("$.complaintKinds[0]").value("contact"));
    }

    @Test
    void writingAPitchNeedsAnExistingLeadAndACompanyProfile() throws Exception {
        mvc.perform(post("/api/leads/{id}/pitch", 99999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("no lead with id 99999")));

        mvc.perform(post("/api/leads/{id}/pitch", leadId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("no company profile yet")));
    }
}
