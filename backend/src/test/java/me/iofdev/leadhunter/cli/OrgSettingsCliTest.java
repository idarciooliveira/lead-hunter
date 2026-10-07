package me.iofdev.leadhunter.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;

import me.iofdev.leadhunter.PostgresTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import picocli.CommandLine;

/** The operator's controls from ADR 0044: a budget and a model for each organization, and the view across all of them. */
@EnabledIf("me.iofdev.leadhunter.PostgresTestSupport#databaseAvailable")
@SpringBootTest(properties = {"leadhunter.cli.enabled=false", "leadhunter.usage.monthly-budget-usd=10",
        "leadhunter.usage.total-monthly-budget-usd=40",
        "leadhunter.llm.model=google/default", "leadhunter.llm.allowed-models=anthropic/haiku,openai/mini"})
class OrgSettingsCliTest extends PostgresTestSupport {

    @Autowired
    SpringCommandFactory factory;

    @BeforeEach
    void twoOrganizations() {
        jdbc.sql("insert into organization (id, name, slug) values ('b-org', 'Beta', 'beta')").update();
    }

    @Test
    void budgetShowsTheDefaultThenTheOperatorsValueThenTheDefaultAgain() {
        assertThat(execute("orgs", "budget", "beta").out()).contains("beta: $10.00 a month (default)");

        assertThat(execute("orgs", "budget", "beta", "25").out()).contains("beta: $25.00 a month (set by the operator)");
        assertThat(execute("orgs", "budget", "test").out()).contains("test: $10.00 a month (default)");

        assertThat(execute("orgs", "budget", "beta", "--reset").out()).contains("beta: $10.00 a month (default)");
    }

    @Test
    void budgetRejectsANegativeAmountAndAnUnknownOrganization() {
        Result negative = execute("orgs", "budget", "beta", "-5");
        assertThat(negative.exitCode()).isEqualTo(1);
        assertThat(negative.err()).contains("the budget cannot be negative");

        Result unknown = execute("orgs", "budget", "nada");
        assertThat(unknown.exitCode()).isEqualTo(1);
        assertThat(unknown.err()).contains("no organization 'nada'");
    }

    @Test
    void modelShowsTheDefaultAndTheAllowedList() {
        Result result = execute("orgs", "model", "beta");

        assertThat(result.out()).contains("beta: google/default (default)")
                .contains("Allowed: google/default, anthropic/haiku, openai/mini");
    }

    @Test
    void modelSetsAnAllowedModelAndResets() {
        assertThat(execute("orgs", "model", "beta", "anthropic/haiku").out()).contains("beta: anthropic/haiku");
        assertThat(execute("orgs", "model", "beta").out()).contains("beta: anthropic/haiku").doesNotContain("(default)");
        assertThat(execute("orgs", "model", "test").out()).contains("test: google/default (default)");

        assertThat(execute("orgs", "model", "beta", "--reset").out()).contains("beta: google/default (default)");
    }

    @Test
    void modelRefusesOneThatIsNotOnTheList() {
        Result result = execute("orgs", "model", "beta", "some/other");

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.err()).contains("'some/other' is not an allowed model")
                .contains("google/default, anthropic/haiku, openai/mini");
        assertThat(execute("orgs", "model", "beta").out()).contains("(default)");
    }

    @Test
    void usageAllOrgsListsEveryOrganizationAgainstItsBudgetAndTheCap() {
        execute("orgs", "budget", "beta", "4");
        jdbc.sql("insert into llm_call (org_id, purpose, model, prompt_tokens, completion_tokens, cost_usd) values ('b-org', 'pitch', 'm', 10, 5, 1)").update();

        Result result = execute("usage", "--all-orgs");

        assertThat(result.exitCode()).isZero();
        assertThat(result.out()).contains("all organizations")
                .containsPattern("beta\\s+\\$1\\.0000\\s+\\$4\\.00\\s+25%")
                .containsPattern("test\\s+\\$0\\.0000\\s+\\$10\\.00\\s+0%")
                .contains("All organizations  $1.0000 of $40.00");
    }

    @Test
    void usageAllOrgsRefusesTheOtherFilters() {
        Result result = execute("usage", "--all-orgs", "--month", "2026-09");

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.err()).contains("--all-orgs shows this month only");
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
