package me.iofdev.leadhunter.company;

import java.util.Optional;

import me.iofdev.leadhunter.auth.OrgId;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** One company profile per organization. See ADR 0019 and ADR 0043. */
@Repository
public class CompanyRepository {

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public CompanyRepository(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /** Inserts or replaces the profile. Returns true when there was none before. */
    public boolean save(OrgId orgId, CompanyProfile profile) {
        return jdbc.sql("""
                        insert into company (org_id, profile) values (:orgId, cast(:profile as jsonb))
                        on conflict (org_id) do update set profile = excluded.profile, updated_at = now()
                        returning (xmax = 0) as inserted
                        """)
                .param("orgId", orgId.value())
                .param("profile", json.writeValueAsString(profile))
                .query(Boolean.class)
                .single();
    }

    public Optional<CompanyProfile> find(OrgId orgId) {
        return jdbc.sql("select profile from company where org_id = :orgId")
                .param("orgId", orgId.value())
                .query((rs, row) -> json.readValue(rs.getString("profile"), CompanyProfile.class))
                .optional();
    }

    /** Contacts per day for the today queue: the profile's, or the default one before a profile exists. */
    public int dailyQueueSize(OrgId orgId) {
        return find(orgId).map(CompanyProfile::dailyQueueSize)
                .orElse(Math.max(1, CompanyProfile.DEFAULT_WEEKLY_CAPACITY / 5));
    }
}
