package me.iofdev.leadhunter.auth;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Users, organizations and who belongs to which, in the tables Better Auth also reads (ADR 0042, 0043). */
@Repository
public class AuthRepository {

    public static final List<String> ROLES = List.of("owner", "admin", "member");

    private static final Pattern SLUG = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    private static final String ID_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public record User(String id, String name, String email) {
    }

    /** {@code organizations} reads like {@code acme (owner), beta (member)}. */
    public record UserRow(String name, String email, boolean verified, String organizations) {
    }

    public record Organization(String id, String name, String slug) {
    }

    public record OrganizationRow(String name, String slug, long members) {
    }

    private final JdbcClient jdbc;
    private final SecureRandom random = new SecureRandom();

    public AuthRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Emails are stored in lower case, as Better Auth does, so a later web sign-in finds the same account. */
    public static String normalizeEmail(String email) {
        String value = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (!value.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new IllegalArgumentException("'" + email + "' is not an email address");
        }
        return value;
    }

    public static String requireRole(String role) {
        String value = role == null ? "" : role.trim().toLowerCase(Locale.ROOT);
        if (!ROLES.contains(value)) {
            throw new IllegalArgumentException("role must be one of " + String.join(", ", ROLES) + ", got '" + role + "'");
        }
        return value;
    }

    /** Creates the user as {@link #createUser(String, String, String)} does and adds them to the organization, all or nothing. */
    @Transactional
    public User createUser(String email, String name, String passwordHash, String organizationId, String role) {
        User user = createUser(email, name, passwordHash);
        addMember(organizationId, user.id(), role);
        return user;
    }

    /** Creates a verified user who signs in with a password: one row in each of {@code app_user} and {@code auth_account}. */
    public User createUser(String email, String name, String passwordHash) {
        String normalized = normalizeEmail(email);
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("a user needs a name");
        }
        String id = newId();
        try {
            // One statement, so a user never exists without the account that lets them sign in.
            jdbc.sql("""
                            with created as (
                                insert into app_user (id, name, email, email_verified) values (:id, :name, :email, true)
                            )
                            insert into auth_account (id, user_id, account_id, provider_id, password)
                            values (:accountId, :id, :id, 'credential', :hash)
                            """)
                    .param("id", id)
                    .param("name", name.trim())
                    .param("email", normalized)
                    .param("accountId", newId())
                    .param("hash", passwordHash)
                    .update();
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("a user with email " + normalized + " already exists");
        }
        return new User(id, name.trim(), normalized);
    }

    public Optional<User> findUser(String email) {
        return jdbc.sql("select id, name, email from app_user where email = :email")
                .param("email", normalizeEmail(email))
                .query((rs, row) -> new User(rs.getString("id"), rs.getString("name"), rs.getString("email")))
                .optional();
    }

    public User requireUser(String email) {
        return findUser(email).orElseThrow(() -> new IllegalArgumentException(
                "no user with email " + normalizeEmail(email) + ". Run: users list"));
    }

    public List<UserRow> listUsers() {
        return jdbc.sql("""
                        select u.name, u.email, u.email_verified,
                               coalesce(string_agg(o.slug || ' (' || m.role || ')', ', ' order by o.slug), '') as organizations
                        from app_user u
                        left join member m on m.user_id = u.id
                        left join organization o on o.id = m.organization_id
                        group by u.id
                        order by u.created_at, u.email
                        """)
                .query((rs, row) -> new UserRow(rs.getString("name"), rs.getString("email"),
                        rs.getBoolean("email_verified"), rs.getString("organizations")))
                .list();
    }

    /** Sets the password of a user, adding a password login when they only used magic links so far. */
    @Transactional
    public void setPassword(String userId, String passwordHash) {
        int updated = jdbc.sql("""
                        update auth_account set password = :hash, updated_at = now()
                        where user_id = :userId and provider_id = 'credential'
                        """)
                .param("userId", userId)
                .param("hash", passwordHash)
                .update();
        if (updated == 0) {
            jdbc.sql("""
                            insert into auth_account (id, user_id, account_id, provider_id, password)
                            values (:id, :userId, :userId, 'credential', :hash)
                            """)
                    .param("id", newId())
                    .param("userId", userId)
                    .param("hash", passwordHash)
                    .update();
        }
        // A password change ends every session, like a password reset on the web.
        jdbc.sql("delete from auth_session where user_id = :userId").param("userId", userId).update();
    }

    /** Deletes the user with their sessions, accounts and memberships. Returns false when there was none. */
    public boolean removeUser(String email) {
        return jdbc.sql("delete from app_user where email = :email").param("email", normalizeEmail(email)).update() > 0;
    }

    public Organization createOrganization(String name, String slug) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("an organization needs a name");
        }
        if (slug == null || !SLUG.matcher(slug).matches()) {
            throw new IllegalArgumentException("slug must be lowercase letters, digits and dashes, like acme-lda");
        }
        String id = newId();
        try {
            jdbc.sql("insert into organization (id, name, slug) values (:id, :name, :slug)")
                    .param("id", id).param("name", name.trim()).param("slug", slug).update();
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("an organization with slug '" + slug + "' already exists");
        }
        return new Organization(id, name.trim(), slug);
    }

    public Optional<Organization> findOrganization(String slug) {
        return jdbc.sql("select id, name, slug from organization where slug = :slug")
                .param("slug", slug)
                .query((rs, row) -> new Organization(rs.getString("id"), rs.getString("name"), rs.getString("slug")))
                .optional();
    }

    public Organization requireOrganization(String slug) {
        return findOrganization(slug).orElseThrow(() -> new IllegalArgumentException(
                "no organization '" + slug + "'. Run: orgs list"));
    }

    public List<OrganizationRow> listOrganizations() {
        return jdbc.sql("""
                        select o.name, o.slug, count(m.id) as members
                        from organization o left join member m on m.organization_id = o.id
                        group by o.id order by o.created_at, o.slug
                        """)
                .query((rs, row) -> new OrganizationRow(rs.getString("name"), rs.getString("slug"), rs.getLong("members")))
                .list();
    }

    /** Adds the user to the organization, or changes their role when they already belong. Returns true when new. */
    public boolean addMember(String organizationId, String userId, String role) {
        return jdbc.sql("""
                        insert into member (id, organization_id, user_id, role) values (:id, :organizationId, :userId, :role)
                        on conflict (organization_id, user_id) do update set role = excluded.role
                        returning (xmax = 0) as inserted
                        """)
                .param("id", newId())
                .param("organizationId", organizationId)
                .param("userId", userId)
                .param("role", requireRole(role))
                .query(Boolean.class)
                .single();
    }

    /** Same kind of id as Better Auth makes: 32 letters and digits. */
    private String newId() {
        StringBuilder id = new StringBuilder(32);
        for (int i = 0; i < 32; i++) {
            id.append(ID_CHARS.charAt(random.nextInt(ID_CHARS.length())));
        }
        return id.toString();
    }
}
