package me.iofdev.leadhunter.usage;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param monthlyBudgetUsd what one month of Apify and LLM spend should stay under. ADR 0006 sets it to $10.
 */
@ConfigurationProperties("leadhunter.usage")
public record UsageProperties(@DefaultValue("10") BigDecimal monthlyBudgetUsd) {
}
