package me.iofdev.leadhunter.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextTest {

    @Test
    void ignoresAccentsAndCase() {
        assertThat(Text.normalize("  Clínica DENTÁRIA ")).isEqualTo("clinica dentaria");
    }

    @Test
    void matchesWholeWordsOnly() {
        assertThat(Text.containsWord("Banco BAI | Bank", "banco")).isTrue();
        assertThat(Text.containsWord("Centro Médico Maianga", "centro medico")).isTrue();
        assertThat(Text.containsWord("Bancada Móveis", "banco")).isFalse();
        assertThat(Text.containsWord("Kerosene Lda", "kero")).isFalse();
    }
}
