package me.iofdev.leadhunter.maps;

public class ScrapeException extends RuntimeException {

    private final String externalRunId;

    public ScrapeException(String message, String externalRunId) {
        super(message);
        this.externalRunId = externalRunId;
    }

    public ScrapeException(String message, Throwable cause) {
        super(message, cause);
        this.externalRunId = null;
    }

    /** The scraper's own run id when the run started before failing, so it can be looked up later. */
    public String externalRunId() {
        return externalRunId;
    }
}
