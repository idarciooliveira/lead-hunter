package me.iofdev.leadhunter.pipeline;

import java.util.List;
import java.util.stream.Collectors;

import me.iofdev.leadhunter.scoring.ScoreItem;

/**
 * Leads as CSV (ADR 0041). Quotes every field that needs it, ends rows with CRLF and starts with a
 * UTF-8 byte order mark, so Excel reads the accents.
 */
public final class LeadCsv {

    public static final String BOM = "﻿";

    private static final List<String> HEADER = List.of(
            "id", "campanha", "nome", "categoria", "telefone", "whatsapp", "pontuacao", "estado", "pitch", "motivos");

    private LeadCsv() {
    }

    public static String of(List<LeadView> leads) {
        StringBuilder csv = new StringBuilder(BOM);
        row(csv, HEADER);
        for (LeadView lead : leads) {
            row(csv, List.of(
                    String.valueOf(lead.id()),
                    lead.campaignSlug(),
                    lead.name(),
                    orEmpty(lead.category()),
                    orEmpty(lead.phoneE164()),
                    orEmpty(lead.whatsappLink()),
                    String.valueOf(lead.score()),
                    lead.status().name(),
                    orEmpty(lead.pitch()),
                    lead.breakdown().stream().map(ScoreItem::reason).collect(Collectors.joining("; "))));
        }
        return csv.toString();
    }

    private static void row(StringBuilder csv, List<String> fields) {
        csv.append(fields.stream().map(LeadCsv::field).collect(Collectors.joining(","))).append("\r\n");
    }

    /** A name starting with = or @ would run as a formula in Excel, so it gets a leading apostrophe. */
    private static String field(String value) {
        String safe = value.startsWith("=") || value.startsWith("@") ? "'" + value : value;
        boolean quote = safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r");
        return quote ? "\"" + safe.replace("\"", "\"\"") + "\"" : safe;
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
