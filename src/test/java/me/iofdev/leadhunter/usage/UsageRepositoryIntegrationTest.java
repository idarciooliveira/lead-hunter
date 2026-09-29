package me.iofdev.leadhunter.usage;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

import me.iofdev.leadhunter.PostgresTestSupport;
import me.iofdev.leadhunter.llm.LlmCallRepository;
import me.iofdev.leadhunter.llm.LlmRequest;
import me.iofdev.leadhunter.llm.LlmResponse;
import me.iofdev.leadhunter.maps.ScrapeRequest;
import me.iofdev.leadhunter.pipeline.RunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;

@EnabledIf("me.iofdev.leadhunter.PostgresTestSupport#databaseAvailable")
class UsageRepositoryIntegrationTest extends PostgresTestSupport {

    @Autowired
    UsageRepository usage;
    @Autowired
    RunRepository runs;
    @Autowired
    LlmCallRepository llmCalls;

    private long clinics;
    private long schools;

    @BeforeEach
    void seed() {
        clinics = campaign("clinicas");
        schools = campaign("escolas");

        succeed(run(clinics), 40, "0.50");
        succeed(run(clinics), 20, "0.25");
        runs.fail(run(schools), "run-x", "boom", new BigDecimal("0.03"));
        // A crash before Apify answered leaves a failed run with no cost.
        runs.fail(run(schools), null, "connection reset", null);

        call(clinics, "pitch", "google/gemma-4-26b-a4b-it", 1000, 200, "0.00200000");
        call(clinics, "pitch", "anthropic/claude-haiku-4.5", 500, 100, "0.00100000");
        call(schools, "review-analysis", "google/gemma-4-26b-a4b-it", 2000, 300, null);
        call(null, "test", "google/gemma-4-26b-a4b-it", 15, 10, "0.00000771");
    }

    @Test
    void sumsApifySpendIncludingFailedRuns() {
        UsageReport.Apify apify = usage.report(UsageFilter.all()).apify();

        assertThat(apify.runs()).isEqualTo(4);
        assertThat(apify.failedRuns()).isEqualTo(2);
        assertThat(apify.unpricedRuns()).isEqualTo(1);
        assertThat(apify.places()).isEqualTo(60);
        assertThat(apify.costUsd()).isEqualByComparingTo("0.78");
    }

    @Test
    void sumsLlmSpendPerModelAndCountsCallsWithoutACost() {
        UsageReport.Llm llm = usage.report(UsageFilter.all()).llm();

        assertThat(llm.calls()).isEqualTo(4);
        assertThat(llm.unpricedCalls()).isEqualTo(1);
        assertThat(llm.promptTokens()).isEqualTo(3515);
        assertThat(llm.completionTokens()).isEqualTo(610);
        assertThat(llm.costUsd()).isEqualByComparingTo("0.00300771");
        assertThat(llm.models()).extracting(UsageReport.ModelSpend::model)
                .containsExactly("google/gemma-4-26b-a4b-it", "anthropic/claude-haiku-4.5");
        assertThat(llm.models().get(0).calls()).isEqualTo(3);
        assertThat(llm.models().get(0).costUsd()).isEqualByComparingTo("0.00200771");
    }

    @Test
    void splitsSpendByCampaignAndKeepsTheRestApart() {
        UsageReport report = usage.report(UsageFilter.all());

        assertThat(report.totalUsd()).isEqualByComparingTo("0.78300771");
        assertThat(report.byCampaign()).extracting(UsageReport.CampaignSpend::slug).containsExactly("clinicas", "escolas");
        assertThat(report.byCampaign().get(0).apifyUsd()).isEqualByComparingTo("0.75");
        assertThat(report.byCampaign().get(0).llmUsd()).isEqualByComparingTo("0.003");
        assertThat(report.byCampaign().get(1).apifyUsd()).isEqualByComparingTo("0.03");
        assertThat(report.llmWithoutCampaignUsd()).isEqualByComparingTo("0.00000771");
    }

    @Test
    void filtersByCampaign() {
        UsageReport report = usage.report(new UsageFilter(null, null, clinics));

        assertThat(report.apify().runs()).isEqualTo(2);
        assertThat(report.apify().costUsd()).isEqualByComparingTo("0.75");
        assertThat(report.llm().calls()).isEqualTo(2);
        assertThat(report.byCampaign()).extracting(UsageReport.CampaignSpend::slug).containsExactly("clinicas");
        assertThat(report.llmWithoutCampaignUsd()).isZero();
    }

    @Test
    void filtersByTimeWindowWithAnExclusiveEnd() {
        OffsetDateTime august = OffsetDateTime.of(2026, 8, 15, 12, 0, 0, 0, ZoneOffset.UTC);
        jdbc.sql("update campaign_run set started_at = :at where campaign_id = :id")
                .param("at", august).param("id", schools).update();
        jdbc.sql("update llm_call set created_at = :at where campaign_id = :id")
                .param("at", august).param("id", schools).update();
        OffsetDateTime september = OffsetDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime october = OffsetDateTime.of(2026, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);

        UsageReport inAugust = usage.report(new UsageFilter(august.withDayOfMonth(1).withHour(0), september, null));
        assertThat(inAugust.apify().runs()).isEqualTo(2);
        assertThat(inAugust.apify().costUsd()).isEqualByComparingTo("0.03");
        assertThat(inAugust.llm().calls()).isEqualTo(1);

        UsageReport inSeptember = usage.report(new UsageFilter(september, october, null));
        assertThat(inSeptember.apify().runs()).isEqualTo(2);
        assertThat(inSeptember.apify().costUsd()).isEqualByComparingTo("0.75");
    }

    @Test
    void listsEntriesNewestFirstWithUnknownCostsAsNull() {
        List<UsageReport.Entry> entries = usage.entries(UsageFilter.all(), 100);

        assertThat(entries).hasSize(8);
        assertThat(entries).extracting(UsageReport.Entry::at).isSortedAccordingTo(Comparator.reverseOrder());
        assertThat(entries).filteredOn(e -> e.costUsd() == null).hasSize(2);
        assertThat(usage.entries(UsageFilter.all(), 3)).hasSize(3);
        assertThat(usage.entries(new UsageFilter(null, null, schools), 100))
                .extracting(UsageReport.Entry::kind).containsOnly("apify", "llm");
    }

    private long campaign(String slug) {
        return jdbc.sql("""
                        insert into campaign (slug, name, answers, search)
                        values (:slug, :slug, '{}'::jsonb, '{}'::jsonb) returning id
                        """)
                .param("slug", slug).query(Long.class).single();
    }

    private long run(long campaignId) {
        return runs.start(campaignId, new ScrapeRequest(List.of("clínica"), "Luanda", 40, "pt-PT"));
    }

    private void succeed(long runId, int places, String cost) {
        runs.succeed(runId, "ext-" + runId, "ds-" + runId, places, new BigDecimal(cost));
    }

    private void call(Long campaignId, String purpose, String model, int prompt, int completion, String cost) {
        LlmRequest request = LlmRequest.text(null, "x").forCampaign(campaignId, purpose);
        llmCalls.save(request, new LlmResponse("ok", model, prompt, completion,
                cost == null ? null : new BigDecimal(cost), "gen", "{\"cost\": 1}"));
    }
}
