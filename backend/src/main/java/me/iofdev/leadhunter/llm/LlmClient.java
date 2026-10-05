package me.iofdev.leadhunter.llm;

/** One chat completion. The rest of the code depends on this, not on a provider. */
public interface LlmClient {

    LlmResponse complete(LlmRequest request);

    /** The model id requests go to, for logs and output. */
    String model();
}
