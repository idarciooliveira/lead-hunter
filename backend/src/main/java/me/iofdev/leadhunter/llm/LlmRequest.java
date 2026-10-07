package me.iofdev.leadhunter.llm;

/**
 * @param json       ask the model for a JSON object instead of free text
 * @param campaignId the campaign this call is for, or null. Used to attribute spend in {@code usage}.
 * @param orgId      the organization to charge when there is no campaign, or null. A campaign's own organization wins.
 * @param purpose    what the call is for, such as {@code pitch}. Used to group spend in {@code usage}.
 * @param model      the model to use, or null for the organization's model or the default. {@link OrgModelLlmClient} fills it in.
 */
public record LlmRequest(
        String system, String user, double temperature, int maxTokens, boolean json, Long campaignId, String orgId,
        String purpose, String model) {

    public static LlmRequest text(String system, String user) {
        return new LlmRequest(system, user, 0.4, 800, false, null, null, "other", null);
    }

    public static LlmRequest json(String system, String user) {
        return new LlmRequest(system, user, 0.0, 800, true, null, null, "other", null);
    }

    public LlmRequest forCampaign(Long campaignId, String purpose) {
        return new LlmRequest(system, user, temperature, maxTokens, json, campaignId, null, purpose, model);
    }

    /** For a call that belongs to an organization but to no campaign, like {@code llm test}. */
    public LlmRequest forOrg(String orgId, String purpose) {
        return new LlmRequest(system, user, temperature, maxTokens, json, null, orgId, purpose, model);
    }

    public LlmRequest withModel(String model) {
        return new LlmRequest(system, user, temperature, maxTokens, json, campaignId, orgId, purpose, model);
    }
}
