package me.iofdev.leadhunter.api;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Stops the API from starting without a service token, so a missing variable is never an open API (ADR 0037). */
@Component
@Profile("web")
class ApiTokenGuard {

    ApiTokenGuard(ApiProperties properties) {
        if (properties.token().isBlank()) {
            throw new IllegalStateException(
                    "LEADHUNTER_API_TOKEN is not set. The API requires it on every call; set it in .env or the environment");
        }
    }
}
