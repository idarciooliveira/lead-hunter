package me.iofdev.leadhunter.maps;

import java.math.BigDecimal;
import java.util.List;

public record ScrapeResult(String externalRunId, String datasetId, BigDecimal costUsd, List<ScrapedPlace> places) {
}
