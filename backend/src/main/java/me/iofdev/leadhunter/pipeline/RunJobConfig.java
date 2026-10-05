package me.iofdev.leadhunter.pipeline;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * The bounded executor ADR 0033 runs background jobs on. One thread: with a
 * single owner, jobs simply queue behind each other, and the partial unique
 * index in V6 keeps the one-job-per-campaign promise. A waiting job already
 * holds its lease and so a pooled connection (ADR 0036), so the queue stays
 * well below the pool's ten connections; past it a start fails at once.
 */
@Configuration
class RunJobConfig {

    @Bean("runJobs")
    TaskExecutor runJobs() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(4);
        executor.setThreadNamePrefix("run-job-");
        executor.initialize();
        return executor;
    }
}
