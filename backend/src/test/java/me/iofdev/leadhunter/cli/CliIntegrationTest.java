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
    void setsUpTheCompanyThenAsksOnlyTheCampaignQuestions(@TempDir Path dir) throws Exception {
        assertThat(executeWithInput(CampaignWizardTest.ANSWERS, "campaign", "new", "--dir", dir.toString()).err())
                .contains("error: no company profile yet. Run: company setup");

        Path companyFile = dir.resolve("company.yml");
        Result company = executeWithInput(CompanyWizardTest.ANSWERS, "company", "setup", "--file", companyFile.toString());
        assertThat(company.exitCode()).as(company.err()).isZero();
        assertThat(company.out())
                .contains("C1/10  Company name")
                .contains("Saved the company profile for Exemplo Software.")
                .contains("warning: clients without a valid phone are matched by name only")
                .contains("Next: campaign new");
        assertThat(execute("company", "update", "-f", companyFile.toString()).out())
                .contains("Updated the company profile for Exemplo Software.");
        assertThat(execute("company", "show").out())
                .contains("Site, 400 mil Kz  (entry offer)")
                .contains("Clients: 2, never shown as leads")
                .contains("Weekly capacity: 40 contacts");

        Result result = executeWithInput(CampaignWizardTest.ANSWERS, "campaign", "new", "--dir", dir.toString());

        assertThat(result.exitCode()).as(result.err()).isZero();
        assertThat(result.out())
                .contains("New campaign for Exemplo Software.")
                .contains("1/11  Sector")
                .doesNotContain("What do you sell")
                .contains("40 of 40 free")
                .contains("warning: the case is from 'clínica dentária', not 'escolas'")
                .contains("Saved campaign 'escolas-em-luanda'")
                .contains("Next: campaign run escolas-em-luanda --dry-run");
        Path file = dir.resolve("escolas-em-luanda.yml");
        assertThat(Files.readString(file)).contains("colégio").contains("service: Site");

        // The written file feeds straight back into `campaign create`.
        assertThat(execute("campaign", "create", "-f", file.toString()).out())
                .contains("Updated campaign 'escolas-em-luanda'");

        // Running it again for the same slug asks first; answering no keeps the saved campaign.
        Result again = executeWithInput(CampaignWizardTest.ANSWERS.replace("Os pais só", "Outro problema") + "n\n",
                "campaign", "new", "--dir", dir.toString());
        assertThat(again.out()).contains("already exists. Replace it?").contains("Nothing saved.");
        assertThat(Files.readString(file)).doesNotContain("Outro problema");
    }

    @Test
    void refusesACampaignForAServiceTheCompanyDoesNotSell(@TempDir Path dir) throws Exception {
        execute("company", "update", "-f", "campaigns/company.yml");
        Path file = dir.resolve("apps.yml");
        Files.writeString(file, Files.readString(Path.of("campaigns", "clinicas-luanda.yml"))
                .replace("service: website standart", "service: Loja online"));

        Result result = execute("campaign", "create", "-f", file.toString());

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.err()).contains("answers.service 'Loja online' is not one of the company's services");
    }

    @Test
    void createsAndPlansTheExampleCampaign() {
        Path file = Path.of("campaigns", "clinicas-luanda.yml");
        assertThat(execute("company", "update").out()).contains("Saved the company profile");

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
        assertThat(execute("campaign", "enrich", "nope").err()).contains("error: no campaign 'nope'");
    }

    @Test
    void enrichesAWaitingLeadAndThenHasNothingLeft() throws Exception {
        com.sun.net.httpserver.HttpServer site =
                com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("localhost", 0), 0);
        byte[] body = "<html><head><meta name=\"viewport\" content=\"width=device-width\"></head><body></body></html>"
                .getBytes(StandardCharsets.UTF_8);
        site.createContext("/", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            try (var out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        site.start();
        try {
            long campaignId = jdbc.sql("""
                    insert into campaign (slug, name, answers, search) values ('dentistas', 'Dentistas', '{}'::jsonb, '{}'::jsonb)
                    returning id
                    """).query(Long.class).single();
            long placeId = jdbc.sql("""
                    insert into place (google_place_id, name, website, website_kind, raw)
                    values ('d1', 'Dentista Sorriso', :website, 'OWN', '{}'::jsonb) returning id
                    """).param("website", "http://localhost:" + site.getAddress().getPort() + "/")
                    .query(Long.class).single();
            jdbc.sql("""
                    insert into lead (campaign_id, place_id, stage, score, score_breakdown)
                    values (:campaignId, :placeId, 'QUALIFIED', 0, '[]'::jsonb)
                    """).param("campaignId", campaignId).param("placeId", placeId).update();

            Result dry = execute("campaign", "enrich", "dentistas", "--dry-run");
            assertThat(dry.exitCode()).as(dry.err()).isZero();
            assertThat(dry.out())
                    .contains("has 1 qualified leads waiting for enrichment")
                    .contains("would enrich up to 1 of them");

            Result result = execute("campaign", "enrich", "dentistas");
            assertThat(result.exitCode()).as(result.err()).isZero();
            assertThat(result.out())
                    .contains("Dentista Sorriso: 0 -> 25 (+25 NO_HTTPS)")
                    .contains("Enriched 1 of 1")
                    .contains("Next: leads list dentistas");

            assertThat(execute("campaign", "enrich", "dentistas").out()).contains("already enriched");
        } finally {
            site.stop(0);
        }
    }

    @Test
    void suggestsCampaignRunWhenDryRunWithNothingWaiting() {
        jdbc.sql("""
                insert into campaign (slug, name, answers, search) values ('vazias', 'Vazias', '{}'::jsonb, '{}'::jsonb)
                """).update();

        Result dry = execute("campaign", "enrich", "vazias", "--dry-run");

        assertThat(dry.exitCode()).as(dry.err()).isZero();
        assertThat(dry.out())
                .contains("Campaign 'vazias' has 0 qualified leads waiting for enrichment. "
                        + "Next: campaign run vazias (to score places and qualify leads)")
                .doesNotContain("would enrich up to 0 of them");
    }

    @Test
    void showsSpendForAMonthAndListsEachRun() {
        jdbc.sql("""
                insert into campaign (slug, name, answers, search) values ('clinicas', 'Clínicas', '{}'::jsonb, '{}'::jsonb)
                """).update();
        jdbc.sql("""
                insert into campaign_run (campaign_id, location, search_terms, max_places, status, places_found, cost_usd, started_at)
                values (1, 'Luanda', '{clínica}', 40, 'SUCCEEDED', 40, 2.5, '2026-09-10T10:00:00Z'),
                       (1, 'Talatona', '{clínica}', 40, 'FAILED', null, 0.5, '2026-09-11T10:00:00Z'),
                       (1, 'Viana', '{clínica}', 40, 'SUCCEEDED', 10, 9, '2026-08-11T10:00:00Z')
                """).update();
        jdbc.sql("""
                insert into llm_call (campaign_id, purpose, model, prompt_tokens, completion_tokens, cost_usd, created_at)
                values (1, 'pitch', 'google/gemma-4-26b-a4b-it', 131500, 22000, 0.0008, '2026-09-12T10:00:00Z'),
                       (null, 'test', 'google/gemma-4-26b-a4b-it', 15, 10, 0.00000771, '2026-09-12T11:00:00Z')
                """).update();

        Result month = execute("usage", "--month", "2026-09");

        assertThat(month.exitCode()).as(month.err()).isZero();
        assertThat(month.out())
                .contains("Usage  ·  2026-09")
                .contains("Apify  $3.0000")
                .contains("Runs          2 (1 failed)")
                .contains("LLM  $0.0008")
                .contains("131.5k in, 22.0k out")
                .contains("Total  $3.0008")
                .contains("30% of $10.00 monthly budget")
                .contains("clinicas")
                .contains("(no campaign)")
                .contains("<$0.0001");
        assertThat(execute("usage", "--month", "2026-08").out()).contains("Apify  $9.0000");
        assertThat(execute("usage", "--campaign", "clinicas").out())
                .contains("Usage  ·  all time, campaign clinicas")
                .doesNotContain("By campaign");

        Result list = execute("usage", "--runs", "--limit", "2");
        assertThat(list.out()).contains("KIND").contains("llm").contains("gemma");
        assertThat(list.out().lines().filter(l -> l.contains("apify") || l.contains(" llm ")).count()).isEqualTo(2);
    }

    @Test
    void reportsBadUsageArguments() {
        assertThat(execute("usage", "--month", "sept").err()).contains("error: --month must look like 2026-09");
        assertThat(execute("usage", "--campaign", "nope").err()).contains("error: no campaign 'nope'");
    }

    @Test
    void showsZerosBeforeAnyRun() {
        Result result = execute("usage");

        assertThat(result.exitCode()).as(result.err()).isZero();
        assertThat(result.out()).contains("Apify  $0.0000").contains("LLM  $0.0000").contains("Total  $0.0000");
        assertThat(execute("usage", "--runs").out()).contains("No runs or calls yet.");
    }
}
