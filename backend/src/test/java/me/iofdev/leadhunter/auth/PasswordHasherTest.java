package me.iofdev.leadhunter.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * The hash format belongs to Better Auth (ADR 0042). The first fixture was made by its {@code hashPassword}, the
 * second by node's scrypt with a fixed salt. If a Better Auth upgrade changes the format, these fail before a
 * CLI-made account stops signing in.
 */
class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void acceptsAHashBetterAuthMade() throws IOException {
        Properties fixture = fixture("better-auth-hash.properties");

        assertThat(hasher.verify(fixture.getProperty("hash"), fixture.getProperty("password"))).isTrue();
        assertThat(hasher.verify(fixture.getProperty("hash"), "wrong password")).isFalse();
    }

    @Test
    void makesTheHashTheWebTestChecks() throws IOException {
        Properties fixture = fixture("java-hash.properties");

        assertThat(hasher.hash(fixture.getProperty("password"), fixture.getProperty("salt")))
                .isEqualTo(fixture.getProperty("hash"));
    }

    @Test
    void normalisesThePasswordLikeBetterAuth() {
        // NFKC turns the circled digit into a plain one, so both spellings are the same password.
        assertThat(hasher.verify(hasher.hash("senha①②③④"), "senha1234")).isTrue();
    }

    @Test
    void writesSaltAndKeyInHex() {
        String hash = hasher.hash("a long enough password");

        assertThat(hash).matches("[0-9a-f]{32}:[0-9a-f]{128}");
        assertThat(hasher.hash("a long enough password")).isNotEqualTo(hash);
    }

    @Test
    void rejectsMalformedHashesWithoutThrowing() {
        assertThat(hasher.verify("", "x")).isFalse();
        assertThat(hasher.verify("nocolon", "x")).isFalse();
        assertThat(hasher.verify("salt:", "x")).isFalse();
        assertThat(hasher.verify("salt:not-hex", "x")).isFalse();
    }

    @Test
    void holdsPasswordsToBetterAuthsLimits() {
        assertThatThrownBy(() -> PasswordHasher.requireValid("short")).hasMessageContaining("8 to 128");
        assertThatThrownBy(() -> PasswordHasher.requireValid("x".repeat(129))).isInstanceOf(IllegalArgumentException.class);
        PasswordHasher.requireValid("12345678");
        PasswordHasher.requireValid("x".repeat(128));
    }

    private static Properties fixture(String name) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = new InputStreamReader(
                PasswordHasherTest.class.getResourceAsStream("/auth/" + name), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }
}
