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
        YearMonth month = YearMonth.now(clock.withZone(ZoneOffset.UTC));
        OffsetDateTime from = month.atDay(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        OffsetDateTime to = month.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

        BigDecimal budget = budgetFor(orgId);
        BigDecimal spent = usage.committed(orgId, from, to);
        if (spent.add(estimateUsd).compareTo(budget) > 0) {
            throw refusal("the monthly budget of this organization", budget, spent, estimateUsd);
        }
        BigDecimal cap = installCap();
        if (cap != null) {
            BigDecimal installSpent = usage.committed(null, from, to);
            if (installSpent.add(estimateUsd).compareTo(cap) > 0) {
                throw refusal("the monthly cap on all organizations together", cap, installSpent, estimateUsd);
            }
        }
    }

    private static BudgetExceededException refusal(String limitName, BigDecimal limit, BigDecimal spent,
                                                   BigDecimal estimate) {
        return new BudgetExceededException("this run would pass " + limitName + " of " + Money.usd(limit)
                + ". This month has " + Money.usd(spent) + " spent or reserved, and this run is estimated at "
                + Money.usd(estimate));
    }
}
