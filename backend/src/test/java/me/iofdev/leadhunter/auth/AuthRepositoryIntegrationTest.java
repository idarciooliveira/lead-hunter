package me.iofdev.leadhunter.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import me.iofdev.leadhunter.PostgresTestSupport;
import me.iofdev.leadhunter.api.ApiTestAuth;
import me.iofdev.leadhunter.auth.AuthRepository.Organization;
import me.iofdev.leadhunter.auth.AuthRepository.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;

@EnabledIf("me.iofdev.leadhunter.PostgresTestSupport#databaseAvailable")
class AuthRepositoryIntegrationTest extends PostgresTestSupport {

    @Autowired
    AuthRepository auth;

    private int count(String sql, Object... params) {
        return jdbc.sql(sql).params(params).query(Integer.class).single();
    }

    private void addSession(String userId) {
        jdbc.sql("insert into auth_session (id, user_id, token, expires_at) values ('s1', :u, 't1', now() + interval '1 day')")
                .param("u", userId).update();
    }

    @Test
    void setPasswordChangesTheExistingLoginEvenWhenItsAccountIdIsNotTheUserId() {
        User user = auth.createUser("ana@example.com", "Ana", "old");
        jdbc.sql("update auth_account set account_id = 'legacy' where user_id = :u").param("u", user.id()).update();

        auth.setPassword(user.id(), "new");

        assertThat(count("select count(*) from auth_account where user_id = ? and provider_id = 'credential'", user.id()))
                .isEqualTo(1);
        assertThat(jdbc.sql("select password from auth_account where user_id = :u").param("u", user.id())
                .query(String.class).single()).isEqualTo("new");
    }

    @Test
    void setPasswordAddsALoginForAUserWithoutOneAndEndsTheirSessions() {
        User user = auth.createUser("ana@example.com", "Ana", "old");
        jdbc.sql("delete from auth_account where user_id = :u").param("u", user.id()).update();
        addSession(user.id());

        auth.setPassword(user.id(), "new");

        assertThat(count("select count(*) from auth_account where user_id = ? and password = 'new'", user.id()))
                .isEqualTo(1);
        assertThat(count("select count(*) from auth_session where user_id = ?", user.id())).isZero();
    }

    @Test
    void createUserInAnOrganizationAddsTheMembership() {
        Organization acme = auth.createOrganization("Acme", "acme");

        User user = auth.createUser("ana@example.com", "Ana", "hash", acme.id(), "admin");

        assertThat(count("select count(*) from member where user_id = ? and organization_id = ? and role = 'admin'",
                user.id(), acme.id())).isEqualTo(1);
    }

    @Test
    void createUserLeavesNoUserBehindWhenTheMembershipFails() {
        assertThatThrownBy(() -> auth.createUser("ana@example.com", "Ana", "hash", "missing-org", "member"))
                .isInstanceOf(RuntimeException.class);

        assertThat(count("select count(*) from app_user where email = ?", "ana@example.com")).isZero();
        assertThat(count("select count(*) from auth_account where account_id <> ?", ApiTestAuth.USER_ID)).isZero();
    }
}
