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
                    plain(String.valueOf(lead.id())),
                    text(lead.campaignSlug()),
                    text(lead.name()),
                    text(orEmpty(lead.category())),
                    plain(orEmpty(lead.phoneE164())),
                    plain(orEmpty(lead.whatsappLink())),
                    plain(String.valueOf(lead.score())),
                    plain(lead.status().name()),
                    text(orEmpty(lead.pitch())),
                    text(lead.breakdown().stream().map(ScoreItem::reason).collect(Collectors.joining("; ")))));
        }
        return csv.toString();
    }

    private static void row(StringBuilder csv, List<String> fields) {
        csv.append(String.join(",", fields)).append("\r\n");
    }

    /** A value that is already safe: a number, or a phone number, which must keep its leading plus. */
    private static String plain(String value) {
        return escape(value);
    }

    /**
     * Free text from Maps or the LLM. A value starting with =, +, -, @, a tab or a carriage return would
     * run as a formula in Excel, so it gets a leading apostrophe.
     */
    private static String text(String value) {
        boolean formula = !value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0;
        return escape(formula ? "'" + value : value);
    }

    private static String escape(String value) {
        boolean quote = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        return quote ? "\"" + value.replace("\"", "\"\"") + "\"" : value;
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
