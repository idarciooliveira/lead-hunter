package me.iofdev.leadhunter.apify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import me.iofdev.leadhunter.maps.ScrapeException;
import me.iofdev.leadhunter.maps.ScrapeRequest;
import me.iofdev.leadhunter.maps.ScrapeResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ApifyGoogleMapsScraperTest {

    private static final String BASE = "https://api.apify.test";
    private static final ScrapeRequest REQUEST =
            new ScrapeRequest(List.of("clínica", "centro médico"), "Talatona, Luanda, Angola", 40, "pt-PT");

    private MockRestServiceServer server;
    private ApifyGoogleMapsScraper scraper;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        ApifyProperties properties = new ApifyProperties("secret-token", BASE, "compass~crawler-google-places",
                Duration.ofSeconds(60), Duration.ofMinutes(20), new BigDecimal("0.004"), 600);
        scraper = new ApifyGoogleMapsScraper(new ApifyClient(builder.build(), "secret-token"), properties);
    }

    private static String run(String status, String usage) {
        return """
                {"data": {"id": "run-1", "status": "%s", "defaultDatasetId": "ds-1", "usageTotalUsd": %s}}
                """.formatted(status, usage);
    }

    @Test
    void startsPollsAndReadsTheDataset() throws IOException {
        server.expect(requestTo(BASE + "/v2/acts/compass~crawler-google-places/runs"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer secret-token"))
                .andExpect(jsonPath("$.searchStringsArray[1]").value("centro médico"))
                .andExpect(jsonPath("$.locationQuery").value("Talatona, Luanda, Angola"))
                .andExpect(jsonPath("$.maxCrawledPlacesPerSearch").value(40))
                .andExpect(jsonPath("$.maxReviews").value(0))
                .andRespond(withSuccess(run("RUNNING", "0"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/v2/actor-runs/run-1?waitForFinish=60"))
                .andRespond(withSuccess(run("SUCCEEDED", "0.312"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/v2/datasets/ds-1/items?clean=true&format=json"))
                .andRespond(withSuccess(new ClassPathResource("apify/places.json").getContentAsString(StandardCharsets.UTF_8),
                        MediaType.APPLICATION_JSON));

        ScrapeResult result = scraper.search(REQUEST);

        assertThat(result.externalRunId()).isEqualTo("run-1");
        assertThat(result.costUsd()).isEqualByComparingTo("0.312");
        assertThat(result.places()).hasSize(2);
        server.verify();
    }

    @Test
    void failedRunsRaiseWithTheRunId() {
        server.expect(requestTo(BASE + "/v2/acts/compass~crawler-google-places/runs"))
                .andRespond(withSuccess(run("FAILED", "0.01"), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> scraper.search(REQUEST))
                .isInstanceOf(ScrapeException.class)
                .hasMessageContaining("FAILED")
                .extracting(e -> ((ScrapeException) e).externalRunId())
                .isEqualTo("run-1");
    }

    @Test
    void refusesToRunWithoutToken() {
        ApifyProperties noToken = new ApifyProperties("", BASE, "a~b", Duration.ofSeconds(1), Duration.ofMinutes(1),
                BigDecimal.ONE, 10);
        ApifyGoogleMapsScraper withoutToken =
                new ApifyGoogleMapsScraper(new ApifyClient(RestClient.create(BASE), ""), noToken);

        assertThatThrownBy(withoutToken::checkReady).hasMessageContaining("APIFY_TOKEN");
        assertThatThrownBy(() -> withoutToken.search(REQUEST)).hasMessageContaining("APIFY_TOKEN");
    }
}
