package me.iofdev.leadhunter.scoring;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import me.iofdev.leadhunter.place.WebsiteCrawler.CrawlResult;

/**
 * Stage 2 rules over the website crawl and the LLM's review complaint classes (ADR 0007 weights,
 * ADR 0027). Every point carries its evidence; the LLM never sets the score.
 */
public final class Stage2Scorer {

    static final int WEBSITE_RULE_POINTS = 25;

    static final int COMPLAINT_RULE_POINTS = 20;

    /** Complaint kinds the review classifier may return and the scorer understands. */
    public static final Set<String> COMPLAINT_KINDS = Set.of("contact", "booking", "waiting");

    private Stage2Scorer() {
    }

    /**
     * Scores the crawl: at most one website rule fires (+25 total), since broken, unreachable,
     * not mobile-friendly and missing HTTPS mostly travel together on the same site.
     */
    public static Score score(CrawlResult crawl, Set<String> complaintKinds) {
        return score(Optional.of(crawl), complaintKinds);
    }

    /**
     * Scores a lead with no crawlable site (no URL, or a social page stage 1 already judged):
     * only the review complaints apply, so a missing website never double-counts as broken.
     */
    public static Score score(Optional<CrawlResult> crawl, Set<String> complaintKinds) {
        List<ScoreItem> items = new ArrayList<>();
        crawl.flatMap(Stage2Scorer::websiteRule).ifPresent(items::add);
        boolean notMobile = crawl.filter(CrawlResult::reachable)
                .map(result -> Boolean.FALSE.equals(result.mobileFriendly()))
                .orElse(false);
        if (notMobile) {
            items.add(new ScoreItem("NOT_MOBILE_FRIENDLY", WEBSITE_RULE_POINTS,
                    "Website has no viewport or responsive layout"));
        }
        complaintRule(complaintKinds).ifPresent(items::add);
        return Score.of(items);
    }

    private static Optional<ScoreItem> websiteRule(CrawlResult crawl) {
        if (!crawl.reachable()) {
            return Optional.of(new ScoreItem("WEBSITE_BROKEN", WEBSITE_RULE_POINTS,
                    "Website unreachable" + (crawl.error() == null ? "" : ": " + crawl.error())));
        }
        if (crawl.https() == Boolean.FALSE) {
            return Optional.of(new ScoreItem("NO_HTTPS", WEBSITE_RULE_POINTS, "Website is plain HTTP, not HTTPS"));
        }
        if (crawl.stale() == Boolean.TRUE) {
            return Optional.of(new ScoreItem("WEBSITE_STALE", WEBSITE_RULE_POINTS,
                    "Website copyright is at least two years old"));
        }
        return Optional.empty();
    }

    private static Optional<ScoreItem> complaintRule(Set<String> complaintKinds) {
        List<String> known = complaintKinds.stream().filter(COMPLAINT_KINDS::contains).sorted().toList();
        if (known.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ScoreItem("REVIEW_COMPLAINTS", COMPLAINT_RULE_POINTS,
                "Reviews complain about " + String.join(", ", known)));
    }
}
