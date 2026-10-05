package me.iofdev.leadhunter.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * The bounded executor ADR 0033 runs background jobs on. One thread: with a
 * single owner, jobs simply queue behind each other, and the partial unique
 * index in V6 keeps the one-job-per-campaign promise even across restarts.
 */
@Configuration
class RunJobConfig {

    @Bean("runJobs")
    TaskExecutor runJobs() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("run-job-");
        executor.initialize();
        return executor;
    }
}
