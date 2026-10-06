package me.iofdev.leadhunter.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import me.iofdev.leadhunter.PostgresTestSupport;
import me.iofdev.leadhunter.campaign.Campaign;
import me.iofdev.leadhunter.campaign.CampaignFileParser;
import me.iofdev.leadhunter.campaign.CampaignRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import picocli.CommandLine;

/** {@code leads today} and {@code leads export} (ADR 0041), on real Postgres. */
@EnabledIf("me.iofdev.leadhunter.PostgresTestSupport#databaseAvailable")
class LeadsTodayCliTest extends PostgresTestSupport {

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

    @BeforeEach
    void seed() {
        campaigns.save(parser.parse(CAMPAIGN));
        Campaign campaign = campaigns.findBySlug("clinicas-teste").orElseThrow();
        lead(campaign, "a", "Baixa", 40, "QUALIFIED", "NEW");
        lead(campaign, "b", "Casa \"Boa\", Lda", 80, "QUALIFIED", "NEW");
        lead(campaign, "c", "Já contactada", 90, "QUALIFIED", "CONTACTED");
        lead(campaign, "d", "Abaixo do corte", 95, "BELOW_CUT", "NEW");
    }

    @Test
    void todayAndExport(@TempDir Path dir) throws Exception {
        assertThat(execute("leads", "today")).contains("Casa").contains("Baixa").doesNotContain("Já contactada");

        Path file = dir.resolve("out.csv");
        assertThat(execute("leads", "export", "clinicas-teste", "--stage", "ALL", "--out", file.toString()))
                .contains("Wrote 4 leads");
        assertThat(Files.readString(file)).startsWith("﻿id,campanha").contains("Abaixo do corte");

        assertThat(execute("leads", "export", "nada")).contains("error: no campaign 'nada'");
    }

    private String execute(String... args) {
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        CommandLine commandLine = CliRunner.newCommandLine(factory);
        commandLine.setOut(new PrintWriter(out));
        commandLine.setErr(new PrintWriter(err));
        commandLine.execute(args);
        return out + err.toString();
    }

    private void lead(Campaign campaign, String gid, String name, int score, String stage, String status) {
        long place = jdbc.sql("""
                        insert into place (google_place_id, name, category, address, neighborhood,
                            phone_e164, phone_mobile, website, website_kind, rating, reviews_count, maps_url, raw)
                        values (:gid, :name, 'Clínica', 'Rua 1', 'Talatona',
                            '+244923456789', true, null, 'NONE', 4.3, 10, 'https://maps.example/p', '{}')
                        returning id
                        """)
                .param("gid", gid)
                .param("name", name)
                .query(Long.class)
                .single();
        jdbc.sql("""
                        insert into lead (campaign_id, place_id, stage, status, score, score_breakdown)
                        values (:campaignId, :placeId, :stage, :status, :score, cast('[]' as jsonb))
                        """)
                .param("campaignId", campaign.id())
                .param("placeId", place)
                .param("stage", stage)
                .param("status", status)
                .param("score", score)
                .update();
    }
}
