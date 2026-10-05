package me.iofdev.leadhunter.pipeline;

import java.util.List;

import me.iofdev.leadhunter.maps.ExternalRunResult;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class RunRepository {

    /** UsageRepository labels runs by location, so review runs use this location. A real kind column belongs with the ADR 0033 migration. */
    public static final String REVIEWS_LOCATION = "reviews";

    private final JdbcClient jdbc;

    public RunRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public long start(long campaignId, String location, List<String> terms, int maxPlaces) {
        return jdbc.sql("""
                        insert into campaign_run (campaign_id, location, search_terms, max_places, status)
                        values (:campaignId, :location, :terms, :maxPlaces, 'RUNNING')
                        returning id
                        """)
                .param("campaignId", campaignId)
                .param("location", location)
                .param("terms", terms.toArray(String[]::new))
                .param("maxPlaces", maxPlaces)
                .query(Long.class)
                .single();
    }

    public void succeed(long runId, ExternalRunResult result, int placesFound) {
        jdbc.sql("""
                        update campaign_run
                        set status = 'SUCCEEDED', external_run_id = :externalRunId, dataset_id = :datasetId,
                            places_found = :placesFound, cost_usd = :costUsd, finished_at = now()
                        where id = :id
                        """)
                .param("id", runId)
                .param("externalRunId", result.externalRunId())
                .param("datasetId", result.datasetId())
                .param("placesFound", placesFound)
                .param("costUsd", result.costUsd())
                .update();
    }

    /** A failed run still costs money, so costUsd is stored when Apify reported one. */
    public void fail(long runId, RunFailure failure) {
        jdbc.sql("""
                        update campaign_run
                        set status = 'FAILED', external_run_id = :externalRunId, error = :error,
                            cost_usd = :costUsd, finished_at = now()
                        where id = :id
                        """)
                .param("id", runId)
                .param("externalRunId", failure.externalRunId())
                .param("error", failure.error())
                .param("costUsd", failure.costUsd())
                .update();
    }
}
