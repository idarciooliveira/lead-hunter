package me.iofdev.leadhunter.cli;

import java.io.PrintWriter;

public final class Format {

    private Format() {
    }

    static void printWarnings(PrintWriter out, Iterable<String> warnings) {
        for (String warning : warnings) {
            out.println("warning: " + warning);
        }
    }

    static String truncate(String value, int width) {
        if (value == null) {
            return "-";
        }
        return value.length() <= width ? value : value.substring(0, width - 1) + "…";
    }

    static String orDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    /** A text bar of {@code width} cells. The fill is clamped to 0..1. */
    static String bar(double fraction, int width) {
        int filled = (int) Math.round(Math.max(0, Math.min(1, fraction)) * width);
        return "█".repeat(filled) + "░".repeat(width - filled);
    }

    /** 131_500 becomes "131.5k". */
    static String tokens(long count) {
        if (count < 1_000) {
            return Long.toString(count);
        }
        if (count < 1_000_000) {
            return String.format("%.1fk", count / 1_000.0);
        }
        return String.format("%.2fM", count / 1_000_000.0);
    }
}
