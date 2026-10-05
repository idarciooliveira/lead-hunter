package me.iofdev.leadhunter.cli;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import me.iofdev.leadhunter.campaign.CampaignRepository;
import me.iofdev.leadhunter.usage.Money;
import me.iofdev.leadhunter.usage.UsageFilter;
import me.iofdev.leadhunter.usage.UsageProperties;
import me.iofdev.leadhunter.usage.UsageReport;
import me.iofdev.leadhunter.usage.UsageRepository;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(
        name = "usage",
        description = "Show what Apify and the LLM have cost.",
        mixinStandardHelpOptions = true)
class UsageCommand implements Runnable {

    private static final int BAR_WIDTH = 30;
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Spec
    CommandSpec spec;

    @Option(names = "--month", paramLabel = "YYYY-MM", description = "Only this month. The budget bar uses it too.")
    String month;

    @Option(names = "--campaign", paramLabel = "SLUG", description = "Only this campaign.")
    String campaignSlug;

    @Option(names = "--runs", description = "List each Apify run and LLM call, newest first.")
    boolean runs;

    @Option(names = "--limit", defaultValue = "30", description = "Rows for --runs. Default: ${DEFAULT-VALUE}.")
    int limit;

    private final UsageRepository usage;
    private final CampaignRepository campaigns;
    private final UsageProperties properties;

    UsageCommand(UsageRepository usage, CampaignRepository campaigns, UsageProperties properties) {
        this.usage = usage;
        this.campaigns = campaigns;
        this.properties = properties;
    }

    @Override
    public void run() {
        PrintWriter out = spec.commandLine().getOut();
        YearMonth selected = month == null ? null : parseMonth(month);
        Long campaignId = campaignSlug == null ? null : CampaignCommand.requireCampaign(campaigns, campaignSlug).id();
        UsageFilter filter = new UsageFilter(start(selected), start(selected == null ? null : selected.plusMonths(1)),
                campaignId);

        out.println("Usage  ·  " + scope(selected));
        out.println();
        if (runs) {
            printEntries(out, usage.entries(filter, limit));
            return;
        }
        UsageReport report = usage.report(filter);
        printApify(out, report.apify());
        printLlm(out, report.llm());
        out.printf("%nTotal  %s%n", Money.usd(report.totalUsd()));
        printBudget(out, selected == null ? YearMonth.now() : selected);
        if (campaignId == null) {
            printCampaigns(out, report);
        }
    }

    private String scope(YearMonth selected) {
        String when = selected == null ? "all time" : selected.toString();
        return campaignSlug == null ? when : when + ", campaign " + campaignSlug;
    }

    private void printApify(PrintWriter out, UsageReport.Apify apify) {
        out.printf("Apify  %s%n", Money.usd(apify.costUsd()));
        out.printf("  Runs          %d (%d failed)%n", apify.runs(), apify.failedRuns());
        out.printf("  Places found  %,d%n", apify.places());
        if (apify.unpricedRuns() > 0) {
            out.printf("  %d %s no cost from Apify, not counted%n", apify.unpricedRuns(),
                    apify.unpricedRuns() == 1 ? "run has" : "runs have");
        }
        out.println();
    }

    private void printLlm(PrintWriter out, UsageReport.Llm llm) {
        out.printf("LLM  %s%n", Money.usd(llm.costUsd()));
        out.printf("  Calls   %d%n", llm.calls());
        out.printf("  Tokens  %s in, %s out%n", Format.tokens(llm.promptTokens()), Format.tokens(llm.completionTokens()));
        for (UsageReport.ModelSpend model : llm.models()) {
            out.printf("  %-30s %s (%d calls)%n", model.model(), Money.usd(model.costUsd()), model.calls());
        }
        if (llm.unpricedCalls() > 0) {
            out.printf("  %d %s no cost from the provider, not counted%n", llm.unpricedCalls(),
                    llm.unpricedCalls() == 1 ? "call has" : "calls have");
        }
    }

    /** The bar always covers all campaigns, because the budget is for the whole account. */
    private void printBudget(PrintWriter out, YearMonth budgetMonth) {
        UsageFilter monthOnly = new UsageFilter(start(budgetMonth), start(budgetMonth.plusMonths(1)), null);
        BigDecimal spent = usage.report(monthOnly).totalUsd();
        BigDecimal budget = properties.monthlyBudgetUsd();
        double fraction = budget.signum() == 0 ? 0 : spent.divide(budget, 4, RoundingMode.HALF_UP).doubleValue();
        out.printf("%nMonth %s, all campaigns  %s%n", budgetMonth, Money.usd(spent));
        out.printf("  %s  %d%% of $%s monthly budget%n", Format.bar(fraction, BAR_WIDTH),
                Math.round(fraction * 100), budget.setScale(2, RoundingMode.HALF_UP).toPlainString());
    }

    private void printCampaigns(PrintWriter out, UsageReport report) {
        if (report.byCampaign().isEmpty() && report.llmWithoutCampaignUsd().signum() == 0) {
            return;
        }
        out.printf("%nBy campaign%n");
        for (UsageReport.CampaignSpend campaign : report.byCampaign()) {
            out.printf("  %-28s Apify %-10s LLM %-10s Total %s%n", Format.truncate(campaign.slug(), 28),
                    Money.usd(campaign.apifyUsd()), Money.usd(campaign.llmUsd()), Money.usd(campaign.totalUsd()));
        }
        if (report.llmWithoutCampaignUsd().signum() > 0) {
            out.printf("  %-28s Apify %-10s LLM %-10s Total %s%n", "(no campaign)", Money.usd(BigDecimal.ZERO),
                    Money.usd(report.llmWithoutCampaignUsd()), Money.usd(report.llmWithoutCampaignUsd()));
        }
    }

    private void printEntries(PrintWriter out, List<UsageReport.Entry> entries) {
        if (entries.isEmpty()) {
            out.println("No runs or calls yet.");
            return;
        }
        out.printf("%-16s %-6s %-22s %-44s %s%n", "WHEN", "KIND", "CAMPAIGN", "WHAT", "COST");
        for (UsageReport.Entry entry : entries) {
            out.printf("%-16s %-6s %-22s %-44s %s%n", WHEN.format(entry.at().atZoneSameInstant(ZoneId.systemDefault())),
                    entry.kind(), Format.truncate(entry.campaignSlug(), 22), Format.truncate(entry.label(), 44),
                    Money.usd(entry.costUsd()));
        }
    }

    private static YearMonth parseMonth(String value) {
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("--month must look like 2026-09, got '" + value + "'");
        }
    }

    private static OffsetDateTime start(YearMonth month) {
        return month == null ? null : month.atDay(1).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
    }
}
