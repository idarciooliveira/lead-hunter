package me.iofdev.leadhunter.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import me.iofdev.leadhunter.PostgresTestSupport;
import me.iofdev.leadhunter.campaign.CampaignRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Campaign and company writes (ADR 0029, 0031): the endpoints share the CLI's parsers and checks, and
 * every one runs against real Postgres.
 */
@EnabledIf("me.iofdev.leadhunter.PostgresTestSupport#databaseAvailable")
@SpringBootTest(properties = {"leadhunter.cli.enabled=false", "spring.main.web-application-type=servlet"})
@AutoConfigureMockMvc
@Import(ApiTestAuth.class)
class ApiWriteIntegrationTest extends PostgresTestSupport {

    private static final String COMPANY = """
            {"name":"TchiowaLabs","intro":"Software para PME","entryOffer":"Site",
             "services":[{"name":"Site","price":"400 mil Kz","deliveryTime":"2 semanas"}]}
            """;

    private static final String CAMPAIGN = """
            {"slug":"clinicas-teste","name":"Clínicas teste",
             "answers":{"sector":"clínicas","problem":"marcações só por telefone","service":"Site"},
             "search":{"terms":["clínica"],"locations":["Talatona"]}}
            """;

    @Autowired
    MockMvc mvc;
    @Autowired
    CampaignRepository campaigns;

    @Test
    void savesTheCompanyProfileAndAppliesDefaults() throws Exception {
        mvc.perform(put("/api/company").contentType(MediaType.APPLICATION_JSON).content(COMPANY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved.name").value("TchiowaLabs"))
                .andExpect(jsonPath("$.saved.weeklyCapacity").value(35))
                .andExpect(jsonPath("$.warnings", hasSize(0)));

        mvc.perform(get("/api/company"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryOffer").value("Site"));
    }

    @Test
    void replacesAnExistingCompanyProfile() throws Exception {
        saveCompany();
        mvc.perform(put("/api/company").contentType(MediaType.APPLICATION_JSON)
                        .content(COMPANY.replace("TchiowaLabs", "Outra")))
                .andExpect(status().isOk());

        mvc.perform(get("/api/company")).andExpect(jsonPath("$.name").value("Outra"));
    }

    @Test
    void rejectsAnInvalidCompanyProfileWithEachProblem() throws Exception {
        mvc.perform(put("/api/company").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"intro\":\"x\",\"entryOffer\":\"Site\",\"services\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("invalid company profile")))
                .andExpect(jsonPath("$.problems", hasItem("name is required")))
                .andExpect(jsonPath("$.problems", hasItem("services needs at least one service")));

        mvc.perform(get("/api/company")).andExpect(status().isNotFound());
    }

    @Test
    void warnsAboutClientsWithoutAPhone() throws Exception {
        mvc.perform(put("/api/company").contentType(MediaType.APPLICATION_JSON)
                        .content(COMPANY.replace("\"entryOffer\"", "\"clients\":[{\"name\":\"Clínica Sol\"}],\"entryOffer\"")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warnings[0]", containsString("Clínica Sol")));
    }

    @Test
    void createsACampaign() throws Exception {
        saveCompany();
        mvc.perform(post("/api/campaigns").contentType(MediaType.APPLICATION_JSON).content(CAMPAIGN))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saved.slug").value("clinicas-teste"))
                .andExpect(jsonPath("$.saved.search.maxPlacesPerSearch").value(40))
                .andExpect(jsonPath("$.saved.qualifiedCount").value(0))
                .andExpect(jsonPath("$.warnings", hasSize(0)));

        mvc.perform(get("/api/campaigns/clinicas-teste")).andExpect(status().isOk());
    }

    @Test
    void creatingAnExistingSlugIsAConflict() throws Exception {
        saveCompany();
        createCampaign();
        mvc.perform(post("/api/campaigns").contentType(MediaType.APPLICATION_JSON).content(CAMPAIGN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void rejectsAnInvalidCampaignWithEachProblem() throws Exception {
        saveCompany();
        mvc.perform(post("/api/campaigns").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"Bad Slug\",\"name\":\"\",\"answers\":{\"sector\":\"x\"},\"search\":{}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.problems", hasItem("name is required")))
                .andExpect(jsonPath("$.problems", hasItem("answers.problem is required")))
                .andExpect(jsonPath("$.problems", hasItem("search.terms needs at least one term")));

        org.assertj.core.api.Assertions.assertThat(campaigns.findAll(ORG)).isEmpty();
    }

    @Test
    void rejectsAServiceTheCompanyDoesNotSell() throws Exception {
        saveCompany();
        mvc.perform(post("/api/campaigns").contentType(MediaType.APPLICATION_JSON)
                        .content(CAMPAIGN.replace("\"service\":\"Site\"", "\"service\":\"Drone\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.problems[0]", containsString("not one of the company's services")));
    }

    @Test
    void needsACompanyProfileFirst() throws Exception {
        mvc.perform(post("/api/campaigns").contentType(MediaType.APPLICATION_JSON).content(CAMPAIGN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("no company profile yet")));
    }

    @Test
    void updatesACampaignKeepingItsSlug() throws Exception {
        saveCompany();
        createCampaign();
        mvc.perform(put("/api/campaigns/{slug}", "clinicas-teste").contentType(MediaType.APPLICATION_JSON)
                        .content(CAMPAIGN.replace("Clínicas teste", "Clínicas renomeadas")
                                .replace("\"slug\":\"clinicas-teste\",", "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved.name").value("Clínicas renomeadas"))
                .andExpect(jsonPath("$.saved.slug").value("clinicas-teste"));
    }

    @Test
    void updateRejectsAChangedSlugAndAMissingCampaign() throws Exception {
        saveCompany();
        createCampaign();
        mvc.perform(put("/api/campaigns/{slug}", "clinicas-teste").contentType(MediaType.APPLICATION_JSON)
                        .content(CAMPAIGN.replace("\"clinicas-teste\"", "\"outro\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("slug cannot change")));

        mvc.perform(put("/api/campaigns/{slug}", "nao-existe").contentType(MediaType.APPLICATION_JSON).content(CAMPAIGN))
                .andExpect(status().isNotFound());
    }

    @Test
    void brokenJsonIsABadRequest() throws Exception {
        mvc.perform(put("/api/company").contentType(MediaType.APPLICATION_JSON).content("{nope"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("invalid JSON body"));
    }

    private void saveCompany() throws Exception {
        mvc.perform(put("/api/company").contentType(MediaType.APPLICATION_JSON).content(COMPANY))
                .andExpect(status().isOk());
    }

    private void createCampaign() throws Exception {
        mvc.perform(post("/api/campaigns").contentType(MediaType.APPLICATION_JSON).content(CAMPAIGN))
                .andExpect(status().isCreated());
    }
}
