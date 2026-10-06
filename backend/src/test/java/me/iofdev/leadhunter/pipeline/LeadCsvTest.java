package me.iofdev.leadhunter.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import me.iofdev.leadhunter.place.WebsiteKind;
import me.iofdev.leadhunter.scoring.ScoreItem;
import org.junit.jupiter.api.Test;

class LeadCsvTest {

    @Test
    void writesBomHeaderAndOneRowPerLead() {
        String csv = LeadCsv.of(List.of(lead("Clínica Sorriso", "Olá, tudo bem?", List.of())));

        assertThat(csv).startsWith("﻿id,campanha,nome,categoria,telefone,whatsapp,pontuacao,estado,pitch,motivos\r\n");
        assertThat(csv).contains("7,clinicas,Clínica Sorriso,Clínica,+244923456789,https://wa.me/244923456789,65,NEW,"
                + "\"Olá, tudo bem?\",\r\n");
    }

    @Test
    void quotesCommasQuotesAndLineBreaksAndJoinsReasons() {
        List<ScoreItem> reasons = List.of(new ScoreItem("A", 20, "sem site"), new ScoreItem("B", 10, "poucas avaliações"));
        String csv = LeadCsv.of(List.of(lead("Casa \"Boa\", Lda", "linha 1\nlinha 2", reasons)));

        assertThat(csv).contains("\"Casa \"\"Boa\"\", Lda\"");
        assertThat(csv).contains("\"linha 1\nlinha 2\"");
        assertThat(csv).contains("sem site; poucas avaliações\r\n");
    }

    @Test
    void emptyFieldsStayEmptyAndFormulasAreDefused() {
        LeadView lead = new LeadView(1, "c", LeadStage.QUALIFIED, LeadStatus.NEW, null, null, 1, List.of(), null,
                "=HYPERLINK(\"x\")", null, null, null, null, false, null, WebsiteKind.NONE, null, 0, null, List.of(), null);
        String csv = LeadCsv.of(List.of(lead));

        assertThat(csv).contains("1,c,\"'=HYPERLINK(\"\"x\"\")\",,,,1,NEW,,\r\n");
    }

    private static LeadView lead(String name, String pitch, List<ScoreItem> reasons) {
        return new LeadView(7, "clinicas", LeadStage.QUALIFIED, LeadStatus.NEW, null, null, 65, reasons, null,
                name, "Clínica", "Rua 1", "Talatona", "+244923456789", true, null, WebsiteKind.NONE,
                new BigDecimal("4.3"), 142, "https://maps.example/p1", List.of(), pitch);
    }
}
