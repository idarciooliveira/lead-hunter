package me.iofdev.leadhunter.pipeline;

import java.math.BigDecimal;

import me.iofdev.leadhunter.maps.ScrapeRequest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class RunRepository {

    private final JdbcClient jdbc;

    public RunRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public long start(long campaignId, ScrapeRequest request) {
        return jdbc.sql("""
                        insert into campaign_run (campaign_id, location, search_terms, max_places, status)
                        values (:campaignId, :location, :terms, :maxPlaces, 'RUNNING')
                        returning id
                        """)
                .param("campaignId", campaignId)
                .param("location", request.location())
                .param("terms", request.terms().toArray(String[]::new))
                .param("maxPlaces", request.maxPlaces())
                .query(Long.class)
                .single();
    }

    public void succeed(long runId, String externalRunId, String datasetId, int placesFound, BigDecimal costUsd) {
        jdbc.sql("""
                        update campaign_run
                        set status = 'SUCCEEDED', external_run_id = :externalRunId, dataset_id = :datasetId,
                            places_found = :placesFound, cost_usd = :costUsd, finished_at = now()
                        where id = :id
                        """)
                .param("id", runId)
                .param("externalRunId", externalRunId)
                .param("datasetId", datasetId)
                .param("placesFound", placesFound)
                .param("costUsd", costUsd)
                .update();
    }

    /** A failed run still costs money, so costUsd is stored when Apify reported one. */
    public void fail(long runId, String externalRunId, String error, BigDecimal costUsd) {
        jdbc.sql("""
                        update campaign_run
                        set status = 'FAILED', external_run_id = :externalRunId, error = :error,
                            cost_usd = :costUsd, finished_at = now()
                        where id = :id
                        """)
                .param("id", runId)
                .param("externalRunId", externalRunId)
                .param("error", error)
                .param("costUsd", costUsd)
                .update();
    }
}
