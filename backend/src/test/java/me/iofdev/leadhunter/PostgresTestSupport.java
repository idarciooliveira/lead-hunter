package me.iofdev.leadhunter;

import me.iofdev.leadhunter.api.ApiTestAuth;
import me.iofdev.leadhunter.auth.OrgId;
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
 * {@code LEADHUNTER_TEST_JDBC_URL} at an existing empty database instead. With neither, they are skipped, unless
 * {@code LEADHUNTER_REQUIRE_DB=true} is set (as {@code ./check} and CI do), which makes them fail instead.
 * JUnit does not inherit {@code @EnabledIf} from a superclass, so each subclass must carry
 * {@code @EnabledIf("me.iofdev.leadhunter.PostgresTestSupport#databaseAvailable")}.
 */
@SpringBootTest(properties = "leadhunter.cli.enabled=false")
public abstract class PostgresTestSupport {

    private static final String EXTERNAL_URL = System.getenv("LEADHUNTER_TEST_JDBC_URL");
    private static PostgreSQLContainer postgres;

    /** The organization every test works in, with {@link ApiTestAuth#USER_ID} as its member. */
    protected static final OrgId ORG = new OrgId(ApiTestAuth.ORG_ID);

    @Autowired
    protected JdbcClient jdbc;

    public static boolean databaseAvailable() {
        boolean available = EXTERNAL_URL != null || DockerClientFactory.instance().isDockerAvailable();
        return databaseAvailable(available, Boolean.parseBoolean(System.getenv("LEADHUNTER_REQUIRE_DB")));
    }

    static boolean databaseAvailable(boolean available, boolean required) {
        if (!available && required) {
            throw new IllegalStateException("LEADHUNTER_REQUIRE_DB is set but no database is available. "
                    + "Start Docker or set LEADHUNTER_TEST_JDBC_URL to an empty Postgres database.");
        }
        return available;
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("leadhunter.api.token", () -> ApiTestAuth.TOKEN);
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
        jdbc.sql("truncate invitation, member, auth_session, auth_account, auth_verification, app_user, organization, lead, campaign_run, llm_call, website_crawl, place_review, place, campaign, company restart identity cascade").update();
        jdbc.sql("insert into organization (id, name, slug) values (:id, 'Test', 'test')").param("id", ApiTestAuth.ORG_ID).update();
        jdbc.sql("insert into app_user (id, name, email) values (:id, 'Test User', 'test@example.com')")
                .param("id", ApiTestAuth.USER_ID).update();
        jdbc.sql("insert into member (id, organization_id, user_id, role) values ('test-member', :org, :user, 'owner')")
                .param("org", ApiTestAuth.ORG_ID).param("user", ApiTestAuth.USER_ID).update();
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : value;
    }
}
