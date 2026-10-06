package me.iofdev.leadhunter.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import me.iofdev.leadhunter.PostgresTestSupport;
import me.iofdev.leadhunter.auth.PasswordHasher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import picocli.CommandLine;

/** {@code users}, {@code orgs} and {@code members} write the tables Better Auth reads (ADR 0042, 0043). */
@EnabledIf("me.iofdev.leadhunter.PostgresTestSupport#databaseAvailable")
class AuthCliTest extends PostgresTestSupport {

    private static final InputStream REAL_STDIN = System.in;

    @Autowired
    SpringCommandFactory factory;
    @Autowired
    PasswordHasher hasher;

    @AfterEach
    void restoreStdin() {
        System.setIn(REAL_STDIN);
    }

    @Test
    void createsAUserWhoseHashBetterAuthCanCheck() {
        execute("orgs", "add", "Clínica Sorriso");

        var added = typing("secret-senha\nsecret-senha\n", "users", "add", "Ana@Acme.AO", "--name", "Ana", "--org",
                "clinica-sorriso", "--role", "admin");

        assertThat(added.err()).isEmpty();
        assertThat(added.out()).contains("Created ana@acme.ao (Ana)").contains("Added to clinica-sorriso as admin.");
        String hash = jdbc.sql("""
                        select a.password from auth_account a join app_user u on u.id = a.user_id
                        where u.email = 'ana@acme.ao' and a.provider_id = 'credential' and a.account_id = u.id
                        """).query(String.class).single();
        assertThat(hasher.verify(hash, "secret-senha")).isTrue();
        assertThat(jdbc.sql("select email_verified from app_user where email = 'ana@acme.ao'")
                .query(Boolean.class).single()).isTrue();
        assertThat(jdbc.sql("select role from member").query(String.class).single()).isEqualTo("admin");
        assertThat(jdbc.sql("select id from app_user").query(String.class).single()).hasSize(32);
    }

    @Test
    void refusesWithoutAskingForAPasswordWhenTheInputIsWrong() {
        typing("secret-senha\nsecret-senha\n", "users", "add", "ana@acme.ao", "--name", "Ana");

        var duplicate = execute("users", "add", "ANA@acme.ao", "--name", "Outra");
        assertThat(duplicate.exitCode()).isOne();
        assertThat(duplicate.err()).contains("error: a user with email ana@acme.ao already exists");

        var noOrg = execute("users", "add", "bia@acme.ao", "--name", "Bia", "--org", "nada");
        assertThat(noOrg.err()).contains("error: no organization 'nada'. Run: orgs list");

        var badRole = execute("users", "add", "bia@acme.ao", "--name", "Bia", "--role", "boss");
        assertThat(badRole.err()).contains("role must be one of owner, admin, member");

        var badEmail = execute("users", "add", "not-an-email", "--name", "Bia");
        assertThat(badEmail.err()).contains("'not-an-email' is not an email address");
    }

    @Test
    void checksThePasswordTwice() {
        var mismatch = typing("longpassword1\nlongpassword2\n", "users", "add", "ana@acme.ao", "--name", "Ana");
        assertThat(mismatch.err()).contains("error: the passwords do not match");

        var weak = typing("short\nshort\n", "users", "add", "ana@acme.ao", "--name", "Ana");
        assertThat(weak.err()).contains("the password must have from 8 to 128 characters");

        assertThat(jdbc.sql("select count(*) from app_user").query(Long.class).single()).isZero();
    }

    @Test
    void changesAPasswordAndEndsTheSessions() {
        typing("secret-senha\nsecret-senha\n", "users", "add", "ana@acme.ao", "--name", "Ana");
        String userId = jdbc.sql("select id from app_user").query(String.class).single();
        jdbc.sql("""
                        insert into auth_session (id, user_id, token, expires_at)
                        values ('s1', :userId, 't1', now() + interval '1 day')
                        """).param("userId", userId).update();

        var changed = typing("nova-senha-1\nnova-senha-1\n", "users", "password", "ana@acme.ao");

        assertThat(changed.out()).contains("Changed the password of ana@acme.ao");
        String hash = jdbc.sql("select password from auth_account").query(String.class).single();
        assertThat(hasher.verify(hash, "nova-senha-1")).isTrue();
        assertThat(jdbc.sql("select count(*) from auth_session").query(Long.class).single()).isZero();
    }

