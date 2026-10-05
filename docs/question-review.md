# Question review

How the company and campaign questions of [ADR 0019](adr/0019-company-profile.md) were designed. We wrote a first version, then ran three review rounds with a reviewer agent briefed as a B2B sales qualification and survey design specialist (BANT, MEDDIC, Jobs-to-be-Done). Each round, the reviewer judged every question, scored the set from 1 to 10, and named its top 3 changes. We rewrote the questions and sent them back.

| Round | Version reviewed | Score | Biggest problem found |
|---|---|---|---|
| 1 | v1 | 5.5 | Answers were prose that the scorer and the funnel report can't use |
| 2 | v2 | 7.5 | Maps signals didn't affect stage 1; the campaign part took 6 to 7 minutes |
| 3 | v3 | 8.5 | Wrong default for the free offer; no sector field |

v4, below, is what `company setup` and `campaign new` ask today.

## Final questions (v4)

### Company profile

Answered once with `company setup`, about 10 minutes. Running it again offers the saved answers as defaults. Stored in the database.

| # | Question | Default | Used by |
|---|---|---|---|
| C1 | Company name | | pitch |
| C2 | How do you introduce the company in one sentence, as you'd say it on the phone? | | pitch |
| C3 | Services you sell: name, price range in Kz, delivery time (optional) | | campaign question 3, pitch |
| C4 | Which one is your entry offer, the cheapest first step a new client can buy? | first service | campaign question 3, objections |
| C5 | Where do you serve clients in person? | Luanda | default campaign locations |
| C6 | Current clients: name and phone | | excluded from every campaign |
| C7 | Results you can prove: sector, client, problem, what you built, result with a number, may we name the client. Up to 2 in the wizard | client "anonymous", may name: no | campaign question 10, pitch |
| C8 | Your answer to four common objections (é caro, já tenho Instagram ou Facebook, o meu sobrinho faz isso, agora não tenho tempo), plus any others | | pitch, calls |
| C9 | How many leads can you contact per week, across all campaigns? | 35 | campaign goal |
| C10 | Optional. Target this quarter: new clients and revenue in Kz | | funnel report |

### Campaign

Asked by `campaign new`, about 3 to 4 minutes before the search settings.

| # | Question | Default | Used by |
|---|---|---|---|
| 1 | Sector, in a word or two | | case check, funnel report |
| 2 | What problem are you betting this sector has? One sentence, in the customer's words | | pitch, funnel report |
| 3 | Which service do you pitch? One per campaign | the entry offer | pitch, funnel report |
| 4 | What does the lead get for free for replying? | Uma análise gratuita do vosso perfil no Google Maps | pitch |
| 5 | Optional. Why would they buy now rather than next year? | | pitch |
| 6 | Google Maps signals, each wanted, disqualifying, or doesn't matter: no website, only a social page, has its own website, fewer than 20 reviews, more than 300 reviews, rating below 4.0 | doesn't matter | stage 1 exclusions, pitch |
| 7 | Minimum Google reviews for a business that can pay | 0 | stage 1 exclusions |
| 8 | On the phone: who answers, who do you ask for, when not to call? | A recepção atende; pedir o dono ou gerente. | calls |
| 9 | Optional. Objections specific to this sector | | pitch, calls |
| 10 | Which case proves it? | none, and the pitch makes no claims | pitch |
| Goal | Meetings, won clients, end date, leads per week, and a stop rule "fewer than N interested after M contacts" | 3, 1, six weeks, free capacity, 2 after two weeks of contacts | funnel report |
| 11 | Tone of the messages | Formal, em português (o senhor / a senhora) | pitch |

The search settings follow: Google Maps terms, locations, places per search, target keywords, words to exclude, extra names to exclude.

Checks the wizard runs:

- It won't start when other running campaigns already use the whole weekly capacity.
- It refuses all three website signals as disqualifying, because that excludes every place.
- It refuses an end date in the past.
- It warns when the case comes from another sector.
- It warns when "fewer than 20 reviews" is wanted but the minimum excludes those places.

## Round 1

### What we sent: v1

#### Company profile (asked once, updated with `company update`)

