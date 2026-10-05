package me.iofdev.leadhunter.cli;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Optional;

import me.iofdev.leadhunter.company.CompanyProfile;
import me.iofdev.leadhunter.company.CompanyProfileParser;
import me.iofdev.leadhunter.company.CompanyRepository;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(
        name = "company",
        description = "Your company profile, shared by every campaign.",
        mixinStandardHelpOptions = true,
        subcommands = {
                CompanyCommand.Setup.class,
                CompanyCommand.Show.class,
                CompanyCommand.Update.class,
                CompanyCommand.Template.class})
class CompanyCommand implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    static CompanyProfile requireCompany(CompanyRepository company) {
        return company.find().orElseThrow(() -> new IllegalStateException(
                "no company profile yet. Run: company setup, or company update -f <file>"));
    }

    @Command(name = "setup", description = "Answer the company questions. Run it again to change answers.")
    static class Setup implements Runnable {

        @Spec
        CommandSpec spec;

        private final CompanyRepository company;

        Setup(CompanyRepository company) {
            this.company = company;
        }

        @Override
        public void run() {
            PrintWriter out = spec.commandLine().getOut();
            Prompter prompter = Prompter.stdin(out,
                    "input ended before the company profile was complete. "
                            + "In Docker, run it with: docker compose run --rm app company setup");
            Optional<CompanyProfile> existing = company.find();
            CompanyProfile profile = new CompanyWizard(prompter, existing).run();
            CompanyProfileParser.validate(profile);
            out.println();

            company.save(profile);
            out.printf("Saved the company profile for %s.%n", profile.name());
            Format.printWarnings(out, CompanyProfileParser.warnings(profile));
            out.println("Next: campaign new");
        }
    }

    @Command(name = "show", description = "Print the saved company profile.")
    static class Show implements Runnable {

        @Spec
        CommandSpec spec;

        private final CompanyRepository company;

        Show(CompanyRepository company) {
            this.company = company;
        }

        @Override
        public void run() {
            PrintWriter out = spec.commandLine().getOut();
            CompanyProfile profile = requireCompany(company);
            out.println(profile.name());
            out.println(profile.intro());
            out.println();
            out.println("Services:");
            for (CompanyProfile.Service s : profile.services()) {
                out.printf("  %s, %s%s%s%n", s.name(), s.price(),
                        s.deliveryTime() == null ? "" : ", " + s.deliveryTime(),
                        s.name().equals(profile.entryOffer()) ? "  (entry offer)" : "");
            }
            out.println("Area: " + String.join(", ", profile.area()));
            out.printf("Clients: %d, never shown as leads%n", profile.clients().size());
            out.println("Cases:");
            if (profile.cases().isEmpty()) {
                out.println("  none. Pitches will make no claims");
            }
            profile.cases().forEach(c -> out.println("  " + c.label()));
            out.printf("Objections with answers: %d%n", profile.objections().size());
            out.printf("Weekly capacity: %d contacts%n", profile.weeklyCapacity());
            CompanyProfile.QuarterTarget target = profile.quarterTarget();
            if (target != null) {
                out.printf("Quarter target: %s new clients, %s Kz%n", Format.orDash(str(target.newClients())),
                        Format.orDash(str(target.revenueKz())));
            }
            Format.printWarnings(out, CompanyProfileParser.warnings(profile));
        }

        private static String str(Object value) {
            return value == null ? null : value.toString();
        }
    }

    @Command(name = "update", description = "Save the company profile from a YAML file.")
    static class Update implements Runnable {

        @Spec
        CommandSpec spec;

        @Option(names = {"-f", "--file"}, required = true, description = "Company YAML file.")
        Path file;

        private final CompanyProfileParser parser;
        private final CompanyRepository company;

        Update(CompanyProfileParser parser, CompanyRepository company) {
            this.parser = parser;
            this.company = company;
        }

        @Override
        public void run() {
            String content = CliFiles.read(file);
            CompanyProfile profile = parser.parse(content);
            boolean created = company.save(profile);
            PrintWriter out = spec.commandLine().getOut();
            out.printf("%s the company profile for %s.%n", created ? "Saved" : "Updated", profile.name());
            Format.printWarnings(out, CompanyProfileParser.warnings(profile));
        }
    }

    @Command(name = "template", description = "Print an example company file.")
    static class Template implements Runnable {

        @Spec
        CommandSpec spec;

        @Override
        public void run() {
            CliFiles.printResource(spec.commandLine().getOut(), "company-template.yml");
        }
    }
}
