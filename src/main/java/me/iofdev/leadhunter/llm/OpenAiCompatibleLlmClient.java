package me.iofdev.leadhunter.llm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

/**
 * Calls {@code POST /chat/completions} on an OpenAI-compatible API. Used with the Vercel AI Gateway,
 * so switching models or providers is a change to {@code LEADHUNTER_LLM_MODEL}. See ADR 0017.
 */
public class OpenAiCompatibleLlmClient implements LlmClient {

    private final RestClient http;
    private final String apiKey;
    private final String model;

    public OpenAiCompatibleLlmClient(RestClient http, String apiKey, String model) {
        this.http = http;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("AI_GATEWAY_API_KEY is not set. Create a key in the Vercel dashboard under AI Gateway");
        }
        JsonNode body;
        try {
            body = http.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload(request))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            throw new LlmException("LLM gateway returned " + e.getStatusCode().value() + ": "
                    + abbreviate(e.getResponseBodyAsString()), e);
        }
        return parse(body);
    }

    private Map<String, Object> payload(LlmRequest request) {
        List<Map<String, String>> messages = new ArrayList<>();
        if (request.system() != null && !request.system().isBlank()) {
            messages.add(Map.of("role", "system", "content", request.system()));
        }
        messages.add(Map.of("role", "user", "content", request.user()));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("messages", messages);
        payload.put("temperature", request.temperature());
        payload.put("max_tokens", request.maxTokens());
        if (request.json()) {
            payload.put("response_format", Map.of("type", "json_object"));
        }
        return payload;
    }

    private LlmResponse parse(JsonNode body) {
        JsonNode content = body == null ? null : body.path("choices").path(0).path("message").get("content");
        if (content == null || !content.isString()) {
            throw new LlmException("LLM gateway response has no message content: " + abbreviate(String.valueOf(body)));
        }
        JsonNode usage = body.path("usage");
        return new LlmResponse(
                content.asString().trim(),
                body.path("model").asString(model),
                usage.path("prompt_tokens").asInt(0),
                usage.path("completion_tokens").asInt(0));
    }

    private static String abbreviate(String value) {
        return value.length() <= 300 ? value : value.substring(0, 300) + "…";
    }
}
