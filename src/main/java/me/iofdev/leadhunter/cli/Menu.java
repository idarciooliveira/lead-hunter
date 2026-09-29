package me.iofdev.leadhunter.cli;

import java.util.List;
import java.util.function.Supplier;

import me.iofdev.leadhunter.campaign.Campaign;

/**
 * The interactive menu. Every entry runs an existing command through {@link Executor}, so output,
 * errors and validation stay in one place. See ADR 0022.
 */
final class Menu {

    /** Runs a command line such as {@code leads list my-campaign} and returns its exit code. */
    interface Executor {
        int execute(String... args);
    }

    private static final List<String> MAIN = List.of(
            "Run a campaign", "Browse leads", "New campaign", "Company profile", "Usage and costs");
    private static final List<String> COMPANY = List.of("Show the profile", "Set up or change it");
    private static final String STAGE_LETTERS = "qbea";

    private final Prompter prompter;
    private final Executor executor;
    private final Supplier<List<Campaign>> campaigns;

    Menu(Prompter prompter, Executor executor, Supplier<List<Campaign>> campaigns) {
        this.prompter = prompter;
        this.executor = executor;
        this.campaigns = campaigns;
    }

    void run() {
        try {
            while (true) {
                prompter.out().println();
                int choice = prompter.choose("Lead Hunter. Enter or 0 quits.", MAIN);
                switch (choice) {
                    case 1 -> runCampaign();
                    case 2 -> browseLeads();
                    case 3 -> executor.execute("campaign", "new");
                    case 4 -> companyProfile();
                    case 5 -> executor.execute("usage");
                    default -> {
                        return;
                    }
                }
            }
        } catch (Prompter.InputEnded e) {
            prompter.out().println();
        }
    }

    private void runCampaign() {
        String slug = pickCampaign("Which campaign?");
        if (slug == null) {
            return;
        }
        if (executor.execute("campaign", "run", slug, "--dry-run") != 0) {
            return;
        }
        if (prompter.confirm("Start the real run? It spends Apify credit.")) {
            executor.execute("campaign", "run", slug);
        }
    }

    private void browseLeads() {
        String slug = pickCampaign("Whose leads?");
        if (slug == null) {
            return;
        }
        String stage = "QUALIFIED";
        boolean list = true;
        while (true) {
            if (list && executor.execute("leads", "list", slug, "--stage", stage) != 0) {
                return;
            }
            list = false;
            String answer = prompter.ask(
                    "A lead id opens it. q qualified, b below the cut, e excluded, a all. Enter goes back.",
                    null, null, false);
            if (answer == null) {
                return;
            }
            String lower = answer.toLowerCase();
            if (lower.length() == 1 && STAGE_LETTERS.contains(lower)) {
                stage = switch (lower.charAt(0)) {
                    case 'q' -> "QUALIFIED";
                    case 'b' -> "BELOW_CUT";
                    case 'e' -> "EXCLUDED";
                    default -> "ALL";
                };
                list = true;
            } else if (answer.matches("\\d+")) {
                executor.execute("leads", "show", answer);
            } else {
                prompter.note("Type a lead id, one of q b e a, or press Enter.");
            }
        }
    }

    private void companyProfile() {
        switch (prompter.choose("Company profile. Enter or 0 goes back.", COMPANY)) {
            case 1 -> executor.execute("company", "show");
            case 2 -> executor.execute("company", "setup");
            default -> {
            }
        }
    }

    /** The slug of the chosen campaign, or null when there are none or the user goes back. */
    private String pickCampaign(String question) {
        List<Campaign> all = campaigns.get();
        if (all.isEmpty()) {
            prompter.out().println("No campaigns yet. Choose New campaign first.");
            return null;
        }
        List<String> labels = all.stream()
                .map(c -> c.slug() + "  " + Format.truncate(c.name(), 40))
                .toList();
        int picked = prompter.choose(question + " Enter or 0 goes back.", labels);
        return picked == 0 ? null : all.get(picked - 1).slug();
    }
}
