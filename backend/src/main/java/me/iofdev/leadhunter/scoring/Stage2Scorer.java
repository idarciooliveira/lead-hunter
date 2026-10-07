package me.iofdev.leadhunter.scoring;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import me.iofdev.leadhunter.place.WebsiteCrawler.CrawlResult;

/**
 * Stage 2 rules over the website crawl and the LLM's review complaint classes (ADR 0007 weights,
 * ADR 0027). Every point carries its evidence; the LLM never sets the score. A clean run appends
 * the zero-point {@link #NO_ISSUES_CODE} marker so the breakdown shows stage 2 ran (ADR 0028).
 */
public final class Stage2Scorer {

    static final int WEBSITE_RULE_POINTS = 25;

    static final int COMPLAINT_RULE_POINTS = 20;

    /** Complaint kinds the review classifier may return and the scorer understands. */
    public static final Set<String> COMPLAINT_KINDS = Set.of("contact", "booking", "waiting");

    /** Zero-point marker proving stage 2 ran and fired no point-earning rule (ADR 0028). */
    public static final String NO_ISSUES_CODE = "STAGE2_NO_ISSUES";

    private static final Set<String> CODES = Set.of("WEBSITE_BROKEN", "NO_HTTPS", "NOT_MOBILE_FRIENDLY",
            "WEBSITE_STALE", "REVIEW_COMPLAINTS", NO_ISSUES_CODE);

    private Stage2Scorer() {
    }

    /** True for the codes this scorer writes, so a stored breakdown can be split into its two stages. */
    public static boolean isStage2Code(String code) {
        return CODES.contains(code);
    }

    /**
     * Scores the crawl: at most one website rule fires (+25 total), since broken, missing HTTPS,
     * not mobile-friendly and stale mostly travel together on the same site.
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
        complaintRule(complaintKinds).ifPresent(items::add);
        if (items.isEmpty()) {
            items.add(crawl.filter(CrawlResult::reachable).isPresent()
                    ? new ScoreItem(NO_ISSUES_CODE, 0,
                            "Website reachable, HTTPS, mobile-friendly; no review complaints")
                    : new ScoreItem(NO_ISSUES_CODE, 0, "No website to crawl; no review complaints"));
        }
        return Score.of(items);
    }

    /** The first website rule that applies, in order: broken, no HTTPS, not mobile-friendly, stale. */
    private static Optional<ScoreItem> websiteRule(CrawlResult crawl) {
        if (!crawl.reachable()) {
            return Optional.of(new ScoreItem("WEBSITE_BROKEN", WEBSITE_RULE_POINTS,
                    "Website unreachable" + (crawl.error() == null ? "" : ": " + crawl.error())));
        }
        if (crawl.https() == Boolean.FALSE) {
            return Optional.of(new ScoreItem("NO_HTTPS", WEBSITE_RULE_POINTS, "Website is plain HTTP, not HTTPS"));
        }
        if (crawl.mobileFriendly() == Boolean.FALSE) {
            return Optional.of(new ScoreItem("NOT_MOBILE_FRIENDLY", WEBSITE_RULE_POINTS,
                    "Website has no viewport or responsive layout"));
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
