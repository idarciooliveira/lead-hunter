package me.iofdev.leadhunter.cli;

final class Format {

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
}
