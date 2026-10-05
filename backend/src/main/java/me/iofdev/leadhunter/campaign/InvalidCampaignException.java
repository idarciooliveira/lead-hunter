package me.iofdev.leadhunter.campaign;

import java.util.List;

public class InvalidCampaignException extends RuntimeException {

    private final List<String> problems;

    public InvalidCampaignException(List<String> problems) {
        super("invalid campaign file:\n  - " + String.join("\n  - ", problems));
        this.problems = List.copyOf(problems);
    }

    public List<String> problems() {
        return problems;
    }
}
