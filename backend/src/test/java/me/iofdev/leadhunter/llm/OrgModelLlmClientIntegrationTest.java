package me.iofdev.leadhunter.llm;

import static org.assertj.core.api.Assertions.assertThat;

import me.iofdev.leadhunter.PostgresTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;

/** A call goes to its organization's model, reached through the campaign or the organization itself (ADR 0044). */
@EnabledIf("me.iofdev.leadhunter.PostgresTestSupport#databaseAvailable")
class OrgModelLlmClientIntegrationTest extends PostgresTestSupport {

    @Autowired
    OrgModelRepository models;

    private final FakeLlmClient delegate = new FakeLlmClient();
    private OrgModelLlmClient client;
    private long campaign;

    @BeforeEach
    void seed() {
        client = new OrgModelLlmClient(delegate, models);
        campaign = jdbc.sql("""
                        insert into campaign (org_id, slug, name, answers, search)
                        values ('test-org', 'clinicas', 'Clínicas', '{}'::jsonb, '{}'::jsonb) returning id
                        """)
                .query(Long.class).single();
    }

    @Test
    void usesTheDefaultWhenTheOrganizationHasNoModel() {
        client.complete(LlmRequest.text(null, "x").forCampaign(campaign, "pitch"));

        assertThat(delegate.last.model()).isNull();
    }

    @Test
    void usesTheOrganizationsModelForACampaignCall() {
        models.set(ORG, "anthropic/haiku");

        client.complete(LlmRequest.text(null, "x").forCampaign(campaign, "pitch"));

        assertThat(delegate.last.model()).isEqualTo("anthropic/haiku");
    }

    @Test
    void usesTheOrganizationsModelForACallWithoutACampaign() {
        models.set(ORG, "openai/mini");

        client.complete(LlmRequest.text(null, "x").forOrg(ORG.value(), "test"));

        assertThat(delegate.last.model()).isEqualTo("openai/mini");
    }

    @Test
    void aRequestThatNamesAModelKeepsIt() {
        models.set(ORG, "anthropic/haiku");

        client.complete(LlmRequest.text(null, "x").forCampaign(campaign, "pitch").withModel("explicit/model"));

        assertThat(delegate.last.model()).isEqualTo("explicit/model");
    }

    @Test
    void anotherOrganizationsModelDoesNotLeak() {
        jdbc.sql("insert into organization (id, name, slug, llm_model) values ('b-org', 'Beta', 'beta', 'anthropic/haiku')").update();

        client.complete(LlmRequest.text(null, "x").forCampaign(campaign, "pitch"));

        assertThat(delegate.last.model()).isNull();
    }
}
