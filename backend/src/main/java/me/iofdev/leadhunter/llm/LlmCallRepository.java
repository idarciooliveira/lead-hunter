package me.iofdev.leadhunter.llm;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class LlmCallRepository {

    private final JdbcClient jdbc;

    public LlmCallRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void save(LlmRequest request, LlmResponse response) {
        jdbc.sql("""
                        insert into llm_call (org_id, campaign_id, purpose, model, prompt_tokens, completion_tokens,
                                              cost_usd, generation_id, raw_usage)
                        values (coalesce((select org_id from campaign where id = :campaignId), :orgId), :campaignId, :purpose, :model, :promptTokens, :completionTokens,
                                :costUsd, :generationId, cast(:rawUsage as jsonb))
                        """)
                .param("orgId", request.orgId())
                .param("campaignId", request.campaignId())
                .param("purpose", request.purpose())
                .param("model", response.model())
                .param("promptTokens", response.promptTokens())
                .param("completionTokens", response.completionTokens())
                .param("costUsd", response.costUsd())
                .param("generationId", response.generationId())
                .param("rawUsage", response.rawUsage())
                .update();
    }
}
