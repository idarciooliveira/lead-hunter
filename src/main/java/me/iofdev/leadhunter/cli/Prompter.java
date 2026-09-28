package me.iofdev.leadhunter.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Terminal questions for the wizards. Enter accepts the value in [brackets]. */
final class Prompter {

    private final BufferedReader in;
    private final PrintWriter out;
    private final String inputEndedMessage;

    Prompter(BufferedReader in, PrintWriter out, String inputEndedMessage) {
        this.in = in;
        this.out = out;
        this.inputEndedMessage = inputEndedMessage;
    }

    PrintWriter out() {
        return out;
    }

    void section(String title) {
        out.println();
        out.println("— " + title);
    }

    void note(String message) {
        out.println("      " + message);
    }

    String ask(String question, String hint, String defaultValue, boolean required) {
        while (true) {
            prompt(question, hint, defaultValue);
            String line = readLine().trim();
            if (!line.isEmpty()) {
                return line;
            }
            if (defaultValue != null) {
                return defaultValue;
            }
            if (!required) {
                return null;
            }
            note("Required.");
        }
    }

    int askInt(String question, String hint, int defaultValue, int min, int max) {
        Long value = askNumber(question, hint, (long) defaultValue, min, max, true);
        return value.intValue();
    }

    Integer askOptionalInt(String question, String hint, int min, int max) {
        Long value = askNumber(question, hint, null, min, max, false);
        return value == null ? null : value.intValue();
    }

    Long askOptionalLong(String question, String hint, long min, long max) {
        return askNumber(question, hint, null, min, max, false);
    }

    private Long askNumber(String question, String hint, Long defaultValue, long min, long max, boolean required) {
        while (true) {
            prompt(question, hint, defaultValue == null ? null : String.valueOf(defaultValue));
            String line = readLine().trim().replaceAll("[\\s.]", "");
            if (line.isEmpty()) {
                if (defaultValue != null || !required) {
                    return defaultValue;
                }
            } else {
                try {
                    long value = Long.parseLong(line);
                    if (value >= min && value <= max) {
                        return value;
                    }
                } catch (NumberFormatException ignored) {
                    // fall through to the message below
                }
            }
            out.printf("      Enter a whole number from %d to %d.%n", min, max);
        }
    }

    LocalDate askDate(String question, LocalDate defaultValue, LocalDate notBefore) {
        while (true) {
            prompt(question, "YYYY-MM-DD", defaultValue.toString());
            String line = readLine().trim();
            if (line.isEmpty()) {
                return defaultValue;
            }
            try {
                LocalDate date = LocalDate.parse(line);
                if (!date.isBefore(notBefore)) {
                    return date;
                }
                note("The date cannot be before " + notBefore + ".");
            } catch (DateTimeParseException e) {
                note("Use the format YYYY-MM-DD, like " + defaultValue + ".");
            }
        }
    }

    /** Returns the lowercase letter typed, or null for Enter. */
    Character askLetter(String question, String allowed) {
        while (true) {
            out.print(question + " > ");
            out.flush();
            String line = readLine().trim().toLowerCase(Locale.ROOT);
            if (line.isEmpty()) {
                return null;
            }
            if (line.length() == 1 && allowed.indexOf(line.charAt(0)) >= 0) {
                return line.charAt(0);
            }
            note("Type one of " + String.join(", ", allowed.split("")) + ", or press Enter.");
        }
    }

    boolean confirm(String question) {
        out.print(question + " [y/N] > ");
        out.flush();
        String answer = readLine().trim().toLowerCase(Locale.ROOT);
        return answer.equals("y") || answer.equals("yes") || answer.equals("s") || answer.equals("sim");
    }

    List<String> askList(String question, String hint, List<String> defaults, boolean required) {
        while (true) {
            out.println(question);
            if (hint != null) {
                note(hint);
            }
            if (!defaults.isEmpty()) {
                note("[" + String.join("; ", defaults) + "]");
            }
            List<String> values = new ArrayList<>();
            while (true) {
                out.print("> ");
                out.flush();
                String line = readLine().trim();
                if (line.isEmpty()) {
                    break;
                }
                values.add(line);
            }
            if (!values.isEmpty()) {
                return values;
            }
            if (!defaults.isEmpty() || !required) {
                return defaults;
            }
            note("Add at least one.");
        }
    }

    /**
     * A list of rows with fields separated by {@code |}. A row with fewer than {@code minFields} filled
     * fields is refused on the spot. Enter on the first line keeps {@code defaults}, shown one per line.
     */
    List<List<String>> askRows(String question, String format, List<List<String>> defaults, int minFields,
                               boolean required) {
        while (true) {
            out.println(question);
            note("One per line: " + format + ". An empty line finishes.");
            for (List<String> row : defaults) {
                note("[" + String.join(" | ", row) + "]");
            }
            List<List<String>> rows = new ArrayList<>();
            while (true) {
                out.print("> ");
                out.flush();
                String line = readLine().trim();
                if (line.isEmpty()) {
                    break;
                }
                List<String> fields = split(line);
                if (fields.stream().limit(minFields).anyMatch(String::isEmpty) || fields.size() < minFields) {
                    note("Not added. Use: " + format);
                    continue;
                }
                rows.add(fields);
            }
            if (!rows.isEmpty()) {
                return rows;
            }
            if (!defaults.isEmpty() || !required) {
                return defaults;
            }
            note("Add at least one.");
        }
    }

    static List<String> split(String line) {
        return Arrays.stream(line.split("\\|", -1)).map(String::trim).toList();
    }

    /** The field at {@code index}, or null when the row is shorter or the field is empty. */
    static String field(List<String> row, int index) {
        return index < row.size() && !row.get(index).isEmpty() ? row.get(index) : null;
    }

    private void prompt(String question, String hint, String defaultValue) {
        out.println(question);
        if (hint != null) {
            note(hint);
        }
        out.print(defaultValue == null ? "> " : "[" + defaultValue + "] > ");
        out.flush();
    }

    private String readLine() {
        try {
            String line = in.readLine();
            if (line == null) {
                throw new IllegalStateException(inputEndedMessage);
            }
            return line;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
