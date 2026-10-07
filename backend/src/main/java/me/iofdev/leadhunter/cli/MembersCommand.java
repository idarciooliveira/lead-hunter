package me.iofdev.leadhunter.cli;

import me.iofdev.leadhunter.auth.AuthRepository;
import me.iofdev.leadhunter.auth.AuthRepository.Organization;
import me.iofdev.leadhunter.auth.AuthRepository.User;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/** Who belongs to which organization (ADR 0043). */
@Command(
        name = "members",
        description = "Who belongs to an organization.",
        mixinStandardHelpOptions = true,
        subcommands = MembersCommand.Add.class)
class MembersCommand implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    @Command(name = "add", description = "Add a user to an organization, or change their role.",
            mixinStandardHelpOptions = true)
    static class Add implements Runnable {

        @Spec
        CommandSpec spec;

        @Parameters(index = "0", paramLabel = "ORG", description = "The organization's slug.")
        String orgSlug;

        @Parameters(index = "1", paramLabel = "EMAIL")
        String email;

        @Option(names = "--role", defaultValue = "member",
                description = "owner, admin or member. Default: ${DEFAULT-VALUE}.")
        String role;

        private final AuthRepository auth;

        Add(AuthRepository auth) {
            this.auth = auth;
        }

        @Override
        public void run() {
            Organization organization = auth.requireOrganization(orgSlug);
            User user = auth.requireUser(email);
            boolean added = auth.addMember(organization.id(), user.id(), role);
            spec.commandLine().getOut().printf("%s %s as %s of %s.%n", added ? "Added" : "Changed",
                    user.email(), AuthRepository.requireRole(role), organization.slug());
        }
    }
}
