package me.iofdev.leadhunter.llm;

/**
 * Sends each call to the model of the organization it belongs to (ADR 0044), or to the default model when the
 * operator set none. A request that already names a model keeps it.
 */
public class OrgModelLlmClient implements LlmClient {

    private final LlmClient delegate;
    private final OrgModelRepository models;

    public OrgModelLlmClient(LlmClient delegate, OrgModelRepository models) {
        this.delegate = delegate;
        this.models = models;
    }

    @Override
    public String model() {
        return delegate.model();
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        if (request.model() != null) {
            return delegate.complete(request);
        }
        return delegate.complete(models.modelFor(request).map(request::withModel).orElse(request));
    }
}
