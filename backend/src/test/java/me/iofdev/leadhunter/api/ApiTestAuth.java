package me.iofdev.leadhunter.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;

/** Gives every MockMvc call of a test the service token, which {@link me.iofdev.leadhunter.PostgresTestSupport} configures. */
@TestConfiguration
public class ApiTestAuth {

    public static final String TOKEN = "test-service-token";

    @Bean
    MockMvcBuilderCustomizer serviceToken() {
        return builder -> builder.defaultRequest(
                get("/")
                        .header("Authorization", "Bearer " + TOKEN));
    }
}
