package me.iofdev.leadhunter.llm;

import java.math.BigDecimal;

/** Canned answers for tests. No test calls a real model. */
public class FakeLlmClient implements LlmClient {

    public LlmRequest last;
    public String answer = "{\"complaints\": []}";
    public RuntimeException failure;

    @Override
    public LlmResponse complete(LlmRequest request) {
        last = request;
        if (failure != null) {
            throw failure;
        }
        return new LlmResponse(answer, "test-model", 10, 5, new BigDecimal("0.00001"), "gen-1", "{}");
    }

    @Override
    public String model() {
        return "test-model";
    }
}
