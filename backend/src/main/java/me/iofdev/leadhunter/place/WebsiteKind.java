package me.iofdev.leadhunter.place;

import java.net.URI;
import java.util.List;
import java.util.Locale;

public enum WebsiteKind {
    /** No website listed on Google Maps. */
    NONE,
    /** The listed website is a social profile, link-in-bio page, or a retired Google business.site page. */
    SOCIAL_ONLY,
    /** A website on the business's own domain. */
    OWN;

    private static final List<String> SOCIAL_HOSTS = List.of(
            "facebook.com", "fb.com", "fb.me", "instagram.com", "linktr.ee", "wa.me", "whatsapp.com",
            "tiktok.com", "twitter.com", "x.com", "linkedin.com", "youtube.com", "beacons.ai", "bio.link",
            "taplink.cc", "business.site");

    public static WebsiteKind classify(String url) {
        if (url == null || url.isBlank()) {
            return NONE;
        }
        String host = host(url.trim());
        if (host == null) {
            return OWN;
        }
        for (String social : SOCIAL_HOSTS) {
            if (host.equals(social) || host.endsWith("." + social)) {
                return SOCIAL_ONLY;
            }
        }
        return OWN;
    }

    private static String host(String url) {
        String withScheme = url.contains("://") ? url : "http://" + url;
        try {
            String host = URI.create(withScheme).getHost();
            return host == null ? null : host.toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
