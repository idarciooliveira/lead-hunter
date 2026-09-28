package me.iofdev.leadhunter.company;

import java.util.List;

public class InvalidCompanyException extends RuntimeException {

    private final List<String> problems;

    public InvalidCompanyException(List<String> problems) {
        super("invalid company profile:\n  - " + String.join("\n  - ", problems));
        this.problems = List.copyOf(problems);
    }

    public List<String> problems() {
        return problems;
    }
}