C1. Company name
C2. What services do you sell? One per line, with a typical price range in Kz.
C3. Where do you operate?
C4. Your current clients. Never shown as leads, used later to calibrate scoring.
C5. What results can you prove? Concrete results from past projects.
C6. How many leads can you contact per week, across all campaigns?
C7. What is the company's goal for the next quarter?

#### Campaign (asked for every campaign)

1. Which sector and which problem are you going after?
2. Which of your services do you pitch in this campaign?
3. What is the measurable goal of this campaign?
4. How does the problem show from the outside in this sector?
5. How big is a good customer in this sector?
6. Who decides in this sector, and how do you reach them?
7. Do you have a case from this sector to use as proof?
8. What share of your weekly capacity goes to this campaign?
9. Who in this sector should never show up?

Then the search settings (not under review): Google Maps terms, locations, places per search,
target keywords, extra excluded names.

### Reviewer's answer

The main problem is that most answers are free prose, but the scoring stage uses explicit rules over about ten Maps fields. An answer the scorer can't map to `category`, `website class`, `rating`, `review count`, `neighborhood` or `open flags` only helps the LLM. Every question should say which of the three consumers it feeds: search/scoring, pitch LLM, or funnel report.

#### Company profile

| # | Verdict | Why / fix |
|---|---|---|
| C1 Name | Keep | Needed for the pitch signature. Add "how you introduce yourself in one line" (for example, "somos a X, fazemos sites e sistemas para PMEs em Luanda"). The LLM needs that more than the bare name. |
| C2 Services + price in Kz | Keep, tighten | This is the best question in the set. Store it as a structured list (id, name, min–max Kz, typical delivery time) so campaign Q2 can pick from it. Add an "entry offer" field: the cheapest first step you can sell, such as a landing page or a WhatsApp catalogue. |
| C3 Where do you operate | Change | It's ambiguous: it could mean an office address, a delivery area or a market. Ask instead: "Which provinces/municipalities will you serve in person? Which only remotely?" That makes the answer a location filter and a proximity score. It also overlaps with the per-campaign locations setting, so make it the default for that setting. |
| C4 Current clients | Change | Free-text names can't be matched against scraped places. Ask for name + phone or Maps link per client so dedup works on phone. "Calibrate scoring" is vague, so say what it's for: the clients' categories and signals become positive examples. Also add an optional "past lost/bad clients" list as negative examples. |
| C5 Provable results | Change | This overlaps with campaign Q7. Turn it into a case library here: sector, client (or "anonymous"), problem, what you built, a result with a number, and whether you may name the client. The campaign then picks a case instead of retyping it. Reject answers that have no number, because "cliente satisfeito" is not proof. |
| C6 Weekly lead capacity | Keep | This is a hard constraint for the top-share cut and for the funnel. Make it a number. |
| C7 Quarter goal | Drop or make numeric | No consumer uses a prose goal. If kept, ask "Revenue target this quarter (Kz)" and "new clients target (#)". The funnel report can then work backwards: wins needed → proposals → meetings → contacts. |

#### Campaign

