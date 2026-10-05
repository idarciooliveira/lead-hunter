package me.iofdev.leadhunter.api;

import java.util.List;

/** What a write endpoint answers: the saved thing and the CLI's non-blocking warnings. */
record SaveResult<T>(T saved, List<String> warnings) {
}
