package me.iofdev.leadhunter.place;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import me.iofdev.leadhunter.place.WebsiteCrawler.CrawlResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Crawl behaviour against a local fixture server, never real sites (ADR 0016).
 */
class WebsiteCrawlerTest {

    private HttpServer server;
    private WebsiteCrawler crawler;

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void startServer(String body) throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        AtomicReference<String> lastPath = new AtomicReference<>();
        server.createContext("/", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            lastPath.set(exchange.getRequestURI().getPath());
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (var out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
        crawler = new WebsiteCrawler(HttpClient.newHttpClient());
    }

    @Test
    void reachableModernSiteYieldsNoPenalties() throws Exception {
        startServer("""
                <html><head><meta name="viewport" content="width=device-width"></head>
                <body><footer>Copyright 2026</footer></body></html>
                """);

        var result = crawler.crawl("http://localhost:" + server.getAddress().getPort());

        assertThat(result.reachable()).isTrue();
        assertThat(result.https()).isFalse();
        assertThat(result.mobileFriendly()).isTrue();
        assertThat(result.stale()).isFalse();
        assertThat(result.contentHash()).isNotBlank();
        assertThat(result.error()).isNull();
    }

    @Test
    void viewportSignalUsesAnyCopyrightYear() throws Exception {
        startServer("<html><meta name=\"viewport\"><body>ok</body></html>");

        var result = crawler.crawl("localhost:" + server.getAddress().getPort());

        assertThat(result.reachable()).isTrue();
        assertThat(result.mobileFriendly()).isTrue();
        assertThat(result.stale()).isFalse();
    }

    @Test
    void staleSiteWithoutViewportFlagsBothSignals() throws Exception {
        startServer("""
                <html><body><p>(c) Copyright 2018 Empresa Lda</p></body></html>
                """);

        var result = crawler.crawl("http://localhost:" + server.getAddress().getPort());

        assertThat(result.reachable()).isTrue();
        assertThat(result.mobileFriendly()).isFalse();
        assertThat(result.stale()).isTrue();
    }

    @Test
    void unreachableHostBecomesAResultNotAnException() {
        CrawlResult result = crawler().crawl("http://127.0.0.1:1/");

        assertThat(result.reachable()).isFalse();
        assertThat(result.error()).isNotBlank();
        assertThat(result.https()).isNull();
        assertThat(result.contentHash()).isNull();
    }

    @Test
    void garbageUrlIsUnreachable() {
        CrawlResult result = crawler().crawl("http:///just-a-path");

        assertThat(result.reachable()).isFalse();
        assertThat(result.error()).contains("no host");
    }

    private WebsiteCrawler crawler() {
        return new WebsiteCrawler(HttpClient.newHttpClient());
    }
}
