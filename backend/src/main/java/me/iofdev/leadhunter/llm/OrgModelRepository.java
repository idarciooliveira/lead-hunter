package me.iofdev.leadhunter.llm;

import java.util.Optional;

import me.iofdev.leadhunter.auth.OrgId;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** The model each organization's calls go to (ADR 0044). */
@Repository
public class OrgModelRepository {

    private final JdbcClient jdbc;

    public OrgModelRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** The model of the organization this call belongs to, through its campaign or its own org id. Empty when none is set. */
    public Optional<String> modelFor(LlmRequest request) {
        return jdbc.sql("""
                        select llm_model from organization
                        where id = coalesce((select org_id from campaign where id = :campaignId), :orgId)
                        """)
                .param("campaignId", request.campaignId())
                .param("orgId", request.orgId())
                .query(String.class)
                .optional();
    }

    public Optional<String> find(OrgId orgId) {
        return jdbc.sql("select llm_model from organization where id = :id")
                .param("id", orgId.value())
                .query(String.class)
                .optional();
    }

    /** A null {@code model} goes back to the default. */
    public void set(OrgId orgId, String model) {
        jdbc.sql("update organization set llm_model = :model where id = :id")
                .param("id", orgId.value())
                .param("model", model)
                .update();
    }
}
