package me.iofdev.leadhunter.place;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import me.iofdev.leadhunter.place.WebsiteCrawler.CrawlResult;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Stage 2 evidence: one row per website crawl attempt, plus the scraped reviews. See ADR 0027. */
@Repository
public class CrawlRepository {

    private final JdbcClient jdbc;

    public CrawlRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void saveCrawl(long placeId, CrawlResult crawl) {
        jdbc.sql("""
                        insert into website_crawl (place_id, url, reachable, https, mobile_friendly, stale,
                                                   content_hash, http_status, error)
                        values (:placeId, :url, :reachable, :https, :mobileFriendly, :stale,
                                :contentHash, :httpStatus, :error)
                        """)
                .param("placeId", placeId)
                .param("url", crawl.url())
                .param("reachable", crawl.reachable())
                .param("https", crawl.https())
                .param("mobileFriendly", crawl.mobileFriendly())
                .param("stale", crawl.stale())
                .param("contentHash", crawl.contentHash())
                .param("httpStatus", crawl.httpStatus())
                .param("error", crawl.error())
                .update();
    }

    /**
     * The newest crawl of the lead's place's current website, or empty when that website was never crawled.
     * A crawl of an earlier website of the same place is not an audit of this one.
     */
    public Optional<StoredCrawl> latestForLead(long leadId) {
        return jdbc.sql("""
                        select w.url, w.reachable, w.https, w.mobile_friendly, w.stale, w.http_status, w.error, w.crawled_at
                        from website_crawl w
                        join place p on p.id = w.place_id
                        join lead l on l.place_id = p.id
                        where l.id = :leadId
                          and w.url = p.website
                        order by w.crawled_at desc, w.id desc
                        limit 1
                        """)
                .param("leadId", leadId)
                .query((rs, row) -> new StoredCrawl(
                        rs.getString("url"),
                        rs.getBoolean("reachable"),
                        (Boolean) rs.getObject("https"),
                        (Boolean) rs.getObject("mobile_friendly"),
                        (Boolean) rs.getObject("stale"),
                        (Integer) rs.getObject("http_status"),
                        rs.getString("error"),
                        rs.getObject("crawled_at", OffsetDateTime.class)))
                .optional();
    }

    /** A crawl as stored: the facts the website rules read, with when they were taken. */
    public record StoredCrawl(String url, boolean reachable, Boolean https, Boolean mobileFriendly, Boolean stale,
                              Integer httpStatus, String error, OffsetDateTime crawledAt) {
    }

    /** Replaces the stored reviews for a place, so a re-fetch never duplicates them. */
    public void saveReviews(long placeId, List<PlaceReview> reviews) {
        jdbc.sql("delete from place_review where place_id = :placeId").param("placeId", placeId).update();
        for (PlaceReview review : reviews) {
            jdbc.sql("insert into place_review (place_id, star, text, published_at) values (:placeId, :star, :text, :publishedAt)")
                    .param("placeId", placeId)
                    .param("star", review.star())
                    .param("text", review.text())
                    .param("publishedAt", review.publishedAt())
                    .update();
        }
    }

    public List<PlaceReview> reviewsForPlace(long placeId) {
        return jdbc.sql("select star, text, published_at from place_review where place_id = :placeId order by id")
                .param("placeId", placeId)
                .query((rs, row) -> new PlaceReview(
                        rs.getObject("star") == null ? null : rs.getInt("star"),
                        rs.getString("text"),
                        rs.getString("published_at")))
                .list();
    }
}
