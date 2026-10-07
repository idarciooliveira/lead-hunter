package me.iofdev.leadhunter.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import me.iofdev.leadhunter.PostgresTestSupport;
import me.iofdev.leadhunter.auth.OrgId;
import me.iofdev.leadhunter.maps.ScrapeResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** The budget check of ADR 0044: $10 for one organization, $15 for all of them together. */
@EnabledIf("me.iofdev.leadhunter.PostgresTestSupport#databaseAvailable")
@SpringBootTest(properties = {"leadhunter.cli.enabled=false", "leadhunter.usage.monthly-budget-usd=10",
        "leadhunter.usage.total-monthly-budget-usd=15"})
class BudgetServiceIntegrationTest extends PostgresTestSupport {

    private static final OrgId OTHER = new OrgId("other-org");

    @Autowired
    BudgetService budget;
    @Autowired
    RunRepository runs;

    private long campaign;
    private long otherCampaign;

    @BeforeEach
    void seed() {
        jdbc.sql("insert into organization (id, name, slug) values ('other-org', 'Other', 'other')").update();
        campaign = campaign(ORG, "clinicas");
        otherCampaign = campaign(OTHER, "escolas");
    }

    @Test
    void refusesARunThatWouldPassTheBudgetAndNamesTheLimitAndBothAmounts() {
        succeed(campaign, "9.50", null);

        assertThatThrownBy(() -> budget.check(ORG, usd("1")))
                .isInstanceOf(BudgetExceededException.class)
                .hasMessageContaining("monthly budget of this organization of $10.0000")
                .hasMessageContaining("$9.5000")
                .hasMessageContaining("$1.0000");
        assertThatCode(() -> budget.check(ORG, usd("0.50"))).doesNotThrowAnyException();
    }

    @Test
    void aRunWithoutACostCountsAtItsEstimate() {
        runs.fail(runs.start(campaign, "Luanda", List.of("clínica"), 40, null, RunRepository.KIND_SCRAPE, usd("4")),
                new RunFailure(null, "connection reset", null));

        assertThatThrownBy(() -> budget.check(ORG, usd("7"))).isInstanceOf(BudgetExceededException.class);
        assertThatCode(() -> budget.check(ORG, usd("5.50"))).doesNotThrowAnyException();
    }

    @Test
    void aRunCountsAtItsRealCostOnceItHasOne() {
        succeed(campaign, "0.50", usd("4"));

        assertThatCode(() -> budget.check(ORG, usd("9"))).doesNotThrowAnyException();
    }

    @Test
    void deletingACampaignKeepsItsSpend() {
        succeed(campaign, "9.50", null);

        jdbc.sql("delete from campaign where id = :id").param("id", campaign).update();

        assertThat(jdbc.sql("select count(*) from campaign_run where campaign_id is null").query(Integer.class).single())
                .isEqualTo(1);
        assertThatThrownBy(() -> budget.check(ORG, usd("1"))).isInstanceOf(BudgetExceededException.class);
    }

    @Test
    void aRunningJobCountsAtItsEstimateUntilItEnds() {
        JobLease job = runs.startJob(campaign, RunRepository.KIND_SCRAPE, null, usd("8"));
        assertThatThrownBy(() -> budget.check(ORG, usd("3"))).isInstanceOf(BudgetExceededException.class);

        long child = runs.start(campaign, "Luanda", List.of("clínica"), 40, job.jobId(), RunRepository.KIND_SCRAPE, usd("8"));
        runs.succeed(child, new ScrapeResult("ext", "ds", usd("2"), List.of()), 40);
        // The estimate is now covered by the child's real cost: $2 spent, $8 reserved minus $2.
        assertThatThrownBy(() -> budget.check(ORG, usd("3"))).isInstanceOf(BudgetExceededException.class);
        assertThatCode(() -> budget.check(ORG, usd("1"))).doesNotThrowAnyException();

        runs.finishJob(job.jobId(), 40);
        job.close();
        assertThatCode(() -> budget.check(ORG, usd("7"))).doesNotThrowAnyException();
    }

