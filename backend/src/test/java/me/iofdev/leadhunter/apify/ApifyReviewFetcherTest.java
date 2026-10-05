package me.iofdev.leadhunter.apify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import me.iofdev.leadhunter.maps.ReviewFetcher.ReviewsResult;
import me.iofdev.leadhunter.maps.ScrapeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ApifyReviewFetcherTest {

    private static final String BASE = "https://api.apify.test";
    private static final String SORRISO = "https://maps.google.com/?cid=p1";
    private static final String VIDA = "https://maps.google.com/?cid=p2";

    private MockRestServiceServer server;
    private ApifyReviewFetcher fetcher;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        ApifyProperties properties = new ApifyProperties("secret-token", BASE, "compass~crawler-google-places",
                Duration.ofSeconds(60), Duration.ofMinutes(20), Duration.ZERO, new BigDecimal("0.004"), 600);
        fetcher = new ApifyReviewFetcher(new ApifyClient(builder.build(), "secret-token"), properties);
    }

    private static String run(String status, String usage) {
        return """
                {"data": {"id": "run-9", "status": "%s", "defaultDatasetId": "ds-9", "usageTotalUsd": %s}}
                """.formatted(status, usage);
    }

    @Test
    void sendsPlaceUrlsAndMapsReviewsByUrl() {
        server.expect(requestTo(BASE + "/v2/acts/compass~crawler-google-places/runs"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.startUrls[0].url").value(SORRISO))
                .andExpect(jsonPath("$.startUrls[1].url").value(VIDA))
                .andExpect(jsonPath("$.maxReviews").value(10))
                .andExpect(jsonPath("$.maxImages").value(0))
                .andExpect(jsonPath("$.language").value("pt-PT"))
                .andRespond(withSuccess(run("SUCCEEDED", "0"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/v2/actor-runs/run-9?waitForFinish=0"))
                .andRespond(withSuccess(run("SUCCEEDED", "0.11"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/v2/datasets/ds-9/items?clean=true&format=json"))
                .andRespond(withSuccess("""
                        [{"url": "%s", "title": "Clínica Sorriso",
                          "reviews": [
                            {"stars": 2, "text": "Ninguém atende o telefone", "publishedAtDate": "2026-08-01"},
                            {"stars": 4, "text": "Bom atendimento", "publishedAtDate": "2026-08-02"}]},
                         {"url": "%s", "title": "Clínica Vida", "reviews": []},
                         {"url": "https://maps.google.com/?cid=other", "title": "Other", "reviews": [
                            {"stars": 1, "text": "Should not be attached", "publishedAtDate": "2026-08-03"}]}]
                        """.formatted(SORRISO, VIDA), MediaType.APPLICATION_JSON));

        ReviewsResult result = fetcher.fetchReviews(List.of(SORRISO, VIDA), 10, "pt-PT");

        assertThat(result.externalRunId()).isEqualTo("run-9");
        assertThat(result.costUsd()).isEqualByComparingTo("0.11");
        assertThat(result.reviewsByUrl().keySet()).containsExactlyInAnyOrder(SORRISO, VIDA);
        assertThat(result.reviewsByUrl().get(SORRISO)).extracting("text")
                .containsExactly("Ninguém atende o telefone", "Bom atendimento");
        assertThat(result.reviewsByUrl().get(VIDA)).isEmpty();
        server.verify();
    }

    @Test
    void failedRunsRaiseWithTheRunId() {
        server.expect(requestTo(BASE + "/v2/acts/compass~crawler-google-places/runs"))
                .andRespond(withSuccess(run("FAILED", "0.01"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/v2/actor-runs/run-9?waitForFinish=0"))
                .andRespond(withSuccess(run("FAILED", "0.02"), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> fetcher.fetchReviews(List.of(SORRISO), 10, "pt-PT"))
                .isInstanceOf(ScrapeException.class)
                .satisfies(e -> assertThat(((ScrapeException) e).costUsd()).isEqualByComparingTo("0.02"));
        server.verify();
    }

    @Test
    void refusesToRunWithoutToken() {
        ApifyProperties noToken = new ApifyProperties("", BASE, "a~b", Duration.ofSeconds(1), Duration.ofMinutes(1),
                Duration.ZERO, BigDecimal.ONE, 10);
        ApifyReviewFetcher withoutToken = new ApifyReviewFetcher(new ApifyClient(RestClient.create(BASE), ""), noToken);

        assertThatThrownBy(withoutToken::checkReady).hasMessageContaining("APIFY_TOKEN");
        assertThatThrownBy(() -> withoutToken.fetchReviews(List.of(SORRISO), 10, "pt-PT"))
                .hasMessageContaining("APIFY_TOKEN");
    }

    @Test
    void inputShape() {
        Map<String, Object> input = ApifyReviewFetcher.input(List.of(SORRISO), 5, "pt-PT");

        assertThat(input).containsEntry("maxReviews", 5).containsEntry("maxImages", 0)
                .containsEntry("scrapeContacts", false).containsEntry("language", "pt-PT");
        assertThat(input.get("startUrls")).isEqualTo(List.of(Map.of("url", SORRISO)));
    }
}
