package me.iofdev.leadhunter.apify;

import java.math.BigDecimal;
import java.util.Set;

public record ApifyRun(String id, String status, String defaultDatasetId, BigDecimal usageTotalUsd) {

    private static final Set<String> TERMINAL = Set.of("SUCCEEDED", "FAILED", "TIMED-OUT", "ABORTED");

    public boolean finished() {
        return TERMINAL.contains(status);
    }

    public boolean succeeded() {
        return "SUCCEEDED".equals(status);
    }
}
