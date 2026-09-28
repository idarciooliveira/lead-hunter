# 0019. Split the questions into a company profile and campaign questions

- Date: 2026-09-28
- Status: Accepted
- Amends: [0013](0013-campaigns-as-yaml.md), [0018](0018-campaign-wizard.md)

## Context

ADR 0013 put 10 questions in every campaign. Half of them describe the company, not the campaign: the offer and its price, the area, current clients, proof, and weekly capacity. Every new campaign asked them again. Current clients were copied into each campaign's `excludeNames` by hand, so a new client missed in one file would get a prospecting message. Weekly capacity was per campaign, so two campaigns could each plan 35 contacts a week for one person.

Most answers were also free prose that only a future pitch could read. Stage 1 filters and scores on the Maps fields alone, so an answer like "no website or only Instagram" changed nothing about which places got through.

We reviewed the questions in three rounds with a reviewer playing a sales qualification specialist. The rounds, scores, and what we took or rejected are in [docs/question-review.md](../question-review.md).

## Decision

**Company profile.** One row in a new `company` table, as jsonb, mirrored in `campaigns/company.yml`. `company setup` asks the questions, and running it again offers the saved answers as defaults. `company update -f` saves the file, `company show` prints it, `company template` prints an example. The profile holds:

- name and a one-sentence introduction;
- services, each with a price range and an optional delivery time, and which one is the entry offer;
- the area served in person, the default campaign locations;
- current clients with name and phone;
- cases with a sector, problem, what was built, a result that must contain a number, and whether the client may be named. The pitch may only claim these;
- objections heard in every sector, with answers;
- weekly contact capacity across all campaigns, and an optional quarter target.

**Campaign questions.** A campaign now answers 11 questions about its goal: sector, the problem it bets on, one service from the profile, what the lead gets for free for replying, an optional reason to buy now, Maps signals, minimum reviews, the phone routine, sector-specific objections, a case from the profile, and the goal (meetings, wins, end date, leads per week, and a stop rule), plus tone. One service per campaign, so outcomes show which service sells.

**Maps signals act at stage 1.** A campaign marks each signal as wanted or disqualifying: no website, only a social page, own website, fewer than 20 reviews, more than 300 reviews, rating below 4.0. A disqualifying signal or a review count below the campaign minimum excludes the place, with the reason stored. Wanted signals are stored for the pitch and the funnel report. The weights in `Stage1Scorer` and ADR 0007 do not change. A place without a rating, or with no reviews, never counts as "rating below 4.0".

**Checks.** Current clients are excluded in every campaign, matched on the normalized phone number first and on the name second. `campaign new` and `campaign create` need a profile, refuse a service the profile doesn't list, and refuse all three website signals as disqualifying. The wizard refuses to start when other running campaigns already use the whole capacity, and refuses an end date in the past. It warns, without blocking, when a case comes from another sector, when "fewer than 20 reviews" is wanted but the minimum excludes it, and when leads per week exceed what is free.

Campaigns saved before this change still load. Their old answers are ignored and their search keeps working. Their YAML files need the new answers before `campaign create` accepts them again.

## Consequences

A new campaign takes about 3 to 4 minutes, down from answering 10 questions of which half repeated. Adding a client or a case happens once. The stage 1 list for a campaign no longer includes places the campaign itself rules out.

Wanted signals don't change the ranking yet, so the top share still follows the global weights. We revisit that with the outcome data from ADR 0012. "Several branches" is not a signal yet because it needs matching across places. The phone routine has one editable default, not one per category.
