package me.iofdev.leadhunter.scoring;

/** One rule that fired, with its points and the reason shown to the user. */
public record ScoreItem(String code, int points, String reason) {
}
