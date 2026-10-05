package me.iofdev.leadhunter.apify;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import me.iofdev.leadhunter.maps.ScrapeException;
import tools.jackson.databind.JsonNode;

/** Starts an actor run, polls it to the end, and settles the cost. Shared by discovery and review runs. */
final class ApifyRuns {

    private ApifyRuns() {
    }

    record Finished(ApifyRun run, List<JsonNode> items) {
    }

    static Finished run(ApifyClient client, ApifyProperties properties, Object input) {
        properties.requireToken();
        ApifyRun run = awaitFinished(client, properties, client.startRun(properties.actorId(), input));
        JsonNode result = client.datasetItems(run.defaultDatasetId());
        List<JsonNode> items = new ArrayList<>();
        if (result != null && result.isArray()) {
            for (JsonNode item : result) {
                items.add(item);
            }
        }
        return new Finished(run, items);
    }

    static ApifyRun awaitFinished(ApifyClient client, ApifyProperties properties, ApifyRun run) {
        Instant deadline = Instant.now().plus(properties.maxRunTime());
        while (!run.finished()) {
            if (Instant.now().isAfter(deadline)) {
                throw new ScrapeException("Apify run " + run.id() + " still " + run.status()
                        + " after " + properties.maxRunTime().toMinutes() + " minutes, aborted",
                        run.id(), abortAndSettle(client, properties, run));
            }
            run = client.getRun(run.id(), properties.pollWait());
        }
        run = settle(client, properties, run);
        if (!run.succeeded()) {
            throw new ScrapeException("Apify run " + run.id() + " ended with status " + run.status(),
                    run.id(), run.usageTotalUsd());
        }
        return run;
    }

    /** Apify keeps billing a run we stop watching, so a timeout aborts it. Returns the cost, or null if Apify can't tell us. */
    private static BigDecimal abortAndSettle(ApifyClient client, ApifyProperties properties, ApifyRun run) {
        try {
            client.abortRun(run.id());
            return settle(client, properties, run).usageTotalUsd();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** The first answer after a run ends can hold a preliminary cost. Wait, then read the run again. */
    private static ApifyRun settle(ApifyClient client, ApifyProperties properties, ApifyRun run) {
        try {
            Thread.sleep(properties.costSettleDelay());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return run;
        }
        return client.getRun(run.id(), Duration.ZERO);
    }
}
