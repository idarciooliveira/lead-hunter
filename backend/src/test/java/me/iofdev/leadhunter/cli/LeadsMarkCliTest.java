package me.iofdev.leadhunter.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;

import me.iofdev.leadhunter.PostgresTestSupport;
import me.iofdev.leadhunter.campaign.Campaign;
import me.iofdev.leadhunter.campaign.CampaignFileParser;
import me.iofdev.leadhunter.campaign.CampaignRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import picocli.CommandLine;

/**
 * {@code leads mark} is a thin adapter over {@code LeadRepository.updateOutcome},
 * the same method {@code PATCH /api/leads/{id}} uses, so the CLI and the API
 * never drift (ADR 0012, 0020).
 */
@EnabledIf("me.iofdev.leadhunter.PostgresTestSupport#databaseAvailable")
class LeadsMarkCliTest extends PostgresTestSupport {

    private static final String CAMPAIGN = """
            slug: clinicas-teste
            name: Clínicas teste
            answers: {sector: clínicas, problem: marcações só por telefone, service: Site}
            search:
              terms: [clínica]
              locations: [Talatona]
            """;

    @Autowired
    SpringCommandFactory factory;
    @Autowired
    CampaignRepository campaigns;
    @Autowired
    CampaignFileParser parser;

    long leadId;

    @BeforeEach
    void seed() {
        campaigns.save(parser.parse(CAMPAIGN));
        Campaign campaign = campaigns.findBySlug("clinicas-teste").orElseThrow();
        long place = jdbc.sql("""
                        insert into place (google_place_id, name, category, address, neighborhood,
                            phone_e164, phone_mobile, website, website_kind, rating, reviews_count, maps_url, raw)
                        values ('p1', 'Clínica Sorriso', 'Clínica', 'Rua 1', 'Talatona',
                            '+244923456789', true, null, 'NONE', 4.3, 142, 'https://maps.example/p1', '{}')
                        returning id
                        """)
                .query(Long.class)
                .single();
        leadId = jdbc.sql("""
                        insert into lead (campaign_id, place_id, stage, score, score_breakdown)
                        values (:campaignId, :placeId, 'QUALIFIED', 65, cast('[]' as jsonb))
                        returning id
                        """)
                .param("campaignId", campaign.id())
                .param("placeId", place)
                .query(Long.class)
                .single();
    }

    @Test
    void marksOutcomesWithTheSameRulesAsTheApi() {
        assertThat(execute("leads", "mark", String.valueOf(leadId), "--status", "INTERESTED").out())
                .contains("Marked lead " + leadId + " 'Clínica Sorriso' as INTERESTED.");

        var lost = execute("leads", "mark", String.valueOf(leadId), "--status", "LOST");
        assertThat(lost.exitCode()).isOne();
        assertThat(lost.err()).contains("error: marking lead " + leadId + " LOST needs a lost reason");

        assertThat(execute("leads", "mark", String.valueOf(leadId),
                "--status", "LOST", "--lost-reason", "NOT_NOW", "--note", "falar em marco").out())
                .contains("as LOST (NOT_NOW)");

        var back = execute("leads", "mark", String.valueOf(leadId), "--status", "NEW");
        assertThat(back.exitCode()).isOne();
        assertThat(back.err()).contains("would hide the contact history");

        var unknown = execute("leads", "mark", "999999", "--status", "CONTACTED");
        assertThat(unknown.exitCode()).isOne();
        assertThat(unknown.err()).contains("error: no lead with id 999999");
    }

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
}
