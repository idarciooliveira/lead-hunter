package me.iofdev.leadhunter.api;

import me.iofdev.leadhunter.pipeline.RunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;

/**
 * Jobs a restart interrupted are marked failed when the web server boots (ADR
 * 0033). There is no resume. Only jobs with no live owner are touched (ADR
 * 0036), so a CLI run or another server keeps its jobs; the CLI fails
 * abandoned jobs of a campaign itself before it starts one.
 */
@Component
@ConditionalOnWebApplication
class InterruptedRunRecovery implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InterruptedRunRecovery.class);

    private final RunRepository runs;

    InterruptedRunRecovery(RunRepository runs) {
        this.runs = runs;
    }

    @Override
    public void run(ApplicationArguments args) {
        int marked = runs.failAbandoned(null);
        if (marked > 0) {
            log.warn("Marked {} interrupted run(s) as failed", marked);
        }
    }
}
