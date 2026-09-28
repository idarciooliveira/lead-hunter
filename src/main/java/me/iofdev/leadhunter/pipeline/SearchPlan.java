package me.iofdev.leadhunter.pipeline;

import java.math.BigDecimal;
import java.util.List;

import me.iofdev.leadhunter.maps.ScrapeRequest;

/** What a campaign run will ask the scraper for, and the most it can cost. */
public record SearchPlan(List<ScrapeRequest> requests, int maxPlaces, BigDecimal estimatedMaxUsd) {
}
