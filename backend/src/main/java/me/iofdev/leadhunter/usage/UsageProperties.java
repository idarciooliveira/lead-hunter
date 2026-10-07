package me.iofdev.leadhunter.usage;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param monthlyBudgetUsd      what one organization's month of Apify and LLM spend may reach unless the operator
 *                              sets its own budget. ADR 0006 sets it to $10, ADR 0044 makes it a limit.
 * @param totalMonthlyBudgetUsd the same limit for all organizations together; null means no install cap
 */
@ConfigurationProperties("leadhunter.usage")
public record UsageProperties(@DefaultValue("10") BigDecimal monthlyBudgetUsd, BigDecimal totalMonthlyBudgetUsd) {
}
