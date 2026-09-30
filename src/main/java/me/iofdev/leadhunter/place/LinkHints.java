package me.iofdev.leadhunter.place;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Finds hrefs around contact and about link text in a crawled homepage. */
final class LinkHints {

    /** href up to the closing quote, within 400 chars of the hint text (label or URL slug). */
    private static final Pattern LINK = Pattern.compile("href\\s*=\\s*[\"']([^\"']{1,300})[\"']", Pattern.CASE_INSENSITIVE);
    private static final int WINDOW = 400;

    private LinkHints() {
    }

    static Optional<String> findHref(String lowerBody, String hint) {
        int at = lowerBody.indexOf(hint);
        while (at >= 0) {
            int from = Math.max(0, at - WINDOW);
            int to = Math.min(lowerBody.length(), at + WINDOW);
            Matcher matcher = LINK.matcher(lowerBody.substring(from, to));
            if (matcher.find()) {
                String href = matcher.group(1);
                if (!href.startsWith("javascript") && !href.startsWith("#")) {
                    return Optional.of(href);
                }
            }
            at = lowerBody.indexOf(hint, at + hint.length());
        }
        return Optional.empty();
    }
}
