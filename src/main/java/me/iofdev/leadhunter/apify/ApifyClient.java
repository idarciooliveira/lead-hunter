package me.iofdev.leadhunter.apify;

import java.math.BigDecimal;
import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

/** Thin wrapper over the Apify REST API v2: start an actor run, poll it, read its dataset. */
public class ApifyClient {

    private final RestClient http;
    private final String token;

    public ApifyClient(RestClient http, String token) {
        this.http = http;
        this.token = token;
    }

    public ApifyRun startRun(String actorId, Object input) {
        JsonNode body = http.post()
                .uri("/v2/acts/{actorId}/runs", actorId.replace('/', '~'))
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .body(input)
                .retrieve()
                .body(JsonNode.class);
        return toRun(body);
    }

    public ApifyRun getRun(String runId, Duration waitForFinish) {
        JsonNode body = http.get()
                .uri("/v2/actor-runs/{runId}?waitForFinish={seconds}", runId, waitForFinish.toSeconds())
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .retrieve()
                .body(JsonNode.class);
        return toRun(body);
    }

    public JsonNode datasetItems(String datasetId) {
        return http.get()
                .uri("/v2/datasets/{datasetId}/items?clean=true&format=json", datasetId)
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .retrieve()
                .body(JsonNode.class);
    }

    private String bearer() {
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("APIFY_TOKEN is not set");
        }
        return "Bearer " + token;
    }

    private static ApifyRun toRun(JsonNode body) {
        JsonNode data = body == null ? null : body.get("data");
        if (data == null || !data.hasNonNull("id")) {
            throw new IllegalStateException("unexpected Apify response: " + body);
        }
        JsonNode usage = data.get("usageTotalUsd");
        return new ApifyRun(
                data.get("id").asString(),
                data.path("status").asString(""),
                data.path("defaultDatasetId").asString(""),
                usage != null && usage.isNumber() ? usage.decimalValue() : BigDecimal.ZERO);
    }
}
