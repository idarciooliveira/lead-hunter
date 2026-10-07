package me.iofdev.leadhunter.usage;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Spend for one {@link UsageFilter}. A run or call with no reported cost adds nothing to the totals and is
 * counted in {@code unpriced} instead, so a gap shows up as a gap and never as a zero.
 */
public record UsageReport(Apify apify, Llm llm, List<CampaignSpend> byCampaign) {

    public BigDecimal totalUsd() {
        return apify.costUsd().add(llm.costUsd());
    }

    public record Apify(int runs, int failedRuns, int unpricedRuns, long places, BigDecimal costUsd) {
    }

    public record Llm(int calls, int unpricedCalls, long promptTokens, long completionTokens, BigDecimal costUsd,
                      List<ModelSpend> models) {
    }

    public record ModelSpend(String model, int calls, BigDecimal costUsd) {
    }

    /** {@code slug} is null for spend whose campaign was deleted or that never had one. */
    public record CampaignSpend(String slug, BigDecimal apifyUsd, BigDecimal llmUsd) {

        public BigDecimal totalUsd() {
            return apifyUsd.add(llmUsd);
        }
    }

    /** One Apify run or LLM call, for {@code usage --runs}. {@code costUsd} is null when unknown. */
    public record Entry(String kind, OffsetDateTime at, String campaignSlug, String label,
                        BigDecimal costUsd) {
    }
}
