package me.iofdev.leadhunter;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Integration tests run against a Postgres started by Testcontainers. Without Docker, point
 * {@code LEADHUNTER_TEST_JDBC_URL} at an existing empty database instead. With neither, they are skipped.
 * JUnit does not inherit {@code @EnabledIf} from a superclass, so each subclass must carry
 * {@code @EnabledIf("me.iofdev.leadhunter.PostgresTestSupport#databaseAvailable")}.
 */
@SpringBootTest(properties = "leadhunter.cli.enabled=false")
public abstract class PostgresTestSupport {

    private static final String EXTERNAL_URL = System.getenv("LEADHUNTER_TEST_JDBC_URL");
    private static PostgreSQLContainer postgres;

    @Autowired
    protected JdbcClient jdbc;

    public static boolean databaseAvailable() {
        return EXTERNAL_URL != null || DockerClientFactory.instance().isDockerAvailable();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        if (EXTERNAL_URL != null) {
            registry.add("spring.datasource.url", () -> EXTERNAL_URL);
            registry.add("spring.datasource.username", () -> env("LEADHUNTER_TEST_DB_USER", "leadhunter"));
            registry.add("spring.datasource.password", () -> env("LEADHUNTER_TEST_DB_PASSWORD", "leadhunter"));
            return;
        }
        synchronized (PostgresTestSupport.class) {
            if (postgres == null) {
                postgres = new PostgreSQLContainer("postgres:17");
                postgres.start();
            }
        }
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void cleanDatabase() {
        jdbc.sql("truncate lead, campaign_run, place, campaign, company restart identity cascade").update();
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : value;
    }
}
