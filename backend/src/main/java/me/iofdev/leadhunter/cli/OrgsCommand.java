package me.iofdev.leadhunter.cli;

import java.io.PrintWriter;

import me.iofdev.leadhunter.auth.AuthRepository;
import me.iofdev.leadhunter.auth.AuthRepository.Organization;
import me.iofdev.leadhunter.auth.AuthRepository.OrganizationRow;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/** Organizations are the tenants (ADR 0043). */
@Command(
        name = "orgs",
        description = "The organizations that own companies and campaigns.",
        mixinStandardHelpOptions = true,
        subcommands = {OrgsCommand.Add.class, OrgsCommand.ListOrgs.class})
class OrgsCommand implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    @Command(name = "add", description = "Create an organization.", mixinStandardHelpOptions = true)
    static class Add implements Runnable {

        @Spec
        CommandSpec spec;

        @Parameters(index = "0", paramLabel = "NAME", description = "The company name, like \"Clínica Sorriso\".")
        String name;

        @Option(names = "--slug", paramLabel = "SLUG", description = "Short id for commands. Default: made from the name.")
        String slug;

        private final AuthRepository auth;

        Add(AuthRepository auth) {
            this.auth = auth;
        }

        @Override
        public void run() {
            String chosen = slug != null ? slug : CampaignWizard.slugify(name);
            Organization organization = auth.createOrganization(name, chosen);
            PrintWriter out = spec.commandLine().getOut();
            out.printf("Created organization %s (%s).%n", organization.name(), organization.slug());
            out.println("Next: users add <email> --name <name> --org " + organization.slug());
        }
    }

    @Command(name = "list", description = "List the organizations.", mixinStandardHelpOptions = true)
    static class ListOrgs implements Runnable {

        @Spec
        CommandSpec spec;

        private final AuthRepository auth;

        ListOrgs(AuthRepository auth) {
            this.auth = auth;
        }

        @Override
        public void run() {
            PrintWriter out = spec.commandLine().getOut();
            var organizations = auth.listOrganizations();
            if (organizations.isEmpty()) {
                out.println("No organizations yet. Run: orgs add <name>");
                return;
            }
            out.printf("%-24s %-30s %s%n", "SLUG", "NAME", "MEMBERS");
            for (OrganizationRow organization : organizations) {
                out.printf("%-24s %-30s %d%n", Format.truncate(organization.slug(), 24),
                        Format.truncate(organization.name(), 30), organization.members());
            }
        }
    }
}
