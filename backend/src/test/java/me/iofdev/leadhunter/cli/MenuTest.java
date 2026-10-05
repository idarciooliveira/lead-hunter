package me.iofdev.leadhunter.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import me.iofdev.leadhunter.campaign.Campaign;
import org.junit.jupiter.api.Test;

class MenuTest {

    private final StringWriter output = new StringWriter();
    private final List<String> commands = new ArrayList<>();
    private int exitCode = 0;

    private void run(String input, List<Campaign> campaigns) {
        Prompter prompter = new Prompter(new BufferedReader(new StringReader(input)),
                new PrintWriter(output, true), "input ended");
        new Menu(prompter, args -> {
            commands.add(String.join(" ", args));
            return exitCode;
        }, () -> campaigns).run();
    }

    private static Campaign campaign(String slug, String name) {
        return new Campaign(1, slug, name, null, null, null, null);
    }

    @Test
    void enterQuits() {
        run("\n", List.of());

        assertThat(output.toString()).contains("1. Run a campaign").contains("5. Usage and costs");
        assertThat(commands).isEmpty();
    }

    @Test
    void printsTheFoxBannerOnceInPlainMode() {
        run("1\n0\n\n", List.of(campaign("escolas", "Escolas")));

        String printed = output.toString();
        assertThat(printed).contains("LEAD HUNTER");
        assertThat(printed.indexOf("LEAD HUNTER"))
                .isEqualTo(printed.lastIndexOf("LEAD HUNTER"));
        assertThat(printed).doesNotContain("\u001B");
    }

    @Test
    void runsTheDryRunFirstAndStartsTheRealRunOnlyAfterYes() {
        List<Campaign> campaigns = List.of(campaign("escolas", "Escolas"), campaign("clinicas", "Clínicas"));

        run("1\n2\ny\n0\n", campaigns);

        assertThat(commands).containsExactly("campaign run clinicas --dry-run", "campaign run clinicas");
    }

    @Test
    void declinesTheRealRunByDefault() {
        run("1\n1\n\n0\n", List.of(campaign("escolas", "Escolas")));

        assertThat(commands).containsExactly("campaign run escolas --dry-run");
    }

    @Test
    void skipsTheRealRunWhenTheDryRunFails() {
        exitCode = 1;

        run("1\n1\n0\n", List.of(campaign("escolas", "Escolas")));

        assertThat(commands).containsExactly("campaign run escolas --dry-run");
        assertThat(output.toString()).doesNotContain("Start the real run");
    }

    @Test
    void browsesLeadsChangesStageAndOpensALead() {
        run("2\n1\nb\n42\nfoo\n\n0\n", List.of(campaign("escolas", "Escolas")));

        assertThat(commands).containsExactly(
                "leads list escolas --stage QUALIFIED",
                "leads list escolas --stage BELOW_CUT",
                "leads show 42");
        assertThat(output.toString()).contains("Type a lead id, one of q b e a, or press Enter.");
    }

    @Test
    void saysSoWhenThereAreNoCampaigns() {
        run("1\n0\n", List.of());

        assertThat(output.toString()).contains("No campaigns yet. Choose New campaign first.");
        assertThat(commands).isEmpty();
    }

    @Test
    void mapsTheOtherEntriesToTheirCommands() {
        run("3\n4\n1\n4\n2\n4\n\n5\n0\n", List.of());

        assertThat(commands).containsExactly("campaign new", "company show", "company setup", "usage");
    }

    @Test
    void asksAgainOnANumberOutsideTheMenu() {
        run("9\n0\n", List.of());

        assertThat(output.toString()).contains("Enter a whole number from 0 to 6.");
    }

    @Test
    void enrichesOnlyAfterYes() {
        run("6\n1\ny\n0\n", List.of(campaign("escolas", "Escolas")));

        assertThat(commands).containsExactly("campaign enrich escolas --dry-run", "campaign enrich escolas");
    }

    @Test
    void declinesEnrichingByDefault() {
        run("6\n1\n\n0\n", List.of(campaign("escolas", "Escolas")));

        assertThat(commands).containsExactly("campaign enrich escolas --dry-run");
    }

    @Test
    void closedInputEndsTheMenuWithoutAnError() {
        run("", List.of());

        assertThat(commands).isEmpty();
    }
}
