package me.iofdev.leadhunter.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import me.iofdev.leadhunter.place.WebsiteCrawler.CrawlResult;
import org.junit.jupiter.api.Test;

class Stage2ScorerTest {

    private static final CrawlResult HEALTHY = new CrawlResult("https://exemplo.ao", true, true, true, false,
            "abc", 200, null);

    private static CrawlResult unreachable() {
        return CrawlResult.unreachable("http://exemplo.ao", null, "connection refused");
    }

    private static List<String> codes(Score score) {
        return score.items().stream().map(ScoreItem::code).toList();
    }

    @Test
    void healthySiteAndNoComplaintsScoreZero() {
        Score score = Stage2Scorer.score(HEALTHY, Set.of());

        assertThat(score.total()).isZero();
        assertThat(score.items()).isEmpty();
    }

    @Test
    void unreachableSiteCostsTwentyFivePointsOnce() {
        Score score = Stage2Scorer.score(unreachable(), Set.of());

        assertThat(codes(score)).containsExactly("WEBSITE_BROKEN");
        assertThat(score.total()).isEqualTo(25);
    }

    @Test
    void noHttpsCostsTwentyFivePointsOnce() {
        CrawlResult plainHttp = new CrawlResult("http://exemplo.ao", true, false, true, false, "abc", 200, null);

        Score score = Stage2Scorer.score(plainHttp, Set.of());

        assertThat(codes(score)).containsExactly("NO_HTTPS");
        assertThat(score.total()).isEqualTo(25);
    }

    @Test
    void staleSiteCostsTwentyFivePointsOnce() {
        CrawlResult stale = new CrawlResult("https://exemplo.ao", true, true, true, true, "abc", 200, null);

        Score score = Stage2Scorer.score(stale, Set.of());

        assertThat(codes(score)).containsExactly("WEBSITE_STALE");
        assertThat(score.total()).isEqualTo(25);
    }

    @Test
    void notMobileFriendlyFiresOnAHealthyHttpsSite() {
        CrawlResult desktopOnly = new CrawlResult("https://exemplo.ao", true, true, false, false, "abc", 200, null);

        Score score = Stage2Scorer.score(desktopOnly, Set.of());

        assertThat(codes(score)).containsExactly("NOT_MOBILE_FRIENDLY");
        assertThat(score.total()).isEqualTo(25);
    }

    @Test
    void brokenAndNotMobileAddUpToWebsiteRuleOnlyOnce() {
        CrawlResult unreachable = unreachable();

        Score score = Stage2Scorer.score(unreachable, Set.of());

        assertThat(codes(score)).containsExactly("WEBSITE_BROKEN");
    }

    @Test
    void complaintKindsAddTwentyPointsWithTheirReason() {
        Score score = Stage2Scorer.score(HEALTHY, Set.of("contact", "waiting", "other"));

        assertThat(codes(score)).containsExactly("REVIEW_COMPLAINTS");
        assertThat(score.items().get(0).reason()).isEqualTo("Reviews complain about contact, waiting");
        assertThat(score.total()).isEqualTo(20);
    }

    @Test
    void unknownComplaintKindsAreIgnored() {
        Score score = Stage2Scorer.score(HEALTHY, Set.of("price", "noise"));

        assertThat(score.total()).isZero();
    }

    @Test
    void leadWithoutACrawlableSiteScoresOnlyComplaints() {
        Score score = Stage2Scorer.score(Optional.empty(), Set.of("booking"));

        assertThat(codes(score)).containsExactly("REVIEW_COMPLAINTS");
        assertThat(score.total()).isEqualTo(20);
    }

    @Test
    void fullStackClampsAtHundred() {
        CrawlResult plainHttp = new CrawlResult("http://exemplo.ao", true, false, false, false, "abc", 200, null);

        Score stage1 = Score.of(List.of(new ScoreItem("NO_WEBSITE_ACTIVE", 30, "x"),
                new ScoreItem("REVIEWS_SWEET_SPOT", 15, "x"), new ScoreItem("TARGET_SECTOR", 10, "x")));
        Score stage2 = Stage2Scorer.score(plainHttp, Set.of("contact", "booking"));

        java.util.List<ScoreItem> all = new java.util.ArrayList<>(stage1.items());
        all.addAll(stage2.items());

        assertThat(Score.of(all).total())
                .isEqualTo(Math.clamp(30 + 15 + 10 + 25 + 20, 0, 100));
    }
}
