package me.iofdev.leadhunter.place;

import java.util.Optional;

/**
 * A phone number normalized to E.164. Angolan mobile numbers start with 9 after the
 * 244 country code, and those are the ones most likely to be on WhatsApp.
 */
public record PhoneNumber(String raw, String e164, boolean angolan, boolean mobile) {

    private static final String ANGOLA_CODE = "244";

    public static Optional<PhoneNumber> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.startsWith("00")) {
            digits = digits.substring(2);
        }
        String national = null;
        if (digits.length() == 9 && (digits.startsWith("9") || digits.startsWith("2"))) {
            national = digits;
        } else if (digits.length() == 12 && digits.startsWith(ANGOLA_CODE)) {
            national = digits.substring(3);
        }
        if (national != null) {
            return Optional.of(new PhoneNumber(raw, "+" + ANGOLA_CODE + national, true, national.startsWith("9")));
        }
        if (digits.length() >= 8 && digits.length() <= 15) {
            return Optional.of(new PhoneNumber(raw, "+" + digits, false, false));
        }
        return Optional.empty();
    }

    public String whatsappLink() {
        return "https://wa.me/" + e164.substring(1);
    }
}
