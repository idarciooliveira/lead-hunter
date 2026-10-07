package me.iofdev.leadhunter.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class EnrichmentPropertiesTest {

    @Test
    void refusesAnEstimateThatWouldWeakenTheBudgetCheck() {
        assertThatThrownBy(() -> new EnrichmentProperties(25, 10, BigDecimal.ZERO, new BigDecimal("0.002")))
                .hasMessageContaining("estimated-usd-per-review must be more than 0");
        assertThatThrownBy(() -> new EnrichmentProperties(25, 10, new BigDecimal("0.0005"), new BigDecimal("-1")))
                .hasMessageContaining("estimated-llm-usd-per-lead must be more than 0");
    }

    @Test
    void estimatesAJobAsReviewsPlusLlmCalls() {
        EnrichmentProperties properties = new EnrichmentProperties(25, 10, new BigDecimal("0.0005"), new BigDecimal("0.002"));

        assertThat(properties.estimateJobUsd(4, 10)).isEqualByComparingTo("0.028");
    }
}
