package me.iofdev.leadhunter.cli;

import java.io.PrintWriter;

import me.iofdev.leadhunter.auth.AuthRepository;
import me.iofdev.leadhunter.auth.AuthRepository.Organization;
import me.iofdev.leadhunter.auth.AuthRepository.User;
import me.iofdev.leadhunter.auth.AuthRepository.UserRow;
import me.iofdev.leadhunter.auth.PasswordHasher;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/** Accounts for the web app. The password is stored the way Better Auth stores it (ADR 0042). */
@Command(
        name = "users",
        description = "The accounts that can sign in to the web app.",
        mixinStandardHelpOptions = true,
        subcommands = {
                UsersCommand.Add.class,
                UsersCommand.ListUsers.class,
                UsersCommand.Password.class,
                UsersCommand.Remove.class})
class UsersCommand implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    /** Asks twice and returns the password, or fails when the two answers differ or the password is too weak. */
    static String askNewPassword(Prompter prompter) {
        String password = prompter.askSecret("Password (8 to 128 characters)");
        PasswordHasher.requireValid(password);
        if (!password.equals(prompter.askSecret("Repeat the password"))) {
            throw new IllegalArgumentException("the passwords do not match");
        }
        return password;
    }

    static Prompter prompter(PrintWriter out) {
        return Prompter.stdin(out, "input ended before the password was complete. "
                + "In Docker, run it with: docker compose run --rm app users add <email> --name <name>");
    }

    @Command(name = "add", description = "Create an account. Asks for the password without showing it.",
            mixinStandardHelpOptions = true)
    static class Add implements Runnable {

        @Spec
        CommandSpec spec;

        @Parameters(index = "0", paramLabel = "EMAIL", description = "The email they sign in with.")
        String email;

        @Option(names = "--name", required = true, description = "Their name, shown in the app.")
        String name;

        @Option(names = "--org", paramLabel = "SLUG", description = "Add them to this organization.")
        String orgSlug;

        @Option(names = "--role", defaultValue = "member",
                description = "owner, admin or member. Default: ${DEFAULT-VALUE}.")
        String role;

        private final AuthRepository auth;
        private final PasswordHasher hasher;

        Add(AuthRepository auth, PasswordHasher hasher) {
            this.auth = auth;
            this.hasher = hasher;
        }

        @Override
        public void run() {
            PrintWriter out = spec.commandLine().getOut();
            // Check everything that can fail before asking for the password.
            String normalized = AuthRepository.normalizeEmail(email);
            String memberRole = AuthRepository.requireRole(role);
            Organization organization = orgSlug == null ? null : auth.requireOrganization(orgSlug);
            if (auth.findUser(normalized).isPresent()) {
                throw new IllegalArgumentException("a user with email " + normalized + " already exists");
            }

            String password = askNewPassword(prompter(out));
            String hash = hasher.hash(password);
            User user = organization == null
                    ? auth.createUser(normalized, name, hash)
                    : auth.createUser(normalized, name, hash, organization.id(), memberRole);
            out.printf("Created %s (%s). They can sign in with the password.%n", user.email(), user.name());
            if (organization == null) {
                out.println("They belong to no organization yet. Next: members add <org> " + user.email());
                return;
            }
            out.printf("Added to %s as %s.%n", organization.slug(), memberRole);
        }
    }

    @Command(name = "list", description = "List the accounts and their organizations.", mixinStandardHelpOptions = true)
    static class ListUsers implements Runnable {

        @Spec
        CommandSpec spec;

        private final AuthRepository auth;

        ListUsers(AuthRepository auth) {
            this.auth = auth;
        }

        @Override
        public void run() {
            PrintWriter out = spec.commandLine().getOut();
            var users = auth.listUsers();
            if (users.isEmpty()) {
                out.println("No users yet. Run: users add <email> --name <name>");
                return;
            }
            out.printf("%-34s %-22s %-9s %s%n", "EMAIL", "NAME", "VERIFIED", "ORGANIZATIONS");
            for (UserRow user : users) {
                out.printf("%-34s %-22s %-9s %s%n", Format.truncate(user.email(), 34), Format.truncate(user.name(), 22),
                        user.verified() ? "yes" : "no", user.organizations());
            }
        }
    }

    @Command(name = "password", description = "Set a new password. It ends their sessions.",
            mixinStandardHelpOptions = true)
    static class Password implements Runnable {

        @Spec
        CommandSpec spec;

        @Parameters(index = "0", paramLabel = "EMAIL")
        String email;

        private final AuthRepository auth;
        private final PasswordHasher hasher;

        Password(AuthRepository auth, PasswordHasher hasher) {
            this.auth = auth;
            this.hasher = hasher;
        }

        @Override
        public void run() {
            PrintWriter out = spec.commandLine().getOut();
            User user = auth.requireUser(email);
            String password = askNewPassword(prompter(out));
            auth.setPassword(user.id(), hasher.hash(password));
            out.printf("Changed the password of %s. Their sessions ended.%n", user.email());
        }
    }

    @Command(name = "remove", description = "Delete an account with its sessions and memberships.",
            mixinStandardHelpOptions = true)
    static class Remove implements Runnable {

        @Spec
        CommandSpec spec;

        @Parameters(index = "0", paramLabel = "EMAIL")
        String email;

        @Option(names = "--yes", description = "Do not ask for confirmation.")
        boolean yes;

        private final AuthRepository auth;

        Remove(AuthRepository auth) {
            this.auth = auth;
        }

        @Override
        public void run() {
            PrintWriter out = spec.commandLine().getOut();
            User user = auth.requireUser(email);
            if (!yes && !prompter(out).confirm("Delete " + user.email() + " (" + user.name() + ")?")) {
                out.println("Nothing deleted.");
                return;
            }
            auth.removeUser(user.email());
            out.printf("Deleted %s.%n", user.email());
        }
    }
}
