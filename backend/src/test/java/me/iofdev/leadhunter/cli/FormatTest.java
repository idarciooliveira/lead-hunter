package me.iofdev.leadhunter.cli;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FormatTest {

    @Test
    void truncatesLongValuesWithAnEllipsis() {
        assertThat(Format.truncate("a very long campaign slug here", 10)).isEqualTo("a very lo…");
        assertThat(Format.truncate(null, 10)).isEqualTo("-");
    }

    @Test
    void orDash() {
        assertThat(Format.orDash(null)).isEqualTo("-");
        assertThat(Format.orDash("  ")).isEqualTo("-");
        assertThat(Format.orDash("x")).isEqualTo("x");
    }

    @Test
    void bar() {
        assertThat(Format.bar(0.0, 4)).isEqualTo("░░░░");
        assertThat(Format.bar(0.5, 4)).isEqualTo("██░░");
        assertThat(Format.bar(1.0, 4)).isEqualTo("████");
        assertThat(Format.bar(2.0, 4)).isEqualTo("████");
        assertThat(Format.bar(-1.0, 4)).isEqualTo("░░░░");
    }

    @Test
    void tokens() {
        assertThat(Format.tokens(999)).isEqualTo("999");
        assertThat(Format.tokens(131_500)).isEqualTo("131.5k");
        assertThat(Format.tokens(2_500_000)).isEqualTo("2.50M");
    }
}
