package me.iofdev.leadhunter.place;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import me.iofdev.leadhunter.PostgresTestSupport;
import me.iofdev.leadhunter.place.WebsiteCrawler.CrawlResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;

@EnabledIf("me.iofdev.leadhunter.PostgresTestSupport#databaseAvailable")
class CrawlRepositoryIntegrationTest extends PostgresTestSupport {

    @Autowired
    CrawlRepository crawls;

    private long placeId;

    @BeforeEach
    void seed() {
        placeId = jdbc.sql("""
                        insert into place (google_place_id, name, website, website_kind, raw)
                        values ('q1', 'Clínica Girassol', 'https://girassol.ao', 'OWN', '{}'::jsonb)
                        returning id
                        """)
                .query(Long.class).single();
    }

    @Test
    void savesAndReadsCrawlsAndReviews() {
        crawls.saveCrawl(placeId, new CrawlResult("https://girassol.ao", true, true, false, false,
                "hash-1", 200, null));
        crawls.saveReviews(placeId, List.of(
                new PlaceReview(2, "Ninguém atende o telefone", "2026-08-01"),
                new PlaceReview(5, null, null)));

        assertThat(jdbc.sql("select reachable, https, mobile_friendly, stale, content_hash, http_status, error from website_crawl")
                .query((rs, row) -> rs.getBoolean(1) + ":" + rs.getBoolean(2) + ":" + rs.getBoolean(3) + ":"
                        + rs.getBoolean(4) + ":" + rs.getString(5) + ":" + rs.getInt(6) + ":" + rs.getString(7))
                .single()).isEqualTo("true:true:false:false:hash-1:200:null");
        assertThat(crawls.reviewsForPlace(placeId)).containsExactly(
                new PlaceReview(2, "Ninguém atende o telefone", "2026-08-01"),
                new PlaceReview(5, null, null));
    }

    @Test
    void savingReviewsAgainReplacesThem() {
        crawls.saveReviews(placeId, List.of(new PlaceReview(1, "Muita espera", null)));
        crawls.saveReviews(placeId, List.of());

        assertThat(crawls.reviewsForPlace(placeId)).isEmpty();
    }

    @Test
    void unreachableCrawlKeepsNullSignals() {
        crawls.saveCrawl(placeId, CrawlResult.unreachable("https://girassol.ao", null, "connection refused"));

        assertThat(jdbc.sql("select reachable, https, error from website_crawl")
                .query((rs, row) -> rs.getBoolean(1) + ":" + rs.getString(2) + ":" + rs.getString(3))
                .single()).isEqualTo("false:null:connection refused");
    }
}
