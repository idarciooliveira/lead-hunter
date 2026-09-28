package me.iofdev.leadhunter.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import me.iofdev.leadhunter.campaign.CampaignFile;
import me.iofdev.leadhunter.scoring.Text;

/**
 * Asks the 10 onboarding questions plus the search settings in the terminal and builds a campaign.
 * See ADR 0018.
 */
final class CampaignWizard {

    static final String DEFAULT_LOCATION = "Luanda, Angola";
    static final int DEFAULT_WEEKLY_CAPACITY = 35;

    private final BufferedReader in;
    private final PrintWriter out;

    CampaignWizard(BufferedReader in, PrintWriter out) {
        this.in = in;
        this.out = out;
    }

    CampaignFile run() {
        out.println("New campaign. Press Enter to accept the value in [brackets].");
        out.println("Lists take one item per line; an empty line finishes the list.");
        out.println();

        String name = ask("Campaign name", "e.g. Clínicas em Luanda", null, true);
        String slug = ask("Short id for commands", "lowercase and dashes", slugify(name), true);

        section("The 10 questions");
        String offer = ask("1/10  What do you sell, in one sentence, and what does it cost?",
                "e.g. Websites e apps para PMEs, projectos a partir de 500 mil Kz", null, true);
        String buyers = ask("2/10  Which types of businesses buy from you?",
                "e.g. Clínicas, escolas, lojas", null, true);
        String area = ask("3/10  Where do you operate?", null, "Luanda", true);
        List<String> referenceClients = askList("4/10  Your best current customers",
                "used to calibrate scoring, and never shown as leads", List.of(), false);
        String idealSize = ask("5/10  How big is a good customer?",
                "e.g. 1 to 3 locations, regular customers", null, false);
        String visiblePain = ask("6/10  What problem do you solve, and how does it show from the outside?",
                "e.g. no website, bookings only by phone", null, false);
        String exclusions = ask("7/10  Who should never show up?",
                "banks, government, telecoms and big chains are always excluded", null, false);
        String decisionMaker = ask("8/10  Who decides, and how do you reach them?", null,
                "Owner or manager. WhatsApp message or phone call.", false);
        String proof = ask("9/10  What makes you different, and what results can you prove?", null, null, false);
        int weeklyCapacity = askInt("10/10 How many leads can you contact per week?", DEFAULT_WEEKLY_CAPACITY, 1, 500);

        section("Search");
        List<String> terms = askList("Google Maps search terms", "e.g. clínica, clínica dentária", List.of(), true);
        List<String> locations = askList("Locations, one scraper run each", "e.g. Talatona, Luanda, Angola",
                List.of(DEFAULT_LOCATION), true);
        int maxPlacesPerSearch = askInt("Max places per term per location",
                CampaignFile.Search.DEFAULT_MAX_PLACES_PER_SEARCH, 1, 200);
        List<String> targetKeywords = askList("Keywords that mark a target sector",
                "matched as whole words in the name or category", terms, false);
        List<String> excludeNames = askList("Names to exclude",
                "current clients go here", referenceClients, false);

        return new CampaignFile(slug, name,
                new CampaignFile.Answers(offer, buyers, area, referenceClients, idealSize, visiblePain, exclusions,
                        decisionMaker, proof, weeklyCapacity),
                new CampaignFile.Search(terms, locations, maxPlacesPerSearch, null, targetKeywords, null,
                        excludeNames, null));
    }

    boolean confirm(String question) {
        out.print(question + " [y/N] > ");
        out.flush();
        String answer = readLine().trim().toLowerCase(Locale.ROOT);
        return answer.equals("y") || answer.equals("yes") || answer.equals("s") || answer.equals("sim");
    }

    static String slugify(String value) {
        String slug = Text.normalize(value).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        return slug.isEmpty() ? null : slug;
    }

    private void section(String title) {
        out.println();
        out.println("— " + title);
    }

    private String ask(String question, String hint, String defaultValue, boolean required) {
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
            out.println("      Required.");
        }
    }

    private int askInt(String question, int defaultValue, int min, int max) {
        while (true) {
            prompt(question, null, String.valueOf(defaultValue));
            String line = readLine().trim();
            if (line.isEmpty()) {
                return defaultValue;
            }
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // fall through to the message below
            }
            out.printf("      Enter a whole number from %d to %d.%n", min, max);
        }
    }

    private List<String> askList(String question, String hint, List<String> defaults, boolean required) {
        while (true) {
            out.println(question);
            if (hint != null) {
                out.println("      " + hint);
            }
            if (!defaults.isEmpty()) {
                out.println("      [" + String.join("; ", defaults) + "]");
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
            out.println("      Add at least one.");
        }
    }

    private void prompt(String question, String hint, String defaultValue) {
        out.println(question);
        if (hint != null) {
            out.println("      " + hint);
        }
        out.print(defaultValue == null ? "> " : "[" + defaultValue + "] > ");
        out.flush();
    }

    private String readLine() {
        try {
            String line = in.readLine();
            if (line == null) {
                throw new IllegalStateException("input ended before the campaign was complete. "
                        + "In Docker, run it with: docker compose run --rm app campaign new");
            }
            return line;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
