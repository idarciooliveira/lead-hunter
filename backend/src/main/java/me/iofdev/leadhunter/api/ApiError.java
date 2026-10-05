package me.iofdev.leadhunter.api;

import java.util.List;

/**
 * Error body for the JSON API. Mirrors the CLI's `error: <message>` on stderr. {@code problems} lists
 * each failed validation rule on its own, empty for every other error.
 */
public record ApiError(String message, List<String> problems) {

    public ApiError(String message) {
        this(message, List.of());
    }
}
