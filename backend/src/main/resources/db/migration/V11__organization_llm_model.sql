-- The model an organization's LLM calls go to (ADR 0044). Null means leadhunter.llm.model. The operator
-- picks it from leadhunter.llm.allowed-models with `orgs model`.
alter table organization add column llm_model text;
