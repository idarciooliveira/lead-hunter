package me.iofdev.leadhunter.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class FormatTest {

    @Test
    void roundsCostsToFourDecimals() {
        assertThat(Format.usd(new BigDecimal("0.043699999999999996"))).isEqualTo("$0.0437");
    }

    @Test
    void keepsCleanValuesUnchanged() {
        assertThat(Format.usd(new BigDecimal("0.0452"))).isEqualTo("$0.0452");
        assertThat(Format.usd(new BigDecimal("12.5"))).isEqualTo("$12.5000");
        assertThat(Format.usd(BigDecimal.ZERO)).isEqualTo("$0.0000");
    }

    @Test
    void showsPositiveDustAsBelowOneThenthOfACent() {
        assertThat(Format.usd(new BigDecimal("0.00001"))).isEqualTo("<$0.0001");
        assertThat(Format.usd(new BigDecimal("0.0000499999999999"))).isEqualTo("<$0.0001");
    }

    @Test
    void reportsUnknownWhenThereIsNoCost() {
        assertThat(Format.usd(null)).isEqualTo("unknown");
    }
}
