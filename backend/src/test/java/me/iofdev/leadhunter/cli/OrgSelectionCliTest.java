package me.iofdev.leadhunter.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;

import me.iofdev.leadhunter.PostgresTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import picocli.CommandLine;

/** ADR 0043: {@code --org}, then the only organization, and an error when there are several. */
@EnabledIf("me.iofdev.leadhunter.PostgresTestSupport#databaseAvailable")
class OrgSelectionCliTest extends PostgresTestSupport {

    @Autowired
    SpringCommandFactory factory;

    @BeforeEach
    void twoOrganizations() {
        jdbc.sql("insert into organization (id, name, slug) values ('b-org', 'Beta', 'beta')").update();
        jdbc.sql("insert into campaign (org_id, slug, name, answers, search) values ('b-org', 'so-da-beta', 'Só da Beta', '{}'::jsonb, '{}'::jsonb)")
                .update();
    }

    @Test
    void severalOrganizationsAndNoChoiceIsAnError() {
        Result result = execute("campaign", "list");

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.err()).contains("choose an organization with --org or LEADHUNTER_ORG: ").contains("beta").contains("test");
    }

    @Test
    void theOrgOptionPicksTheOrganizationAndKeepsTheOthersApart() {
        Result beta = execute("--org", "beta", "campaign", "list");
        Result test = execute("--org", "test", "campaign", "list");

        assertThat(beta.out()).contains("so-da-beta");
        assertThat(test.out()).doesNotContain("so-da-beta").contains("No campaigns yet");
    }

    @Test
    void anUnknownOrganizationIsAnError() {
        Result result = execute("--org", "nada", "campaign", "list");

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.err()).contains("no organization 'nada'. Run: orgs list");
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

    private record Result(int exitCode, String out, String err) {
    }
}
