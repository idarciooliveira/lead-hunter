package me.iofdev.leadhunter.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import me.iofdev.leadhunter.PostgresTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import picocli.CommandLine;

@EnabledIf("me.iofdev.leadhunter.PostgresTestSupport#databaseAvailable")
class CliIntegrationTest extends PostgresTestSupport {

    @Autowired
    SpringCommandFactory factory;

    private record Result(int exitCode, String out, String err) {
    }

    private Result execute(String... args) {
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        CommandLine commandLine = CliRunner.newCommandLine(factory);
        commandLine.setOut(new PrintWriter(out));
        commandLine.setErr(new PrintWriter(err));
        int exitCode = commandLine.execute(args);
        return new Result(exitCode, out.toString(), err.toString());
    }

    private Result executeWithInput(String input, String... args) {
        InputStream original = System.in;
        System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        try {
            return execute(args);
        } finally {
            System.setIn(original);
        }
    }

    @Test
    void newAsksTheQuestionsSavesTheCampaignAndWritesItsFile(@TempDir Path dir) throws Exception {
        Result result = executeWithInput(CampaignWizardTest.ANSWERS, "campaign", "new", "--dir", dir.toString());

        assertThat(result.exitCode()).as(result.err()).isZero();
        assertThat(result.out())
                .contains("1/10  What do you sell")
                .contains("Saved campaign 'escolas-em-luanda'")
                .contains("Next: campaign run escolas-em-luanda --dry-run");
        Path file = dir.resolve("escolas-em-luanda.yml");
        assertThat(Files.readString(file)).contains("colégio");

        // The written file feeds straight back into `campaign create`.
        assertThat(execute("campaign", "create", "-f", file.toString()).out())
                .contains("Updated campaign 'escolas-em-luanda'");

        // Running it again for the same slug asks first; answering no keeps the saved campaign.
        Result again = executeWithInput(CampaignWizardTest.ANSWERS.replace("Sites e apps para PMEs", "Outra oferta") + "n\n",
                "campaign", "new", "--dir", dir.toString());
        assertThat(again.out()).contains("already exists. Replace it?").contains("Nothing saved.");
        assertThat(Files.readString(file)).doesNotContain("Outra oferta");
    }

    @Test
    void createsAndPlansTheExampleCampaign() {
        Path file = Path.of("campaigns", "clinicas-luanda.yml");

        Result created = execute("campaign", "create", "--file", file.toString());
        assertThat(created.exitCode()).isZero();
        assertThat(created.out()).contains("Created campaign 'clinicas-luanda'");

        Result dryRun = execute("campaign", "run", "clinicas-luanda", "--dry-run");
        assertThat(dryRun.exitCode()).isZero();
        assertThat(dryRun.out())
                .contains("would start 4 scraper runs")
                .contains("Talatona, Luanda, Angola: clínica, clínica dentária, centro médico, up to 120 places")
                .contains("Up to 480 places, about $1.92");

        Result list = execute("campaign", "list");
        assertThat(list.out()).contains("clinicas-luanda").contains("0.0000");

        Result leads = execute("leads", "list", "clinicas-luanda");
        assertThat(leads.out()).contains("No qualified leads");
        assertThat(execute("leads", "list", "clinicas-luanda", "--stage", "ALL").out())
                .startsWith("No leads in 'clinicas-luanda'");
    }

    @Test
    void reportsErrorsWithoutStackTraces() {
        Result missing = execute("campaign", "run", "nope");

        assertThat(missing.exitCode()).isEqualTo(1);
        assertThat(missing.err()).isEqualToIgnoringNewLines("error: no campaign 'nope'. Run: campaign list");
    }
}
