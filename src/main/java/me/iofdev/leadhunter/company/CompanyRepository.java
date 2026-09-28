package me.iofdev.leadhunter.company;

import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** The single company row. See ADR 0019. */
@Repository
public class CompanyRepository {

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public CompanyRepository(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /** Inserts or replaces the profile. Returns true when there was none before. */
    public boolean save(CompanyProfile profile) {
        return jdbc.sql("""
                        insert into company (id, profile) values (1, cast(:profile as jsonb))
                        on conflict (id) do update set profile = excluded.profile, updated_at = now()
                        returning (xmax = 0) as inserted
                        """)
                .param("profile", json.writeValueAsString(profile))
                .query(Boolean.class)
                .single();
    }

    public Optional<CompanyProfile> find() {
        return jdbc.sql("select profile from company where id = 1")
                .query((rs, row) -> json.readValue(rs.getString("profile"), CompanyProfile.class))
                .optional();
    }
}
