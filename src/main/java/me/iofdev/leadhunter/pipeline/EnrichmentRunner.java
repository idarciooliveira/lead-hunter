package me.iofdev.leadhunter.pipeline;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import me.iofdev.leadhunter.campaign.Campaign;
import me.iofdev.leadhunter.llm.ReviewComplaints;
import me.iofdev.leadhunter.maps.ReviewFetcher;
import me.iofdev.leadhunter.maps.ReviewFetcher.ReviewsResult;
import me.iofdev.leadhunter.maps.ScrapeException;
import me.iofdev.leadhunter.place.CrawlRepository;
import me.iofdev.leadhunter.place.PlaceReview;
import me.iofdev.leadhunter.place.WebsiteCrawler;
import me.iofdev.leadhunter.place.WebsiteCrawler.CrawlResult;
import me.iofdev.leadhunter.place.WebsiteKind;
import me.iofdev.leadhunter.cli.Format;
import me.iofdev.leadhunter.scoring.Score;
import me.iofdev.leadhunter.scoring.ScoreItem;
import me.iofdev.leadhunter.scoring.Stage2Scorer;
import org.springframework.stereotype.Service;

/** Stage 2 of the pipeline in ADR 0006: crawl, fetch reviews, classify complaints, rescore. See ADR 0027. */
@Service
public class EnrichmentRunner {

    private final ReviewFetcher reviews;
    private final ReviewComplaints complaints;
    private final CrawlRepository crawls;
    private final LeadRepository leads;
    private final RunRepository runs;
    private final WebsiteCrawler crawler;

    public EnrichmentRunner(ReviewFetcher reviews, ReviewComplaints complaints, CrawlRepository crawls,
                            LeadRepository leads, RunRepository runs) {
        this.reviews = reviews;
        this.complaints = complaints;
        this.crawls = crawls;
        this.leads = leads;
        this.runs = runs;
        this.crawler = new WebsiteCrawler(HttpClient.newHttpClient());
    }

    public EnrichmentSummary enrich(Campaign campaign, int batchSize, int maxReviews, Consumer<String> progress) {
        List<EnrichmentTarget> targets = leads.unenrichedQualified(campaign.id(), batchSize);
        fetchReviews(campaign, targets, maxReviews, progress);

        int enriched = 0;
        for (EnrichmentTarget target : targets) {
            Optional<CrawlResult> crawl = crawlSite(target, progress);
            Set<String> kinds = complaints.classify(campaign.id(), crawls.reviewsForPlace(target.placeId()));
            Score stage2 = Stage2Scorer.score(crawl, kinds);
            List<ScoreItem> all = new ArrayList<>(target.stage1Breakdown());
            all.addAll(stage2.items());
            Score combined = Score.of(all);
            leads.saveStage2(target.leadId(), combined, kinds);
            enriched++;
            String added = stage2.items().stream()
                    .map(item -> "+" + item.points() + " " + item.code())
                    .collect(Collectors.joining(", "));
            progress.accept("  " + target.name() + ": " + target.stage1Score() + " -> " + combined.total()
                    + (added.isEmpty() ? " (no change)" : " (" + added + ")"));
        }
        return new EnrichmentSummary(targets.size(), enriched, leads.countByStage(campaign.id()));
    }

    private Optional<CrawlResult> crawlSite(EnrichmentTarget target, Consumer<String> progress) {
        if (target.website() == null || target.website().isBlank() || target.websiteKind() != WebsiteKind.OWN) {
            return Optional.empty();
        }
        CrawlResult crawl = crawler.crawl(target.website());
        crawls.saveCrawl(target.placeId(), crawl);
        if (!crawl.reachable()) {
            progress.accept("  " + target.name() + ": website unreachable"
                    + (crawl.error() == null ? "" : " (" + crawl.error() + ")"));
        }
        return Optional.of(crawl);
    }

    /**
     * One Apify run over the batch's Maps URLs. A missing token skips reviews with a note; a failed
     * run still records its cost and the batch keeps its crawl scores.
     */
    private void fetchReviews(Campaign campaign, List<EnrichmentTarget> targets, int maxReviews,
                              Consumer<String> progress) {
        Map<String, Long> placeIdsByUrl = new LinkedHashMap<>();
        for (EnrichmentTarget target : targets) {
            if (target.mapsUrl() != null && !target.mapsUrl().isBlank()) {
                placeIdsByUrl.putIfAbsent(target.mapsUrl(), target.placeId());
            }
        }
        if (placeIdsByUrl.isEmpty()) {
            return;
        }
        List<String> urls = List.copyOf(placeIdsByUrl.keySet());
        try {
            reviews.checkReady();
        } catch (IllegalStateException e) {
            progress.accept("Skipping reviews: " + e.getMessage());
            return;
        }
        progress.accept("Fetching up to " + maxReviews + " reviews for " + urls.size() + " places...");
        long runId = runs.startReviews(campaign.id(), urls);
        ReviewsResult result;
        try {
            result = reviews.fetchReviews(urls, maxReviews, campaign.search().language());
        } catch (ScrapeException e) {
            runs.fail(runId, e.externalRunId(), e.getMessage(), e.costUsd());
            progress.accept("  reviews failed: " + e.getMessage());
            return;
        } catch (RuntimeException e) {
            runs.fail(runId, null, e.toString(), null);
            progress.accept("  reviews failed: " + e.getMessage());
            return;
        }
        runs.succeed(runId, result.externalRunId(), result.datasetId(), urls.size(), result.costUsd());
        for (Map.Entry<String, Long> entry : placeIdsByUrl.entrySet()) {
            crawls.saveReviews(entry.getValue(), result.reviewsByUrl().getOrDefault(entry.getKey(), List.of()));
        }
        progress.accept("  reviews for " + result.reviewsByUrl().size() + " of " + urls.size()
                + " places, " + Format.usd(result.costUsd()));
    }

    /** Exposes the stored reviews of one place, so the CLI can show what the classifier read. */
    public List<PlaceReview> reviewsForPlace(long placeId) {
        return crawls.reviewsForPlace(placeId);
    }
}
