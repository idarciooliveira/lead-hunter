package me.iofdev.leadhunter.api;

/** Error body for the JSON API. Mirrors the CLI's `error: <message>` on stderr. */
public record ApiError(String message) {
}
