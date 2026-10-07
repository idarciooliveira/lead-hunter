package me.iofdev.leadhunter.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;

import me.iofdev.leadhunter.auth.OrgId;
import me.iofdev.leadhunter.campaign.CampaignRepository;
import me.iofdev.leadhunter.usage.UsageFilter;
import me.iofdev.leadhunter.pipeline.BudgetService;
import me.iofdev.leadhunter.usage.UsageReport;
import me.iofdev.leadhunter.usage.UsageRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Spend reporting as `usage` prints it, as JSON. Same filters: `month` and `campaign`. */
@RestController
@RequestMapping("/api/usage")
class UsageController {

    private final UsageRepository usage;
    private final CampaignRepository campaigns;
    private final BudgetService budget;

    UsageController(UsageRepository usage, CampaignRepository campaigns, BudgetService budget) {
        this.usage = usage;
        this.campaigns = campaigns;
        this.budget = budget;
    }

    public record Summary(
            String scope,
            UsageReport.Apify apify,
            UsageReport.Llm llm,
            List<UsageReport.CampaignSpend> byCampaign,
            BigDecimal llmWithoutCampaignUsd,
            BigDecimal totalUsd,
            BigDecimal budgetUsd) {
    }

    /** Totals for the filter. When `entries` is true, the newest runs and calls instead. */
    @GetMapping
    Summary summary(
            OrgId org,
            @RequestParam(required = false) String month,
            @RequestParam(required = false, name = "campaign") String campaignSlug) {
        UsageFilter filter = filter(org, month, campaignSlug);
        UsageReport report = usage.report(filter);
        return new Summary(
                scope(month, campaignSlug),
                report.apify(),
                report.llm(),
                report.byCampaign(),
                report.llmWithoutCampaignUsd(),
                report.totalUsd(),
                budget.budgetFor(org));
    }

    @GetMapping("/entries")
    List<UsageReport.Entry> entries(
            OrgId org,
            @RequestParam(required = false) String month,
            @RequestParam(required = false, name = "campaign") String campaignSlug,
            @RequestParam(defaultValue = "30") int limit) {
        return usage.entries(filter(org, month, campaignSlug), Math.clamp(limit, 1, 200));
    }

    private UsageFilter filter(OrgId org, String month, String campaignSlug) {
        YearMonth selected = month == null ? null : parseMonth(month);
        YearMonth next = selected == null ? null : selected.plusMonths(1);
        Long campaignId = campaignSlug == null ? null : campaigns.findBySlug(org, campaignSlug)
                .orElseThrow(() -> new IllegalArgumentException("no campaign '" + campaignSlug + "'. Run: campaign list"))
                .id();
        return new UsageFilter(org, start(selected), start(next), campaignId);
    }

    private static String scope(String month, String campaignSlug) {
        String when = month == null ? "all time" : month;
        return campaignSlug == null ? when : when + ", campaign " + campaignSlug;
    }

    private static YearMonth parseMonth(String value) {
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("--month must look like 2026-09, got '" + value + "'");
        }
    }

    private static OffsetDateTime start(YearMonth month) {
        return month == null ? null : month.atDay(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
    }
}
