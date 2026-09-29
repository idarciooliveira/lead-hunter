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
                        insert into llm_call (campaign_id, purpose, model, prompt_tokens, completion_tokens,
                                              cost_usd, generation_id, raw_usage)
                        values (:campaignId, :purpose, :model, :promptTokens, :completionTokens,
                                :costUsd, :generationId, cast(:rawUsage as jsonb))
                        """)
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
