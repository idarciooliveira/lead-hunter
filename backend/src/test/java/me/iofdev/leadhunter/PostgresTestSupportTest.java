package me.iofdev.leadhunter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PostgresTestSupportTest {

    @Test
    void skipsWhenNoDatabaseAndNotRequired() {
        assertThat(PostgresTestSupport.databaseAvailable(false, false)).isFalse();
    }

    @Test
    void failsWhenNoDatabaseAndRequired() {
        assertThatThrownBy(() -> PostgresTestSupport.databaseAvailable(false, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LEADHUNTER_TEST_JDBC_URL");
    }

    @Test
    void runsWhenADatabaseIsAvailable() {
        assertThat(PostgresTestSupport.databaseAvailable(true, true)).isTrue();
    }
}
