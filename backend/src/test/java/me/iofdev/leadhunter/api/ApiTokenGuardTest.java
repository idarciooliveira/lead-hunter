package me.iofdev.leadhunter.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ApiTokenGuardTest {

    @Test
    void refusesToStartWithoutAToken() {
        assertThatThrownBy(() -> new ApiTokenGuard(new ApiProperties("")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LEADHUNTER_API_TOKEN");
        assertThatThrownBy(() -> new ApiTokenGuard(new ApiProperties("  "))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void startsWithAToken() {
        assertThatCode(() -> new ApiTokenGuard(new ApiProperties("secret"))).doesNotThrowAnyException();
    }

    @Test
    void theTokenDefaultsToEmpty() {
        assertThat(new ApiProperties("").token()).isEmpty();
    }
}
