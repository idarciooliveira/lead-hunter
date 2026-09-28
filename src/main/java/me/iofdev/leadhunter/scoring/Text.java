package me.iofdev.leadhunter.scoring;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Accent- and case-insensitive text matching, so "Clínica" matches "clinica". */
public final class Text {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    private Text() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        return DIACRITICS.matcher(decomposed).replaceAll("").toLowerCase(Locale.ROOT).trim();
    }

    /** True when {@code keyword} appears in {@code text} as whole words. */
    public static boolean containsWord(String text, String keyword) {
        String needle = normalize(keyword);
        if (needle.isEmpty()) {
            return false;
        }
        return Pattern.compile("(^|[^\\p{L}\\p{N}])" + Pattern.quote(needle) + "($|[^\\p{L}\\p{N}])")
                .matcher(normalize(text))
                .find();
    }
}
