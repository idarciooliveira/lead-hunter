package me.iofdev.leadhunter.cli;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Format {

    private Format() {
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

    /** Dollars with 4 decimals. An amount that would round to zero shows as "<$0.0001" so small LLM costs stay visible. */
    public static String usd(BigDecimal amount) {
        if (amount == null) {
            return "unknown";
        }
        BigDecimal rounded = amount.setScale(4, RoundingMode.HALF_UP);
        if (rounded.signum() == 0 && amount.signum() > 0) {
            return "<$0.0001";
        }
        return "$" + rounded.toPlainString();
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
