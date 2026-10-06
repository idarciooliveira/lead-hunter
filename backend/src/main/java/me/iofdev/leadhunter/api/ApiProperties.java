package me.iofdev.leadhunter.api;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param token the service token every {@code /api/**} call but the health probe must send as a bearer token
 *              (ADR 0037). Empty means no call is accepted.
 */
@ConfigurationProperties("leadhunter.api")
public record ApiProperties(@DefaultValue("") String token) {
}