| # | Verdict | Why / fix |
|---|---|---|
| 1 Sector + problem | Split | It's double-barreled. "Sector" should become a list of Maps categories: that answer is the search setup, so ask it once, in the search settings. "Problem" should be a one-line hypothesis in the customer's words ("clinics lose bookings because patients can only call"). The pitch LLM uses this most. |
| 2 Service to pitch | Keep, make a choice | Pick 1–2 from the C2 list, with the entry offer highlighted. Free text here duplicates C2. |
| 3 Measurable goal | Change | "Measurable" alone produces answers like "get clients". Force the funnel shape: target meetings, proposals and wins (#), plus an end date and the number of leads to contact. Add a kill criterion: "stop or rethink if fewer than N INTERESTED after M contacted". The funnel report can't judge a goal without a deadline and a denominator. |
| 4 Problem visible from outside | Keep, restructure | This is the most important question and currently the vaguest. Offer a checklist tied to available fields: no website / social-only / real site; rating below X; review count above Y (busy but underserved); multiple categories; opening hours missing. Add a free-text "later signals" box for the site crawl and review texts (e.g. reviews mention "difícil marcar", "não atendem o telefone"). Each ticked signal becomes a scoring rule, which is what ADR 0007 needs. |
| 5 Size of a good customer | Change | You can't observe size directly. Ask for proxies: minimum review count, multiple branches (same name at several addresses), premium neighborhoods (Talatona, Miramar, Alvalade...), category tiers. Also ask the smallest business that can afford the pitched service, which is the Budget in BANT, since PME ability to pay is the main qualifier here. |
| 6 Who decides + how to reach | Change | In Luanda PMEs the answer is almost always "the owner, via WhatsApp/phone", so this gets the same answer every time. Replace it with what changes per sector: "Who answers the listed phone, what do you ask for, and when is the best day/time to call?" (e.g. restaurants: not 12–15h). The pitch writer and the call routine use this. |
| 7 Sector case | Change | Pick from the C5 library. Allow "none" and then ask "closest adjacent case" so the LLM doesn't invent proof. Also add a rule that the LLM never claims a case that isn't in the library. |
| 8 Share of weekly capacity | Change | A percentage overlaps with C6. Ask "Leads per week for this campaign (#)", check that it fits C6 minus the other active campaigns, and use it for the top-share cut. |
| 9 Who should never show up | Keep, restructure | It overlaps with "extra excluded names" in the search settings. Make it structured: excluded categories, chains/franchises/multinationals (a name-pattern list), public entities, competitors (other software/web agencies), and places marked closed. Free-text reasons are useless to the filter. |

#### Missing

1. **Offer/hook (entry point).** Nothing asks what the lead gets for replying: a free audit of their Google profile, a demo, a mockup of their site. For cold WhatsApp this matters most after targeting. The LLM needs it, and without it every message is "we build websites".
2. **Timing/trigger (the T in BANT, the JTBD "struggling moment").** Why would they buy now? Examples: newly opened (few reviews but good rating), season (schools in Jan/Feb, events in Dec), growth signals (a new branch). Some triggers are observable and should become scoring rules.
3. **Expected objections + answer.** Examples: "já tenho Facebook", "é caro", "o meu sobrinho faz". The pitch LLM and the owner on the call both need this, and it makes LOST reasons easier to interpret.
4. **LOST reason categories per campaign** (price, no need, timing, no decision-maker reached, went elsewhere). Without fixed categories, recalibration after 50–100 leads won't work.
5. **Hypothesis being tested.** One line: "We believe X sector with signal Y converts better than Z." This turns each campaign into a calibration experiment the funnel report can confirm or reject.
6. **Language/tone per campaign** (PT formal "o senhor", PT informal, EN). The LLM will get this wrong otherwise.

#### Overlaps

- C5 ↔ Q7 (proof)
- C6 ↔ Q8 (capacity)
- C3 ↔ locations setting
- Q1 sector ↔ Maps terms
- Q9 ↔ extra excluded names
- Q6 duplicates itself across campaigns

#### Score: 5.5/10

The split between profile and campaign is right, and C2 (with prices), Q3 and Q4 point in the right direction. But most questions produce prose that the rule-based scorer and the funnel report can't consume, and there is no offer, timing or objection input for the pitch.

#### Top 3 changes by impact

1. **Make Q4, Q5 and Q9 structured checklists tied to the scraped fields** (website class, rating, review count, neighborhood, branches, categories), with a separate free-text box for later crawl/review signals. This directly produces the scoring rules and exclusions; right now those answers are unusable by the scorer.
2. **Rewrite Q3 as funnel numbers + end date + kill criterion, and replace Q8 with leads/week checked against C6.** This gives the funnel report something to measure and makes each campaign a testable hypothesis for the 50–100-lead recalibration.
3. **Add an offer/hook question and an objections question, and turn C5 into a tagged case library that Q7 picks from.** This gives the pitch LLM concrete, non-invented material, which is what raises reply rates on cold WhatsApp.

## Round 2

### What we sent: v2

#### Company profile (`company setup`, then `company update`)

C1. Company name.
C2. How do you introduce the company in one sentence, as you'd say it on the phone?
C3. Services you sell. One per line: name | price range in Kz | typical delivery time.
C4. Which of these is your entry offer, the cheapest first step a new client can buy?
C5. Where do you serve clients in person? Municipalities or provinces. Becomes the default campaign locations.
C6. Current clients. One per line: name, optionally | phone. They never show up as leads, in any campaign.
C7. Cases you can prove, added one at a time: sector | client name or "anonymous" | problem | what you built | result with a number | may we name the client (y/n).
C8. How many leads can you contact per week, across all campaigns? (number)
C9. Target for this quarter: new clients (number) and revenue (Kz).

#### Campaign (`campaign new`)

1. What problem are you betting this sector has? One sentence in the customer's words, e.g. "clínicas perdem marcações porque só atendem por telefone".
2. Which service do you pitch? Pick 1 or 2 from the company list.
3. What does the lead get for replying? e.g. free review of their Google profile, a mockup of their site, a 15 minute demo.
4. Why would they buy now rather than next year? Optional. e.g. newly opened, school enrolment season.
5. Which signals on Google Maps point to the problem? Pick from: no website | only Instagram/Facebook | many reviews (busy) | few reviews (new) | rating below 4 | several branches. Plus, in free text, what to look for later in reviews and on the website.
6. What's the smallest business that can pay for the service? Minimum number of Google reviews, and anything else you'd use to judge size.
7. Who answers the listed phone, who do you ask for, and when shouldn't you call?
8. Which objections do you expect, and how do you answer each? One per line: objection | answer.
9. Which case proves it? Pick from the company cases, or none. With none, the pitch makes no claims.
10. Goal: meetings (number), won clients (number), end date, leads per week for this campaign. Checked against company capacity.
11. When do you stop? "Fewer than N interested after M contacted."
12. Tone: formal (o senhor/a senhora) or informal, and the language.

Then the search settings: Google Maps terms, locations (default from C5), places per search, target keywords,
categories or words to exclude (e.g. competitors: agência digital), extra excluded names.

#### What I did not take from round 1, and why
- Structured signals (Q5) are stored per campaign but do not change scoring weights yet. Weights live in Stage1Scorer and ADR 0007 and change only after 50-100 marked leads.
- LOST reasons stay global. ADR 0012 already fixes them: no budget, wrong person, already has a supplier, not interested.
- "Hypothesis being tested" is folded into Q1. "Past bad clients" dropped for now, the owner has too few.

### Reviewer's answer

v2 is a big step up. Nearly every answer now has a clear consumer. The remaining problems are answer time, one rejection that I think is a real mistake (Q5 not affecting scoring), and a few small conflicts.

#### Company profile

| # | Verdict | Notes |
|---|---|---|
| C1 Name | Keep | |
| C2 One-sentence intro | Keep | |
| C3 Services with price and delivery time | Keep | Make delivery time optional. It is the field most often left blank or guessed, and the pitch rarely needs it. |
| C4 Entry offer | Keep | Make it a pick from C3, not free text. |
| C5 In-person area | Keep | |
| C6 Current clients | Change | Matching on name alone fails on Maps variants ("Clínica Sagrada Esperança" vs "Sagrada Esperança - Talatona"). Make phone strongly recommended, accept a Maps link as an alternative, and warn when a line has only a name. Also allow pasting a list or importing a file. Typing 20+ clients by hand breaks the 10-minute budget. |
| C7 Case library | Keep, default "may name" to n | This is the slowest item: about 6 fields, 1–2 min per case. At setup, ask for at most 2 cases, then say "add more with `company update`". |
| C8 Weekly capacity | Keep | |
| C9 Quarter target | Keep, make optional | It is only used for the funnel report's "on track?" line. |

**Company time:** about 8–12 minutes, depending on how many clients and cases the owner enters. It fits in 10 minutes with paste/import for C6, a 2-case cap on C7, and optional delivery time in C3.

#### Campaign

| # | Verdict | Notes |
|---|---|---|
| 1 Problem bet | Keep | Good. |
| 2 Service to pitch | Change | Allow only one service. With two, you can't tell which pitch converted, and the recalibration data gets muddied. Pitch a second service in a separate campaign. |
| 3 Offer for replying | Keep | Best new question. Default it to C4 with an edit option. |
| 4 Why now | Keep, optional | But the outcome can't be measured today (see LOST reasons below). |
| 5 Maps signals | Keep, and add anti-signals | Add "has a real website" as a pick. Let the owner mark each signal as **wanted** or **disqualifying**. For a website campaign, a real website disqualifies; for a booking-system campaign it may be a positive. Also, "few reviews (new)" conflicts with Q6's minimum review count, so the wizard should warn when both are set. |
| 6 Smallest business that can pay | Keep | Put the review-count number first. Show "anything else" as an optional line. |
| 7 Phone: who answers, who to ask for, when not to call | Change | This is three questions in one. Pre-fill "when not to call" from a per-category default (restaurants 11:30–15:00, clinics mornings) and let the owner edit it. That leaves one answer to type. |
| 8 Objections | Move | Most objections are the same in every sector: "é caro", "já tenho Facebook/Instagram", "o meu sobrinho faz", "não tenho tempo". Keep an objection library in the company profile. The campaign then shows the library pre-ticked and asks only "any sector-specific objection?". This saves about 1.5 min per campaign. |
| 9 Case | Keep | Warn when the case sector doesn't match the campaign sector. |
| 10 Goal and leads per week | Keep, merge with 11 | Put both on one screen with computed defaults. |
| 11 Stop rule | Default it | Suggest a value such as "fewer than 2 interested after 40 contacted", computed from leads per week × 2 weeks, and let the owner accept it with Enter. |
| 12 Tone and language | Default it | Formal Portuguese, press Enter to change. The owner will pick the same answer nearly every time. |

**Campaign time as written:** about 6–7 minutes before search settings. The slowest items are Q8 (~90s), Q7 (~40s), Q10 + Q11 (~60s) and Q5 (~30s). With the cuts above, it is about 3.5–4 minutes: roughly 8 typed answers and 4 accepted defaults. That leaves room for the search settings within about 5 minutes.

#### Rejections I still disagree with

1. **Q5 not affecting scoring: a mistake.** I agree that the scoring weights should wait for 50–100 marked leads. But the direction of a signal changes per campaign, and one global weight set can't express that. With global weights, the top-share cut will keep leads the campaign doesn't want, for example businesses that already have a real site in a website campaign. The owner then contacts the wrong leads. The calibration data is also skewed, because you only learn outcomes for leads the global weights chose. Fix: leave the ADR 0007 weights alone, and apply Q5 only as a per-campaign filter and sign (wanted = must-have or boost, disqualifying = excluded). Record that in a new ADR.
2. **LOST reasons fixed by ADR 0012: partly a mistake.** There is no "not now / timing" reason. Q4 bets on timing, and without that reason you can't measure whether the bet held. Leads who say "talk to me in March" get recorded as "not interested", which hides future deals. Add "not now" as a new reason in an ADR that supersedes 0012 on this point.
3. **Hypothesis folded into Q1: acceptable**, as long as the funnel report prints Q1 next to the result.
4. **Past bad clients dropped: acceptable** for now.

#### Still missing

- Anti-signals, covered in the Q5 row above.
- A company-level objection library, covered in the Q8 row above.
- Nothing else of major weight.

#### Score: 7.5/10

The structure is right and almost every answer has a consumer. Points are lost to the campaign part running over 5 minutes, Q5 having no effect on the stage-1 cut, and the missing "not now" reason.

#### Top 3 changes by impact

1. **Make Q5 a per-campaign filter with wanted and disqualifying signals**, without touching the weights, and record it in a new ADR. This fixes who gets contacted and keeps the calibration data clean.
2. **Bring the campaign part under 5 minutes:**
   - move objections to the company profile;
   - pre-fill Q3 from C4, Q12 as formal Portuguese, and the stop rule from the goal;
   - use per-category defaults for "when not to call";
   - merge Q10 and Q11.
3. **Add "not now" to the LOST reasons (new ADR) and limit Q2 to one service**, so the timing bet and the pitch can each be measured.

## Round 3

### What we sent: v3

#### Company profile (`company setup` about 10 min, then `company update -f company.yml`)

C1. Company name.
C2. How do you introduce the company in one sentence, as you'd say it on the phone?
C3. Services you sell. One per line: name | price range in Kz | delivery time (optional).
C4. Which one is your entry offer, the cheapest first step? Pick a number from C3.
C5. Where do you serve clients in person? Becomes the default campaign locations. [Luanda]
C6. Current clients. One per line: name | phone. A line with only a name gets a warning, because names on Maps vary. For a long list, edit company.yml and run `company update`.
C7. Up to 2 cases now, more later in company.yml: sector | client or "anonymous" | problem | what you built | result with a number | may we name the client [n].
C8. Objections you hear in every sector, and your answer. One per line: objection | answer. Hint: é caro, já tenho Instagram, o meu sobrinho faz, não tenho tempo.
C9. How many leads can you contact per week, across all campaigns? [35]
C10. Optional. Target this quarter: new clients (number) and revenue (Kz).

#### Campaign (`campaign new`, target 4 min before search settings)

1. What problem are you betting this sector has? One sentence in the customer's words.
2. Which service do you pitch? One number from the company list. A second service is a second campaign.
3. What does the lead get for replying? [default: the entry offer]
4. Optional. Why would they buy now rather than next year?
5. Google Maps signals. For each, w = wanted, d = disqualifying, Enter = doesn't matter:
   no website | only a social page | has its own website | fewer than 20 reviews | more than 300 reviews | rating below 4.0
   Disqualifying signals exclude the place at stage 1 with a reason. Wanted signals go to the pitch and the funnel report. The ADR 0007 weights don't change.
   The wizard warns when "fewer than 20 reviews" is wanted and Q6 asks for more than 20.
6. Minimum Google reviews for a business that can pay. [0] Places below it are excluded.
7. On the phone: who answers, who do you ask for, when not to call. [A recepção atende; pedir o dono ou gerente.]
8. Optional. Objections specific to this sector, beyond the company list: objection | answer.
9. Which case proves it? A number from the company cases, or Enter for none. With none, the pitch makes no claims. Warns when the case sector differs from the campaign.
10. Goal, on one screen: meetings [3], won clients [1], end date [+6 weeks], leads per week [company capacity minus other active campaigns],
    stop rule [fewer than 2 interested after 2 weeks of contacts].
11. Tone. [formal Portuguese, o senhor/a senhora]

Then the search settings: Google Maps terms, locations (default from C5), places per search, target keywords,
words to exclude (e.g. agência digital), extra excluded names.

#### Decisions on round 2
- Taken: signals as a per-campaign filter with wanted and disqualifying; one service per campaign; objections moved to the company; defaults for entry offer, tone, stop rule and phone routine; goal and stop rule merged; clients with phone and a warning; cases capped at 2 during setup.
- Taken as a docs change: LOST reason "not now" in a new ADR, since `lead mark` isn't built yet.
- Not taken: per-category "when not to call" defaults. We have no table of categories yet, one editable default is enough for now.
- Not taken: "several branches" as a signal. It needs matching across places, which comes later.

### Reviewer's answer

v3 is close to ready. I found two design errors that are cheap to fix, and a handful of checks the wizard needs so it can't produce empty or wrong lead lists. Anything not marked "fix before shipping" can stay as it is.

#### Company profile

| # | Verdict | Notes |
|---|---|---|
| C1 Name | Keep | |
| C2 One-sentence intro | Keep | |
| C3 Services, price, optional delivery time | Keep | |
| C4 Entry offer (pick from C3) | Keep | |
| C5 In-person area [Luanda] | Keep | |
| C6 Current clients, name and phone | Keep | **Fix before shipping:** normalize phones on both sides before matching. Remove spaces, dashes and the +244 or 00244 prefix, then compare the 9-digit number. Otherwise "923 456 789" and "+244923456789" don't match, a current client shows up as a lead, and no warning fires. |
| C7 Up to 2 cases | Keep | |
| C8 Company objections | Keep | *Fine for v1:* pre-fill the four hinted objections so the owner types only the answers. That saves about a minute. |
| C9 Weekly capacity [35] | Keep | |
| C10 Quarter target, optional | Keep | |

**Company time:** about 9–12 minutes. C6 and C7 take most of it, and a long client list goes through `company.yml` anyway. That is fine.

#### Campaign

| # | Verdict | Notes |
|---|---|---|
| 1 Problem bet | Keep | |
| 2 One service | Keep | |
| 3 What the lead gets for replying [entry offer] | Change. **Fix before shipping** | The default is the wrong kind of thing. The entry offer is the cheapest *paid* service. Q3 asks what the lead gets *for free* for replying. With this default, the LLM writes "reply and you can buy our landing page", which is a sale, not a reason to reply. Leave the default empty and show hints ("free review of your Google profile", "a mockup of your site", "a 15 minute demo"), or default to "free review of your Google profile". |
| 4 Why now, optional | Keep | |
| 5 Maps signals | Keep. **Fix before shipping:** 3 checks | (a) "no website", "only a social page" and "has its own website" cover every place between them. If all three are marked d, every place is excluded, so block that. (b) A place with no rating must count as unknown, not as "rating below 4.0". Otherwise marking rating as d excludes every new business. State the rule in the ADR. (c) A place with 0 reviews counts as "fewer than 20 reviews". Make sure the warning about the Q6 conflict also fires when "fewer than 20" is marked d and Q6 is set to 20 or more. *Fine for v1:* wanted signals don't change the ranking, so the top-share cut still follows the global weights. Revisit after 50–100 marked leads. |
| 6 Minimum reviews [0] | Keep | |
| 7 Phone routine [default] | Keep | *Fine for v1:* in very small shops the owner answers the phone, so the default is sometimes wrong. The owner can edit it. |
| 8 Sector objections, optional | Keep | |
| 9 Case | Keep. **Fix before shipping** (see "Conflicts" below) | |
| 10 Goal screen | Keep. **Fix before shipping:** block when the leads-per-week default is 0 or less (other campaigns already use the whole capacity). Also block an end date in the past. | *Fine for v1:* the default stop rule depends on time, so a slow week triggers it early. Revisit once there is outcome data. |
| 11 Tone [formal PT] | Keep | |

**Campaign time:** about 2.5–3.5 minutes when the owner accepts the defaults, and about 4 when both optional questions are answered. That is within the target, with room for the search settings.

#### Conflicts and missing items

- **Fix before shipping: the campaign has no sector field.** Q9 warns "when the case sector differs from the campaign", but the campaign never records a sector. Q1 is a problem sentence and the Maps terms are search strings. The funnel report also needs a label to compare campaigns across sectors. Add a one-word sector or campaign name at the top of `campaign new` (about 5 seconds), or drop the warning. I recommend adding the field.
- **Fine for v1:** "not now" as a docs-only change. When `lead mark` is built, the enum and the migration must include it from the first version.

#### Round 2 rejections

- **Per-category "when not to call" defaults:** agreed. One editable default is enough. Fine for v1.
- **"Several branches" as a signal:** agreed. Fine for v1. Revisit when places can be matched across searches.

#### Score: 8.5/10

Almost every answer now has a consumer. The campaign part fits the time budget and the scope decisions are sound. Points are lost to the wrong Q3 default and the missing sector field.

#### Top 3 changes, all fix before shipping

1. **Q3 default:** replace "entry offer" with a free thing the lead gets for replying, or with hints and no default. This affects every message the LLM writes.
2. **Add a campaign sector/name field.** The Q9 mismatch warning and comparisons across campaigns depend on it.
3. **Add wizard and matching checks:**
   - block marking all three website signals as disqualifying;
   - treat a missing rating as unknown;
   - block when leads per week is 0 or less;
   - normalize phones to 9 digits for matching current clients.

   Each one prevents an empty or wrong lead list that would otherwise fail without any warning.

## What changed after round 3

All the "fix before shipping" items went into v4:

- The free offer (question 4) defaults to a free review of the Google profile, not the paid entry offer.
- A sector question comes first, so the case check has something to compare against.
- The wizard refuses all three website signals as disqualifying.
- A missing rating, or a rating with no reviews, never counts as "rating below 4.0".
- The wizard refuses to start when no weekly capacity is free.
- Clients are matched on the phone normalized to E.164, so "923 111 222", "+244923111222" and "00244923111222" match.

We also took the "fine for v1" suggestion to pre-fill the four common objections. We left the other "fine for v1" items for after 50 to 100 marked leads: wanted signals that change the ranking, per-category phone defaults, and a stop rule that doesn't depend on time.
