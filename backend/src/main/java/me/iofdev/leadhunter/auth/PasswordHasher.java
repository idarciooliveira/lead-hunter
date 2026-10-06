package me.iofdev.leadhunter.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.HexFormat;

import org.bouncycastle.crypto.generators.SCrypt;
import org.springframework.stereotype.Component;

/**
 * Writes and checks passwords in Better Auth's format, so an account made in the CLI can sign in on the web
 * (ADR 0042). The format is {@code <salt hex>:<key hex>}. The password is NFKC-normalised and hashed with
 * scrypt (N=16384, r=16, p=1, 64-byte key). The salt is 16 random bytes written as 32 hex characters, and
 * scrypt takes those 32 characters as text, not the 16 bytes behind them. Two committed fixtures pin all of it.
 */
@Component
public class PasswordHasher {

    private static final int N = 16384;
    private static final int R = 16;
    private static final int P = 1;
    private static final int KEY_BYTES = 64;
    private static final int SALT_BYTES = 16;
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;

    private final SecureRandom random = new SecureRandom();

    /** Refuses passwords Better Auth would also refuse. */
    public static void requireValid(String password) {
        if (password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "the password must have from " + MIN_LENGTH + " to " + MAX_LENGTH + " characters");
        }
    }

    public String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        return hash(password, HexFormat.of().formatHex(salt));
    }

    /** The hash for a given salt. Package-private so a test can pin the output. */
    String hash(String password, String saltHex) {
        return saltHex + ":" + HexFormat.of().formatHex(key(password, saltHex));
    }

    public boolean verify(String hash, String password) {
        int colon = hash.indexOf(':');
        if (colon <= 0 || colon == hash.length() - 1) {
            return false;
        }
        byte[] expected;
        try {
            expected = HexFormat.of().parseHex(hash.substring(colon + 1));
        } catch (IllegalArgumentException e) {
            return false;
        }
        return MessageDigest.isEqual(expected, key(password, hash.substring(0, colon)));
    }

    private static byte[] key(String password, String saltHex) {
        byte[] secret = Normalizer.normalize(password, Normalizer.Form.NFKC).getBytes(StandardCharsets.UTF_8);
        return SCrypt.generate(secret, saltHex.getBytes(StandardCharsets.UTF_8), N, R, P, KEY_BYTES);
    }
}
