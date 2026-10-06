# Calibration on existing clients

ADR 0003 says a scoring model that does not rank the seed clients high is wrong. This records the first attempt, on 2026-10-06. It changes no weights.

## What was run

A throwaway campaign searched Google Maps in Luanda for `ajabalg`, `kintexia` and `Horizonte Tour Angola`, 2 places per term, with no filters and `qualifyShare: 1.0`. Cost: $0.0152 of Apify credit. The campaign was deleted afterwards.

| Seed client | Found | Reviews | Website | Score |
|---|---|---|---|---|
| ajabalg | AJABALG, real estate builder | 0 | own site | 0 |
| kintexia | no result | | | |
| horizontetourangola | `Agência Horizonte` (horizonte.com) and `go.tours_lda_turismo`, neither confirmed | 0 | own site, none | 0 |

Every place scored 0: the mobile number gives +10 and fewer than 5 reviews gives -15, and the sum clamps at 0.

## What this shows, and what it does not

- Only AJABALG is a sure match. One data point cannot justify a weight change.
- Today's listing is not the lead the clients were before they bought. AJABALG has an own website, probably the one we built. A client's Maps state after the sale says little about the score it should have had before.
- With 0 reviews on every place, the review rules (`NO_WEBSITE_ACTIVE`, `REVIEWS_SWEET_SPOT`) cannot fire. If the seed clients were small and quiet when they signed, the -15 for under 5 reviews may be penalising the exact buyers we want. The data does not settle that.

## The owner's answer on the pre-sale state

Before the sale, none of the seed clients had a website, a social page or a Google Maps listing. The scraper cannot return a business that is not on Maps, so stage 1 cannot score them, and ADR 0003's test (rank the seed clients high) cannot be run. Scoring weights are not the cause.

This leaves a gap. Buyers who start with no online presence are invisible to this pipeline. Finding them needs another source, such as WhatsApp business directories, Instagram or Facebook search, or referrals. That is a scope and data source decision, so it needs its own ADR before any code.

## What is needed to finish

1. Add the three seed clients to the company profile (`company setup`), so they never show up as leads. The profile has 0 clients today.
2. Drop the seed-client test from ADR 0003's calibration plan. Write a new ADR for it, since the accepted one cannot be rewritten.
3. After 50 to 100 marked leads (ADR 0012), compare score ranges against outcomes. That is the calibration that can work.
