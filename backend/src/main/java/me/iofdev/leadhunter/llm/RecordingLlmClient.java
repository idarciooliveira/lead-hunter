package me.iofdev.leadhunter.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores every completed call in {@code llm_call} so {@code usage} can report spend. Code that needs an LLM
 * keeps depending on {@link LlmClient}. A failed call costs nothing and stores nothing. A failure to store
 * the row is logged and the answer is still returned, because the money is already spent.
 */
public class RecordingLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(RecordingLlmClient.class);

    private final LlmClient delegate;
    private final LlmCallRepository calls;

    public RecordingLlmClient(LlmClient delegate, LlmCallRepository calls) {
        this.delegate = delegate;
        this.calls = calls;
    }

    @Override
    public String model() {
        return delegate.model();
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        LlmResponse response = delegate.complete(request);
        try {
            calls.save(request, response);
        } catch (RuntimeException e) {
            log.warn("could not record the LLM call {}: {}", response.generationId(), e.getMessage());
        }
        return response;
    }
}
