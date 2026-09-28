package me.iofdev.leadhunter.llm;

/** The model's answer and the token usage the gateway reports, for cost tracking. */
public record LlmResponse(String text, String model, int promptTokens, int completionTokens) {
}
