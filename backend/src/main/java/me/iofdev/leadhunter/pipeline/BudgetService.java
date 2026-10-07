package me.iofdev.leadhunter.pipeline;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;

import me.iofdev.leadhunter.auth.OrgId;
import me.iofdev.leadhunter.usage.Money;
import me.iofdev.leadhunter.usage.UsageProperties;
import me.iofdev.leadhunter.usage.UsageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Refuses a job that would take an organization past its monthly budget, or all organizations past the install
 * cap (ADR 0044). It runs when a job starts and never stops one that is already running. Months are calendar
 * months in UTC, like the usage API.
 */
@Service
public class BudgetService {

    private final UsageRepository usage;
    private final UsageProperties properties;
    private final Clock clock;

    @Autowired
    public BudgetService(UsageRepository usage, UsageProperties properties) {
        this(usage, properties, Clock.systemUTC());
    }

    BudgetService(UsageRepository usage, UsageProperties properties, Clock clock) {
        this.usage = usage;
        this.properties = properties;
        this.clock = clock;
    }

    /** The organization's own budget, or the default when the operator set none. */
    public BigDecimal budgetFor(OrgId orgId) {
        return usage.orgBudget(orgId).orElse(properties.monthlyBudgetUsd());
    }

    /** The cap on all organizations together, or null when there is none. */
    public BigDecimal installCap() {
        return properties.totalMonthlyBudgetUsd();
    }

    /**
     * Throws {@link BudgetExceededException} when this month's spend plus {@code estimateUsd} passes the
     * organization's budget or the install cap. The message names the limit and both amounts.
     */
    public void check(OrgId orgId, BigDecimal estimateUsd) {
        // One month for both limits, so a check that runs across midnight UTC never mixes two months.
        YearMonth month = thisMonth();
        BigDecimal budget = budgetFor(orgId);
        BigDecimal spent = committed(orgId, month);
        if (spent.add(estimateUsd).compareTo(budget) > 0) {
            throw refusal("the monthly budget of this organization", budget, spent, estimateUsd);
        }
        BigDecimal cap = installCap();
        if (cap != null) {
            BigDecimal installSpent = committed(null, month);
            if (installSpent.add(estimateUsd).compareTo(cap) > 0) {
                throw refusal("the monthly cap on all organizations together", cap, installSpent, estimateUsd);
            }
        }
    }

    /**
     * What the check counts for this UTC month: spent, plus what running jobs and unpriced runs reserve. An
     * {@code orgId} of null means every organization. The usage views show it next to the reported spend, so
     * a refusal makes sense when little has been reported yet.
     */
    public BigDecimal committedThisMonth(OrgId orgId) {
        return committed(orgId, thisMonth());
    }

    private YearMonth thisMonth() {
        return YearMonth.now(clock.withZone(ZoneOffset.UTC));
    }

    private BigDecimal committed(OrgId orgId, YearMonth month) {
        return usage.committed(orgId, start(month), start(month.plusMonths(1)));
    }

    private static OffsetDateTime start(YearMonth month) {
        return month.atDay(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
    }

    private static BudgetExceededException refusal(String limitName, BigDecimal limit, BigDecimal spent,
                                                   BigDecimal estimate) {
        return new BudgetExceededException("this run would pass " + limitName + " of " + Money.usd(limit)
                + ". This month has " + Money.usd(spent) + " spent or reserved, and this run is estimated at "
                + Money.usd(estimate));
    }
}
