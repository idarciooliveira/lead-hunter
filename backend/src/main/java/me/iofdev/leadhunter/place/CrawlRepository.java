package me.iofdev.leadhunter.place;

import java.util.List;

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
