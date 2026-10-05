package me.iofdev.leadhunter.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import me.iofdev.leadhunter.pipeline.RunRepository.JobView;

/**
 * One job as the UI polls it (ADR 0033). Scores, counts and costs are data,
 * as the rows hold them; the UI never computes them. {@code SUCCEEDED} rows
 * read as {@code DONE}, matching the history the web client already renders.
 */
public record RunDto(
        long id,
        String campaignSlug,
        String kind,
        String status,
        OffsetDateTime startedAt,
        long done,
        Integer total,
        BigDecimal costUsd,
        String error) {

    static RunDto from(JobView job) {
        return new RunDto(
                job.id(),
                job.campaignSlug(),
                job.kind(),
                job.status().equals("SUCCEEDED") ? "DONE" : job.status(),
                job.startedAt(),
                job.done(),
                job.total(),
                job.costUsd(),
                job.error());
    }
}
