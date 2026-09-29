package me.iofdev.leadhunter.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import me.iofdev.leadhunter.campaign.Campaign;
import me.iofdev.leadhunter.campaign.CampaignChecks;
import me.iofdev.leadhunter.campaign.CampaignFile;
import me.iofdev.leadhunter.campaign.CampaignFileParser;
import me.iofdev.leadhunter.campaign.CampaignRepository;
import me.iofdev.leadhunter.company.CompanyProfile;
import me.iofdev.leadhunter.company.CompanyRepository;
import me.iofdev.leadhunter.maps.ScrapeRequest;
import me.iofdev.leadhunter.pipeline.CampaignRunner;
import me.iofdev.leadhunter.pipeline.RunSummary;
import me.iofdev.leadhunter.pipeline.SearchPlan;
import org.springframework.core.io.ClassPathResource;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(
        name = "campaign",
        description = "Create and run campaigns.",
        mixinStandardHelpOptions = true,
        subcommands = {
                CampaignCommand.New.class,
                CampaignCommand.Template.class,
                CampaignCommand.Create.class,
                CampaignCommand.ListCampaigns.class,
                CampaignCommand.Run.class})
class CampaignCommand implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    static Campaign requireCampaign(CampaignRepository campaigns, String slug) {
        return campaigns.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("no campaign '" + slug + "'. Run: campaign list"));
    }

    @Command(name = "new", description = "Answer the campaign questions in the terminal. Saves the campaign and writes its YAML file.")
    static class New implements Runnable {

        @Spec
        CommandSpec spec;

        @Option(names = "--dir", defaultValue = "campaigns",
                description = "Folder for the YAML file. Default: ${DEFAULT-VALUE}.")
        Path dir;

        @Option(names = "--no-file", description = "Save to the database only, without writing a YAML file.")
        boolean noFile;

        private final CampaignFileParser parser;
        private final CampaignRepository campaigns;
        private final CompanyRepository company;

        New(CampaignFileParser parser, CampaignRepository campaigns, CompanyRepository company) {
            this.parser = parser;
            this.campaigns = campaigns;
            this.company = company;
        }

        @Override
        public void run() {
            PrintWriter out = spec.commandLine().getOut();
            CompanyProfile profile = CompanyCommand.requireCompany(company);
            LocalDate today = LocalDate.now();
            List<Campaign> all = campaigns.findAll();
            Prompter prompter = new Prompter(
                    new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)), out,
                    "input ended before the campaign was complete. "
                            + "In Docker, run it with: docker compose run --rm app campaign new");
            CampaignWizard wizard = new CampaignWizard(prompter, profile,
                    slug -> CampaignChecks.freeCapacity(profile, all, slug, today), today);
            CampaignFile campaign = wizard.run();
            CampaignFileParser.validate(campaign);
            CampaignChecks.requireFits(campaign, profile);
            out.println();
            CompanyCommand.printWarnings(out, CampaignChecks.warnings(campaign, profile, all, today));

            if (campaigns.findBySlug(campaign.slug()).isPresent()
                    && !wizard.confirm("Campaign '" + campaign.slug() + "' already exists. Replace it?")) {
                out.println("Nothing saved.");
                return;
            }
            Path file = dir.resolve(campaign.slug() + ".yml");
            boolean writeFile = !noFile
                    && (!Files.exists(file) || wizard.confirm(file + " already exists. Overwrite it?"));

            campaigns.save(campaign);
            out.printf("Saved campaign '%s'.%n", campaign.slug());
            if (writeFile) {
                try {
                    Files.createDirectories(dir);
                    Files.writeString(file, parser.toYaml(campaign));
                } catch (IOException e) {
                    throw new IllegalStateException("campaign saved, but writing " + file + " failed: " + e.getMessage());
                }
                out.printf("Guardado em %s. Testa sem gastar: campaign run %s --dry-run%n", file, campaign.slug());
            }
            out.printf("Next: campaign run %s --dry-run%n", campaign.slug());
        }
    }

    @Command(name = "template", description = "Print an example campaign file with the campaign questions.")
    static class Template implements Runnable {

        @Spec
        CommandSpec spec;

        @Override
        public void run() {
            try (InputStream in = new ClassPathResource("campaign-template.yml").getInputStream()) {
                spec.commandLine().getOut().print(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                spec.commandLine().getOut().flush();
            } catch (IOException e) {
                throw new IllegalStateException("template missing from the jar", e);
            }
        }
    }

    @Command(name = "create", description = "Save a campaign from a YAML file. Saving the same slug again updates it.")
    static class Create implements Runnable {

        @Spec
        CommandSpec spec;

        @Option(names = {"-f", "--file"}, required = true, description = "Campaign YAML file.")
        Path file;

        private final CampaignFileParser parser;
        private final CampaignRepository campaigns;
        private final CompanyRepository company;

        Create(CampaignFileParser parser, CampaignRepository campaigns, CompanyRepository company) {
            this.parser = parser;
            this.campaigns = campaigns;
            this.company = company;
        }

        @Override
        public void run() {
            String content;
            try {
                content = Files.readString(file);
            } catch (IOException e) {
                throw new IllegalArgumentException("cannot read " + file + ": " + e.getMessage());
            }
            CampaignFile campaign = parser.parse(content);
            CompanyProfile profile = CompanyCommand.requireCompany(company);
            CampaignChecks.requireFits(campaign, profile);
            List<String> warnings = CampaignChecks.warnings(campaign, profile, campaigns.findAll(), LocalDate.now());
            boolean created = campaigns.save(campaign);
            CompanyCommand.printWarnings(spec.commandLine().getOut(), warnings);
            spec.commandLine().getOut().printf("%s campaign '%s'. Next: campaign run %s --dry-run%n",
                    created ? "Created" : "Updated", campaign.slug(), campaign.slug());
        }
    }

    @Command(name = "list", description = "List campaigns and what they have cost so far.")
    static class ListCampaigns implements Runnable {

        @Spec
        CommandSpec spec;

        private final CampaignRepository campaigns;

        ListCampaigns(CampaignRepository campaigns) {
            this.campaigns = campaigns;
        }

        @Override
        public void run() {
            PrintWriter out = spec.commandLine().getOut();
            List<Campaign> all = campaigns.findAll();
            if (all.isEmpty()) {
                out.println("No campaigns yet. Start with: campaign new");
                return;
            }
            out.printf("%-28s %-40s %10s%n", "SLUG", "NAME", "SPENT USD");
            for (Campaign campaign : all) {
                out.printf("%-28s %-40s %10s%n", campaign.slug(), Format.truncate(campaign.name(), 40),
                        campaign.totalCostUsd().setScale(4, java.math.RoundingMode.HALF_UP));
            }
        }
    }

    @Command(name = "run", description = "Search Google Maps for a campaign, filter, score, and rank the results.")
    static class Run implements Runnable {

        @Spec
        CommandSpec spec;

        @Parameters(index = "0", description = "Campaign slug.")
        String slug;

        @Option(names = "--dry-run", description = "Show the searches and the maximum cost without calling the scraper.")
        boolean dryRun;

        @Option(names = "--allow-over-limit", description = "Run even if the plan exceeds leadhunter.apify.max-places-per-run.")
        boolean allowOverLimit;

        private final CampaignRepository campaigns;
        private final CampaignRunner runner;

        Run(CampaignRepository campaigns, CampaignRunner runner) {
            this.campaigns = campaigns;
            this.runner = runner;
        }

        @Override
        public void run() {
            PrintWriter out = spec.commandLine().getOut();
            Campaign campaign = requireCampaign(campaigns, slug);
            SearchPlan plan = runner.plan(campaign);

            if (dryRun) {
                out.printf("Campaign '%s' would start %d scraper runs:%n", campaign.slug(), plan.requests().size());
                for (ScrapeRequest request : plan.requests()) {
                    out.printf("  %s: %s, up to %d places%n", request.location(),
                            String.join(", ", request.terms()), request.maxPlaces());
                }
                out.printf("Up to %d places, about $%s at the configured price per place.%n",
                        plan.maxPlaces(), plan.estimatedMaxUsd().setScale(2, java.math.RoundingMode.HALF_UP));
                out.println("Next: run without --dry-run to start this campaign (will spend Apify credit)");
                return;
            }

            RunSummary summary = runner.run(campaign, allowOverLimit, message -> {
                out.println(message);
                out.flush();
            });
            out.println();
            out.printf("Scraper runs: %d, failed: %d%n", summary.scraperRuns(), summary.failedRuns());
            out.printf("Places found: %d, new to this campaign: %d, excluded this run: %d%n",
                    summary.placesFound(), summary.newLeads(), summary.excludedThisRun());
            out.printf("Campaign totals: %d qualified, %d below the cut, %d excluded%n",
                    summary.totals().qualified(), summary.totals().belowCut(), summary.totals().excluded());
            out.printf("Cost of this run: $%s%n", summary.costUsd());
            if (summary.failedRuns() == summary.scraperRuns()) {
                throw new IllegalStateException("every scraper run failed. See the messages above");
            }
            out.printf("Next: leads list %s%n", campaign.slug());
        }
    }
}
