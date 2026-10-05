package me.iofdev.leadhunter.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

class LeadFoxTest {

    @Test
    void plainModeHasNoEscapeCodes() {
        StringWriter sink = new StringWriter();

        LeadFox.print(new PrintWriter(sink, true), false);

        assertThat(sink.toString()).doesNotContain("\u001B").containsPattern("[.:=+*#%@]");
    }

    @Test
    void colorModeUsesTruecolorHalfBlocks() {
        StringWriter sink = new StringWriter();

        LeadFox.print(new PrintWriter(sink, true), true);

        assertThat(sink.toString()).contains("\u001B[38;2;").contains("\u001B[48;2;")
                .contains("▀").contains("LEAD HUNTER").contains("\u001B[0m");
    }

    @Test
    void backgroundIsTransparent() {
        assertThat(LeadFox.artwork()[0][0]).isNull();
    }

    @Test
    void artRowsShareTheSameWidthAndUseTheHigherResolution() {
        StringWriter sink = new StringWriter();

        LeadFox.print(new PrintWriter(sink, true), false);

        String[] lines = sink.toString().split("\n", -1);
        // The 38-pixel sprite is sampled in pairs for 19 terminal rows, followed by the wordmark.
        assertThat(lines).hasSize(22);
        for (int i = 0; i < 19; i++) {
            assertThat(lines[i]).hasSize(42);
        }
        assertThat(sink.toString()).contains("LEAD HUNTER").contains("LOCAL LEADS. SMARTER OUTREACH.");
    }
}
