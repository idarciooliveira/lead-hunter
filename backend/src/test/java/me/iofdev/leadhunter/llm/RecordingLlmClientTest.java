package me.iofdev.leadhunter.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class RecordingLlmClientTest {

    private static final LlmResponse RESPONSE =
            new LlmResponse("Olá", "m", 10, 5, new BigDecimal("0.0001"), "gen_1", "{}");

    private final LlmCallRepository calls = mock(LlmCallRepository.class);

    private static LlmClient answering(LlmResponse response) {
        return new LlmClient() {
            @Override
            public LlmResponse complete(LlmRequest request) {
                return response;
            }

            @Override
            public String model() {
                return "m";
            }
        };
    }

    @Test
    void storesTheCallAndReturnsTheAnswer() {
        LlmRequest request = LlmRequest.text(null, "Olá").forCampaign(7L, "pitch");

        LlmResponse response = new RecordingLlmClient(answering(RESPONSE), calls).complete(request);

        assertThat(response).isSameAs(RESPONSE);
        verify(calls).save(request, RESPONSE);
    }

    @Test
    void stillReturnsTheAnswerWhenStoringFails() {
        doThrow(new IllegalStateException("db down")).when(calls).save(any(), any());

        LlmResponse response = new RecordingLlmClient(answering(RESPONSE), calls).complete(LlmRequest.text(null, "Olá"));

        assertThat(response).isSameAs(RESPONSE);
    }

    @Test
    void storesNothingWhenTheCallFails() {
        LlmClient failing = new LlmClient() {
            @Override
            public LlmResponse complete(LlmRequest request) {
                throw new LlmException("LLM gateway returned 500");
            }

            @Override
            public String model() {
                return "m";
            }
        };

        assertThatThrownBy(() -> new RecordingLlmClient(failing, calls).complete(LlmRequest.text(null, "Olá")))
                .isInstanceOf(LlmException.class);
        verify(calls, never()).save(any(), any());
    }
}
