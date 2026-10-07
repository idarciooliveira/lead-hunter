package me.iofdev.leadhunter.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import me.iofdev.leadhunter.PostgresTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** ADR 0037: every {@code /api/**} call needs the service token, except the health probe. This test sends the header itself. */
@EnabledIf("me.iofdev.leadhunter.PostgresTestSupport#databaseAvailable")
@SpringBootTest(properties = {"leadhunter.cli.enabled=false", "spring.main.web-application-type=servlet"})
@AutoConfigureMockMvc
class ServiceTokenIntegrationTest extends PostgresTestSupport {

    @Autowired
    MockMvc mvc;

    @Test
    void healthIsOpen() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void aCallWithoutTheTokenIsRefused() throws Exception {
        mvc.perform(get("/api/campaigns"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("missing or wrong API token"));
    }

    @Test
    void aWrongTokenIsRefused() throws Exception {
        mvc.perform(get("/api/campaigns").header("Authorization", "Bearer nope")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/campaigns").header("Authorization", ApiTestAuth.TOKEN)).andExpect(status().isUnauthorized());
    }

    @Test
    void theRightTokenGetsPastTheFilter() throws Exception {
        mvc.perform(get("/api/campaigns").header("Authorization", "Bearer " + ApiTestAuth.TOKEN)
                        .header("X-LeadHunter-User", ApiTestAuth.USER_ID)
                        .header("X-LeadHunter-Org", ApiTestAuth.ORG_ID))
                .andExpect(status().isOk());
    }

    @Test
    void theRightTokenWithoutAUserAndOrganizationIsForbidden() throws Exception {
        mvc.perform(get("/api/campaigns").header("Authorization", "Bearer " + ApiTestAuth.TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("not a member of this organization"));
    }

    @Test
    void theBrowserCannotPreflightAnOrigin() throws Exception {
        mvc.perform(options("/api/leads/1")
                        .header("Authorization", "Bearer " + ApiTestAuth.TOKEN)
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
