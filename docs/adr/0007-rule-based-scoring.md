# 0007. Score with explicit rules; the LLM only reads reviews and writes pitches

- Date: 2026-09-28
- Status: Accepted

## Context

A number like "87" means nothing on its own. The owner needs to know why a lead is good before calling. We also want to tune the scoring from real outcomes, which is hard if an LLM invents the number each time.

## Decision

The score is a sum of rules written in Java, clamped to 0 to 100. Each rule that fires adds a line to the breakdown with its points and a human-readable reason. Starting weights, aimed at a software factory:

| Signal | Points |
|---|---|
| No website, but 20+ reviews | +30 |
| Website broken, no HTTPS, not mobile-friendly, or stale | +25 |
| Only a social or link-in-bio page as website | +20 |
| Reviews complain about contact, booking, or waiting | +20 |
| 20 to 300 reviews | +15 |
| Angolan mobile number, likely on WhatsApp | +10 |
| Category in a target sector | +10 |
| Fewer than 5 reviews | -15 |
| Over 1,000 reviews | -20 |
| Chain, bank, telecom, government, multinational, existing client | excluded |

Rules that need the website crawl or reviews run in stage 2. The LLM, Claude Haiku 4.5, classifies review complaints and writes the pitch. It never sets the score.

## Consequences

Scores are explainable and can be tested in unit tests. Weights will be wrong at first. Recorded outcomes are how we fix them, see ADR 0012.
