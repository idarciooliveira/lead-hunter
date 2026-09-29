package me.iofdev.leadhunter.maps;

import java.math.BigDecimal;

public class ScrapeException extends RuntimeException {

    private final String externalRunId;
    private final BigDecimal costUsd;

    public ScrapeException(String message, String externalRunId) {
        this(message, externalRunId, null);
    }

    public ScrapeException(String message, String externalRunId, BigDecimal costUsd) {
        super(message);
        this.externalRunId = externalRunId;
        this.costUsd = costUsd;
    }

    public ScrapeException(String message, Throwable cause) {
        super(message, cause);
        this.externalRunId = null;
        this.costUsd = null;
    }

    /** The scraper's own run id when the run started before failing, so it can be looked up later. */
    public String externalRunId() {
        return externalRunId;
    }

    /** What the failed run cost, as reported by the scraper's own API. Null when unknown. Failed runs are billed too. */
    public BigDecimal costUsd() {
        return costUsd;
    }
}
