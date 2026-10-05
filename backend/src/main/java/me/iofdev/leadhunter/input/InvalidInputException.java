package me.iofdev.leadhunter.input;

import java.util.List;

public class InvalidInputException extends RuntimeException {

    private final List<String> problems;

    public InvalidInputException(String what, List<String> problems) {
        super("invalid " + what + ":\n  - " + String.join("\n  - ", problems));
        this.problems = List.copyOf(problems);
    }

    public List<String> problems() {
        return problems;
    }
}
