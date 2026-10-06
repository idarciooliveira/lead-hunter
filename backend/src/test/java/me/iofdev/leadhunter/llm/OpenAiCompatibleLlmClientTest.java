package me.iofdev.leadhunter.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.time.Duration;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiCompatibleLlmClientTest {

    private static final String BASE = "https://gateway.test/v1";
    private static final String MODEL = "google/gemma-4-26b-a4b-it";

    private MockRestServiceServer server;
    private OpenAiCompatibleLlmClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OpenAiCompatibleLlmClient(builder.build(), "gw-key", MODEL, Duration.ZERO);
    }

    private static final String OK = """
            {"id": "chatcmpl-1", "model": "google/gemma-4-26b-a4b-it",
             "choices": [{"index": 0, "message": {"role": "assistant", "content": "  Bom dia!  "}, "finish_reason": "stop"}],
             "usage": {"prompt_tokens": 42, "completion_tokens": 7, "total_tokens": 49}}
            """;

    @Test
    void sendsAChatCompletionToTheGateway() {
        server.expect(requestTo(BASE + "/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer gw-key"))
                .andExpect(jsonPath("$.model").value(MODEL))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[0].content").value("És um vendedor."))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andExpect(jsonPath("$.max_tokens").value(800))
                .andExpect(jsonPath("$.response_format").doesNotExist())
                .andRespond(withSuccess(OK, MediaType.APPLICATION_JSON));

        LlmResponse response = client.complete(LlmRequest.text("És um vendedor.", "Olá"));

        assertThat(response.text()).isEqualTo("Bom dia!");
        assertThat(response.model()).isEqualTo(MODEL);
        assertThat(response.promptTokens()).isEqualTo(42);
        assertThat(response.completionTokens()).isEqualTo(7);
        server.verify();
    }

    @Test
    void readsTheCostTheGatewayReports() {
        server.expect(requestTo(BASE + "/chat/completions"))
                .andRespond(withSuccess("""
                        {"id": "gen_1", "generationId": "gen_1", "model": "google/gemma-4-26b-a4b-it",
                         "choices": [{"index": 0, "message": {"role": "assistant", "content": "Olá",
                           "provider_metadata": {"gateway": {"cost": "0.00000771", "generationId": "gen_1"}}}}],
                         "usage": {"prompt_tokens": 15, "completion_tokens": 10, "cost": 7.71e-06, "is_byok": false}}
                        """, MediaType.APPLICATION_JSON));

        LlmResponse response = client.complete(LlmRequest.text(null, "Olá"));

        assertThat(response.costUsd()).isEqualByComparingTo("0.00000771");
        assertThat(response.generationId()).isEqualTo("gen_1");
        assertThat(response.rawUsage()).contains("\"cost\"").contains("\"prompt_tokens\":15");
    }

    @Test
    void fallsBackToTheCostInTheGatewayMetadata() {
        server.expect(requestTo(BASE + "/chat/completions"))
                .andRespond(withSuccess("""
                        {"id": "gen_2", "model": "google/gemma-4-26b-a4b-it",
                         "choices": [{"index": 0, "message": {"role": "assistant", "content": "Olá",
                           "provider_metadata": {"gateway": {"cost": "0.0004"}}}}],
                         "usage": {"prompt_tokens": 1, "completion_tokens": 1}}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.complete(LlmRequest.text(null, "Olá")).costUsd()).isEqualByComparingTo("0.0004");
    }

    @Test
    void leavesTheCostEmptyWhenTheProviderSendsNone() {
        server.expect(requestTo(BASE + "/chat/completions"))
                .andRespond(withSuccess(OK, MediaType.APPLICATION_JSON));

        LlmResponse response = client.complete(LlmRequest.text(null, "Olá"));

        assertThat(response.costUsd()).isNull();
        assertThat(response.generationId()).isEqualTo("chatcmpl-1");
    }

    @Test
    void asksForJsonAndSkipsAnEmptySystemPrompt() {
        server.expect(requestTo(BASE + "/chat/completions"))
                .andExpect(jsonPath("$.messages", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.response_format.type").value("json_object"))
                .andExpect(jsonPath("$.temperature").value(0.0))
                .andRespond(withSuccess(OK, MediaType.APPLICATION_JSON));

        client.complete(LlmRequest.json(null, "Classifica estas avaliações"));

        server.verify();
    }

    @Test
    void reportsGatewayErrorsWithStatusAndBody() {
        server.expect(requestTo(BASE + "/chat/completions"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\": {\"message\": \"Invalid API key\"}}"));

        assertThatThrownBy(() -> client.complete(LlmRequest.text(null, "Olá")))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("401")
                .hasMessageContaining("Invalid API key");
    }

    @Test
    void triesAgainAfterANetworkErrorAndAGatewayFailure() {
        server.expect(requestTo(BASE + "/chat/completions"))
                .andRespond(withException(new IOException("Request cancelled")));
        server.expect(requestTo(BASE + "/chat/completions"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(requestTo(BASE + "/chat/completions"))
                .andRespond(withSuccess(OK, MediaType.APPLICATION_JSON));

        assertThat(client.complete(LlmRequest.text(null, "Olá")).text()).isEqualTo("Bom dia!");
        server.verify();
    }

    @Test
    void givesUpAfterThreeFailedAttempts() {
        for (int i = 0; i < OpenAiCompatibleLlmClient.MAX_ATTEMPTS; i++) {
            server.expect(requestTo(BASE + "/chat/completions"))
                    .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("slow down"));
        }

        assertThatThrownBy(() -> client.complete(LlmRequest.text(null, "Olá")))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("429");
        server.verify();
    }

    @Test
    void doesNotRetryAClientError() {
        server.expect(requestTo(BASE + "/chat/completions"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.complete(LlmRequest.text(null, "Olá")))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("400");
        server.verify();
    }

    @Test
    void rejectsResponsesWithoutContent() {
        server.expect(requestTo(BASE + "/chat/completions"))
                .andRespond(withSuccess("{\"choices\": []}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.complete(LlmRequest.text(null, "Olá")))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("no message content");
    }

    @Test
    void refusesToCallWithoutAKey() {
        OpenAiCompatibleLlmClient noKey = new OpenAiCompatibleLlmClient(RestClient.create(BASE), " ", MODEL);

        assertThatThrownBy(() -> noKey.complete(LlmRequest.text(null, "Olá")))
                .hasMessageContaining("AI_GATEWAY_API_KEY");
    }
}
