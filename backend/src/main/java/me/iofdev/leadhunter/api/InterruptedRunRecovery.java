package me.iofdev.leadhunter.api;

import me.iofdev.leadhunter.pipeline.RunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * A run interrupted by a restart is marked failed on startup (ADR 0033).
 * There is no resume. This also keeps the CLI working after a killed web
 * run: stale RUNNING rows would trip the one-job-per-campaign index.
 */
@Component
class InterruptedRunRecovery implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InterruptedRunRecovery.class);

    private final RunRepository runs;

    InterruptedRunRecovery(RunRepository runs) {
        this.runs = runs;
    }

    @Override
    public void run(ApplicationArguments args) {
        int marked = runs.markInterrupted();
        if (marked > 0) {
            log.warn("Marked {} interrupted run(s) as failed", marked);
        }
    }
}