    @Test
    void givesAMagicLinkOnlyUserAPassword() {
        jdbc.sql("insert into app_user (id, name, email) values ('u1', 'Bia', 'bia@acme.ao')").update();

        typing("segunda-senha\nsegunda-senha\n", "users", "password", "bia@acme.ao");

        assertThat(hasher.verify(jdbc.sql("select password from auth_account where user_id = 'u1'")
                .query(String.class).single(), "segunda-senha")).isTrue();
    }

    @Test
    void listsAndRemovesUsersWithTheirMemberships() {
        execute("orgs", "add", "Acme", "--slug", "acme");
        typing("secret-senha\nsecret-senha\n", "users", "add", "ana@acme.ao", "--name", "Ana", "--org", "acme");

        var list = execute("users", "list");
        assertThat(list.out()).contains("ana@acme.ao").contains("Ana").contains("yes").contains("acme (member)");

        var declined = typing("n\n", "users", "remove", "ana@acme.ao");
        assertThat(declined.out()).contains("Nothing deleted.");

        var removed = execute("users", "remove", "ana@acme.ao", "--yes");
        assertThat(removed.out()).contains("Deleted ana@acme.ao.");
        assertThat(jdbc.sql("select count(*) from app_user").query(Long.class).single()).isZero();
        assertThat(jdbc.sql("select count(*) from member").query(Long.class).single()).isZero();
        assertThat(jdbc.sql("select count(*) from auth_account").query(Long.class).single()).isZero();
        assertThat(execute("users", "list").out()).contains("No users yet");
    }

    @Test
    void createsOrganizationsAndMembers() {
        assertThat(execute("orgs", "list").out()).contains("No organizations yet");

        var added = execute("orgs", "add", "Lojas do Zango");
        assertThat(added.out()).contains("Created organization Lojas do Zango (lojas-do-zango).")
                .contains("--org lojas-do-zango");
        assertThat(execute("orgs", "add", "Outra", "--slug", "lojas-do-zango").err())
                .contains("an organization with slug 'lojas-do-zango' already exists");
        assertThat(execute("orgs", "add", "Maiúsculas", "--slug", "Not A Slug").err())
                .contains("slug must be lowercase letters, digits and dashes");

        typing("secret-senha\nsecret-senha\n", "users", "add", "ana@acme.ao", "--name", "Ana");
        assertThat(execute("members", "add", "lojas-do-zango", "ana@acme.ao").out())
                .contains("Added ana@acme.ao as member of lojas-do-zango.");
        assertThat(execute("members", "add", "lojas-do-zango", "ana@acme.ao", "--role", "owner").out())
                .contains("Changed ana@acme.ao as owner of lojas-do-zango.");
        assertThat(execute("members", "add", "nada", "ana@acme.ao").err()).contains("no organization 'nada'");
        assertThat(execute("members", "add", "lojas-do-zango", "ninguem@acme.ao").err())
                .contains("no user with email ninguem@acme.ao");

        assertThat(execute("orgs", "list").out()).contains("lojas-do-zango").contains("Lojas do Zango");
        assertThat(execute("users", "list").out()).contains("lojas-do-zango (owner)");
    }

    private record Result(int exitCode, String out, String err) {
    }

    private Result execute(String... args) {
        return typing("", args);
    }

    /** Runs the command with {@code stdin} as the terminal input, where passwords are typed. */
    private Result typing(String stdin, String... args) {
        System.setIn(new ByteArrayInputStream(stdin.getBytes(StandardCharsets.UTF_8)));
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        CommandLine commandLine = CliRunner.newCommandLine(factory);
        commandLine.setOut(new PrintWriter(out));
        commandLine.setErr(new PrintWriter(err));
        int exitCode = commandLine.execute(args);
        return new Result(exitCode, out.toString(), err.toString());
    }
}
