package me.iofdev.leadhunter.place;

import java.util.Locale;

/**
 * Heuristics over the crawled HTML for the ADR 0007 website rules (broken, not mobile-friendly, stale).
 * No JavaScript rendering: signals are what plain HTML shows.
 */
final class WebsiteSignals {

    private WebsiteSignals() {
    }

    /** True when the site declares a viewport or uses responsive media queries / framework grids. */
    static boolean mobileFriendly(String html) {
        String lower = html.toLowerCase(Locale.ROOT);
        return lower.contains("name=\"viewport\"")
                || lower.contains("name='viewport'")
                || lower.contains("@media")
                || lower.contains("bootstrap")
                || lower.contains("tailwind");
    }

    /** True when the newest copyright year found on the page is at least two years behind. */
    static boolean stale(String html) {
        String lower = html.toLowerCase(Locale.ROOT);
        int newest = 0;
        int index = lower.indexOf("copyright");
        while (index >= 0 && newest < currentYear() - 1) {
            newest = Math.max(newest, yearAfter(lower, index));
            index = lower.indexOf("copyright", index + 9);
        }
        if (newest == 0) {
            index = lower.indexOf("&copy;");
            while (index >= 0 && newest < currentYear() - 1) {
                newest = Math.max(newest, yearAfter(lower, index));
                index = lower.indexOf("&copy;", index + 6);
            }
        }
        return newest > 0 && newest <= currentYear() - 2;
    }

    private static int yearAfter(String lower, int from) {
        int end = Math.min(lower.length(), from + 200);
        for (int at = from; at < end - 4; at++) {
            if (Character.isDigit(lower.charAt(at))) {
                String candidate = lower.substring(at, Math.min(at + 4, end));
                if (candidate.chars().allMatch(Character::isDigit)) {
                    int year = Integer.parseInt(candidate);
                    if (year >= 1990 && year <= 2100) {
                        return year;
                    }
                }
            }
        }
        return 0;
    }

    private static int currentYear() {
        return java.time.Year.now().getValue();
    }
}
