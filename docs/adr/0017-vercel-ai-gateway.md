# 0017. Call LLMs through the Vercel AI Gateway; test with Gemma 4 26B A4B

- Date: 2026-09-28
- Status: Accepted
- Supersedes: [0014](0014-claude-haiku.md)

## Context

ADR 0014 chose Claude Haiku 4.5 through the Anthropic API. The owner wants a single gateway instead of one provider, so models can be compared and swapped without code changes. The Vercel AI Gateway lists models from many providers under `provider/model` ids and offers an OpenAI-compatible API.

For testing, the owner picked `google/gemma-4-26b-a4b-it`: Google's open-weight mixture-of-experts model with 26B parameters and about 4B active per token. The gateway lists it with structured JSON output and support for over 140 languages, including Portuguese.

## Decision

- All LLM calls go through the Vercel AI Gateway's OpenAI-compatible endpoint, `https://ai-gateway.vercel.sh/v1/chat/completions`, authenticated with `AI_GATEWAY_API_KEY`.
- The code depends on a small `LlmClient` interface. `OpenAiCompatibleLlmClient` implements it with Spring's `RestClient`, with no provider SDK.
- The model id comes from `LEADHUNTER_LLM_MODEL`, default `google/gemma-4-26b-a4b-it`. The base URL comes from `LEADHUNTER_LLM_BASE_URL`, so any OpenAI-compatible endpoint works.
- Gemma 4 26B A4B is for testing. Before stage 2 pitches go into daily use, we compare it with at least one larger model on the same 20 leads and record the choice in a new ADR.
- ADR 0007 still holds: the LLM reads reviews and writes pitches and never sets the score. Where 0007 names Claude Haiku 4.5, read "the model configured here".
- `AI_GATEWAY_API_KEY` replaces `ANTHROPIC_API_KEY` wherever ADR 0011 and ADR 0016 mention it.
- `lead-hunter llm test` sends one prompt and prints the answer, latency, and token usage, to check the key and the model.

## Consequences

Switching models is an environment variable change. One key and one bill cover every provider. We lose provider-specific features that the OpenAI-compatible format doesn't carry, such as Anthropic prompt caching. Costs depend on the chosen model's gateway price, which we log through token usage, so the $10 budget in ADR 0006 needs rechecking once the final model is chosen. The gateway is one more service between us and the model.
