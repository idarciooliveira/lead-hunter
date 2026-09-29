package me.iofdev.leadhunter.llm;

import java.math.BigDecimal;

/**
 * The model's answer and what the gateway reports about the call.
 *
 * @param costUsd      what the call cost, as reported by the provider. Null when the response has no cost.
 * @param generationId the provider's id for this call, for looking it up later
 * @param rawUsage     the provider's {@code usage} block as JSON, kept so parsing can be fixed later
 */
public record LlmResponse(
        String text,
        String model,
        int promptTokens,
        int completionTokens,
        BigDecimal costUsd,
        String generationId,
        String rawUsage) {
}
