package me.iofdev.leadhunter.pipeline;

import java.math.BigDecimal;

import me.iofdev.leadhunter.maps.ScrapeException;

public record RunFailure(String externalRunId, String error, BigDecimal costUsd) {

    public static RunFailure of(RuntimeException e) {
        if (e instanceof ScrapeException scrape) {
            return new RunFailure(scrape.externalRunId(), scrape.getMessage(), scrape.costUsd());
        }
        return new RunFailure(null, e.toString(), null);
    }
}
