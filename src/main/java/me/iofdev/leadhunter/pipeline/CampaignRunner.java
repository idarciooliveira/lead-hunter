package me.iofdev.leadhunter.pipeline;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import me.iofdev.leadhunter.apify.ApifyProperties;
import me.iofdev.leadhunter.campaign.Campaign;
import me.iofdev.leadhunter.campaign.CampaignFile;
import me.iofdev.leadhunter.company.CompanyProfile;
import me.iofdev.leadhunter.company.CompanyRepository;
import me.iofdev.leadhunter.maps.GoogleMapsScraper;
import me.iofdev.leadhunter.maps.ScrapeException;
import me.iofdev.leadhunter.maps.ScrapeRequest;
import me.iofdev.leadhunter.maps.ScrapeResult;
import me.iofdev.leadhunter.maps.ScrapedPlace;
import me.iofdev.leadhunter.place.PhoneNumber;
import me.iofdev.leadhunter.place.PlaceRepository;
import me.iofdev.leadhunter.place.WebsiteKind;
import me.iofdev.leadhunter.scoring.Exclusions;
import me.iofdev.leadhunter.scoring.Score;
import me.iofdev.leadhunter.scoring.Stage1Scorer;
import org.springframework.stereotype.Service;

/** Stage 1 of the pipeline in ADR 0006: discover, filter, score, cut. */
@Service
public class CampaignRunner {

    private final GoogleMapsScraper scraper;
    private final PlaceRepository places;
    private final LeadRepository leads;
    private final RunRepository runs;
    private final ApifyProperties apify;
    private final CompanyRepository company;

    public CampaignRunner(GoogleMapsScraper scraper, PlaceRepository places, LeadRepository leads,
                          RunRepository runs, ApifyProperties apify, CompanyRepository company) {
        this.scraper = scraper;
        this.places = places;
        this.leads = leads;
        this.runs = runs;
        this.apify = apify;
        this.company = company;
    }

    public SearchPlan plan(Campaign campaign) {
        CampaignFile.Search search = campaign.search();
        List<ScrapeRequest> requests = search.locations().stream()
                .map(location -> new ScrapeRequest(search.terms(), location, search.maxPlacesPerSearch(), search.language()))
                .toList();
        int maxPlaces = requests.stream().mapToInt(ScrapeRequest::maxPlaces).sum();
        return new SearchPlan(requests, maxPlaces, apify.estimatedUsdPerPlace().multiply(BigDecimal.valueOf(maxPlaces)));
    }

    public RunSummary run(Campaign campaign, boolean allowOverLimit, Consumer<String> progress) {
        SearchPlan plan = plan(campaign);
        if (plan.maxPlaces() > apify.maxPlacesPerRun() && !allowOverLimit) {
            throw new BudgetExceededException("this run could return up to " + plan.maxPlaces()
                    + " places, above the limit of " + apify.maxPlacesPerRun()
                    + ". Lower search.maxPlacesPerSearch, split the campaign, or pass --allow-over-limit");
        }
        scraper.checkReady();
        List<CompanyProfile.Client> clients = company.find().map(CompanyProfile::clients).orElse(List.of());

        int failed = 0;
        int found = 0;
        int created = 0;
        int excluded = 0;
        BigDecimal cost = BigDecimal.ZERO;

        for (ScrapeRequest request : plan.requests()) {
            progress.accept("Searching " + request.terms().size() + " terms in " + request.location() + "...");
            long runId = runs.start(campaign.id(), request);
            ScrapeResult result;
            try {
                result = scraper.search(request);
            } catch (ScrapeException e) {
                runs.fail(runId, e.externalRunId(), e.getMessage());
                progress.accept("  failed: " + e.getMessage());
                failed++;
                continue;
            } catch (RuntimeException e) {
                runs.fail(runId, null, e.toString());
                progress.accept("  failed: " + e.getMessage());
                failed++;
                continue;
            }
            runs.succeed(runId, result.externalRunId(), result.datasetId(), result.places().size(), result.costUsd());
            cost = cost.add(result.costUsd());
            found += result.places().size();

            for (ScrapedPlace place : result.places()) {
                Optional<PhoneNumber> phone = PhoneNumber.parse(place.phone());
                WebsiteKind website = WebsiteKind.classify(place.website());
                long placeId = places.upsert(place, phone, website);

                Optional<String> exclusion = Exclusions.check(place, phone, website, campaign.search(), clients);
                boolean isNew;
                if (exclusion.isPresent()) {
                    isNew = leads.saveStage1(campaign.id(), placeId, runId, LeadStage.EXCLUDED, Score.of(List.of()),
                            exclusion.get());
                    excluded++;
                } else {
                    Score score = Stage1Scorer.score(place, phone, website, campaign.search().targetKeywords());
                    isNew = leads.saveStage1(campaign.id(), placeId, runId, LeadStage.BELOW_CUT, score, null);
                }
                if (isNew) {
                    created++;
                }
            }
            progress.accept("  " + result.places().size() + " places, $" + result.costUsd());
        }

        leads.applyStage1Cut(campaign.id(), campaign.search().qualifyShare());
        return new RunSummary(plan.requests().size(), failed, found, created, excluded,
                leads.countByStage(campaign.id()), cost);
    }
}
