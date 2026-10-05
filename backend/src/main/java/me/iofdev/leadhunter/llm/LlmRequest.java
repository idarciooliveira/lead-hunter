package me.iofdev.leadhunter.llm;

/**
 * @param json       ask the model for a JSON object instead of free text
 * @param campaignId the campaign this call is for, or null. Used to attribute spend in {@code usage}.
 * @param purpose    what the call is for, such as {@code pitch}. Used to group spend in {@code usage}.
 */
public record LlmRequest(
        String system, String user, double temperature, int maxTokens, boolean json, Long campaignId, String purpose) {

    public static LlmRequest text(String system, String user) {
        return new LlmRequest(system, user, 0.4, 800, false, null, "other");
    }

    public static LlmRequest json(String system, String user) {
        return new LlmRequest(system, user, 0.0, 800, true, null, "other");
    }

    public LlmRequest forCampaign(Long campaignId, String purpose) {
        return new LlmRequest(system, user, temperature, maxTokens, json, campaignId, purpose);
    }
}
