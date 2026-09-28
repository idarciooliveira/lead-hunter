package me.iofdev.leadhunter.llm;

/**
 * @param json ask the model for a JSON object instead of free text
 */
public record LlmRequest(String system, String user, double temperature, int maxTokens, boolean json) {

    public static LlmRequest text(String system, String user) {
        return new LlmRequest(system, user, 0.4, 800, false);
    }

    public static LlmRequest json(String system, String user) {
        return new LlmRequest(system, user, 0.0, 800, true);
    }
}
