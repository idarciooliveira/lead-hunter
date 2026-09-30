package me.iofdev.leadhunter.place;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Stage 2 crawl of a place's own website, plain Java HTTP with no browser (ADR 0027). Fetches the
 * homepage and one hop to a contact or about page, capped at 512 KB per body and 20 seconds per lead.
 */
public final class WebsiteCrawler {

    static final int MAX_BODY_BYTES = 512 * 1024;
    static final Duration PAGE_TIMEOUT = Duration.ofSeconds(8);
    static final Duration LEAD_TIMEOUT = Duration.ofSeconds(20);

    static final List<String> SECOND_PAGE_HINTS = List.of("contacto", "contato", "contact", "sobre", "about", "quemsomos");

    private final HttpClient http;
    private final long deadlineBudgetMillis;

    public WebsiteCrawler(HttpClient http) {
        this(http, LEAD_TIMEOUT.toMillis());
    }

    WebsiteCrawler(HttpClient http, long deadlineBudgetMillis) {
        this.http = http;
        this.deadlineBudgetMillis = deadlineBudgetMillis;
    }

    /**
     * Crawls the site. Every failure mode — unreachable host, timeout, non-HTML — becomes a result
     * with reachable=false and the error stored, never an exception to the caller.
     */
    public CrawlResult crawl(String websiteUrl) {
        long start = System.currentTimeMillis();
        URI homepage;
        try {
            homepage = uri(websiteUrl);
        } catch (IllegalArgumentException e) {
            return CrawlResult.unreachable(websiteUrl, null, "unparseable URL: " + e.getMessage());
        }

        PageFetch home = fetch(homepage, start);
        if (!home.ok()) {
            return CrawlResult.unreachable(websiteUrl, home.status(), home.error());
        }

        boolean https = "https".equalsIgnoreCase(homepage.getScheme());
        String body = home.body();
        Optional<String> secondUrl = secondPageUrl(homepage, body);
        if (secondUrl.isPresent() && remainingMillis(start) > 0) {
            PageFetch second = fetch(uriOrNull(secondUrl.get()), start);
            if (second.ok() && second.body() != null) {
                body = body + "\n" + second.body();
            }
        }

        return new CrawlResult(websiteUrl, true, https, WebsiteSignals.mobileFriendly(body), WebsiteSignals.stale(body),
                sha256(body), home.status(), null);
    }

    private URI uri(String url) {
        String withScheme = url.trim().contains("://") ? url.trim() : "http://" + url.trim();
        URI uri = URI.create(withScheme);
        if (uri.getHost() == null) {
            throw new IllegalArgumentException("no host in " + url);
        }
        return uri;
    }

    private URI uriOrNull(String url) {
        try {
            return uri(url);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Optional<String> secondPageUrl(URI homepage, String body) {
        String lower = body.toLowerCase(Locale.ROOT);
        for (String hint : SECOND_PAGE_HINTS) {
            Optional<String> href = LinkHints.findHref(lower, hint);
            if (href.isPresent()) {
                return href.map(link -> absolutize(homepage, link));
            }
        }
        return Optional.empty();
    }

    private String absolutize(URI homepage, String href) {
        try {
            return homepage.resolve(href).toString();
        } catch (IllegalArgumentException e) {
            return href;
        }
    }

    private PageFetch fetch(URI uri, long start) {
        if (uri == null || remainingMillis(start) <= 0) {
            return PageFetch.failed(null, "out of time budget");
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(PAGE_TIMEOUT)
                    .header("User-Agent", "LeadHunter/0.1 (+internal lead tool)")
                    .header("Accept", "text/html,application/xhtml+xml")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 400) {
                return PageFetch.failed(response.statusCode(), "HTTP " + response.statusCode());
            }
            String contentType = response.headers().firstValue("content-type").orElse("");
            if (!contentType.isBlank() && !contentType.toLowerCase(Locale.ROOT).contains("html")
                    && !contentType.startsWith("text/")) {
                return PageFetch.failed(response.statusCode(), "not HTML: " + contentType);
            }
            byte[] bytes = response.body();
            if (bytes.length > MAX_BODY_BYTES) {
                bytes = java.util.Arrays.copyOf(bytes, MAX_BODY_BYTES);
            }
            return PageFetch.ok(response.statusCode(), new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
        } catch (java.net.http.HttpTimeoutException e) {
            return PageFetch.failed(null, "timeout");
        } catch (Exception e) {
            return PageFetch.failed(null, rootMessage(e));
        }
    }

    private long remainingMillis(long start) {
        return deadlineBudgetMillis - (System.currentTimeMillis() - start);
    }

    private static String rootMessage(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null ? cause.toString() : cause.getMessage();
    }

    private record PageFetch(boolean ok, Integer status, String body, String error) {

        static PageFetch ok(int status, String body) {
            return new PageFetch(true, status, body, null);
        }

        static PageFetch failed(Integer status, String error) {
            return new PageFetch(false, status, null, error);
        }
    }

    static String sha256(String body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(body.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("no SHA-256", e);
        }
    }

    public record CrawlResult(String url, boolean reachable, Boolean https, Boolean mobileFriendly, Boolean stale,
                              String contentHash, Integer httpStatus, String error) {

        public static CrawlResult unreachable(String url, Integer status, String error) {
            return new CrawlResult(url, false, null, null, null, null, status, error);
        }
    }
}