    @Test
    void aJobThatStartedLastMonthStillReservesItsEstimate() {
        try (JobLease job = runs.startJob(campaign, RunRepository.KIND_SCRAPE, null, usd("8"))) {
            jdbc.sql("update campaign_run set started_at = date_trunc('month', now()) - interval '1 hour' where id = :id")
                    .param("id", job.jobId()).update();

            assertThatThrownBy(() -> budget.check(ORG, usd("3"))).isInstanceOf(BudgetExceededException.class);
            assertThat(budget.committedThisMonth(ORG)).isEqualByComparingTo("8");
        }
    }

    @Test
    void twoJobsThatStartAtOnceAreAdmittedOneAfterTheOther() throws Exception {
        CountDownLatch firstAdmitted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<JobLease> first = pool.submit(() -> runs.startJob(campaign, RunRepository.KIND_SCRAPE, null, usd("6"),
                    () -> {
                        budget.check(ORG, usd("6"));
                        firstAdmitted.countDown();
                        await(releaseFirst);
                    }));
            assertThat(firstAdmitted.await(10, TimeUnit.SECONDS)).isTrue();
            Future<JobLease> second = pool.submit(() -> runs.startJob(otherCampaignOfOrg(), RunRepository.KIND_SCRAPE,
                    null, usd("6"), () -> budget.check(ORG, usd("6"))));

            // The second start waits on the admission lock while the first has not written its row.
            assertThatThrownBy(() -> second.get(500, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            releaseFirst.countDown();

            try (JobLease admitted = first.get(10, TimeUnit.SECONDS)) {
                assertThatThrownBy(() -> second.get(10, TimeUnit.SECONDS))
                        .hasCauseInstanceOf(BudgetExceededException.class);
            }
        } finally {
            releaseFirst.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void theOrganizationsOwnBudgetReplacesTheDefault() {
        jdbc.sql("update organization set monthly_budget_usd = 2 where id = :id").param("id", ORG.value()).update();

        assertThatThrownBy(() -> budget.check(ORG, usd("3"))).hasMessageContaining("$2.0000");
        assertThatCode(() -> budget.check(OTHER, usd("3"))).doesNotThrowAnyException();
    }

    @Test
    void anotherOrganizationIsNotBlocked() {
        succeed(campaign, "9.50", null);

        assertThatCode(() -> budget.check(OTHER, usd("1"))).doesNotThrowAnyException();
    }

    @Test
    void theInstallCapBlocksEveryOrganization() {
        succeed(campaign, "9.50", null);
        succeed(otherCampaign, "4.50", null);

        assertThatCode(() -> budget.check(OTHER, usd("1"))).doesNotThrowAnyException();
        assertThatThrownBy(() -> budget.check(OTHER, usd("1.50")))
                .isInstanceOf(BudgetExceededException.class)
                .hasMessageContaining("cap on all organizations together of $15.0000");
    }

    private long otherCampaignOfOrg() {
        return campaign(ORG, "restaurantes");
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private long campaign(OrgId org, String slug) {
        return jdbc.sql("""
                        insert into campaign (org_id, slug, name, answers, search)
                        values (:org, :slug, :slug, '{}'::jsonb, '{}'::jsonb) returning id
                        """)
                .param("org", org.value()).param("slug", slug).query(Long.class).single();
    }

    private void succeed(long campaignId, String cost, BigDecimal estimate) {
        long run = runs.start(campaignId, "Luanda", List.of("clínica"), 40, null, RunRepository.KIND_SCRAPE, estimate);
        runs.succeed(run, new ScrapeResult("ext-" + run, "ds-" + run, usd(cost), List.of()), 40);
    }

    private static BigDecimal usd(String amount) {
        return new BigDecimal(amount);
    }
}
