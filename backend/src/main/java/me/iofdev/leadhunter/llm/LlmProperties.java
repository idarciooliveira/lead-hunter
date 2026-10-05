package me.iofdev.leadhunter.llm;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param apiKey  Vercel AI Gateway key, from {@code AI_GATEWAY_API_KEY}
 * @param baseUrl any OpenAI-compatible endpoint; defaults to the Vercel AI Gateway. See ADR 0017.
 * @param model   gateway model id in {@code provider/model} form
 * @param timeout how long to wait for one completion
 */
@ConfigurationProperties("leadhunter.llm")
public record LlmProperties(
        String apiKey,
        @DefaultValue("https://ai-gateway.vercel.sh/v1") String baseUrl,
        @DefaultValue("google/gemma-4-26b-a4b-it") String model,
        @DefaultValue("60s") Duration timeout) {
}
