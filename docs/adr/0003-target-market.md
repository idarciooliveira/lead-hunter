# 0003. Target small PMEs in Luanda first, then Lubango and Benguela

- Date: 2026-09-28
- Status: Accepted

## Context

Past clients are small PMEs from different sectors: tourism, procurement, and a camera agency. The owner works alone and sells remotely. Google Maps coverage in Angola is thinner than in Europe or Brazil. Many real businesses have only an Instagram or Facebook page and a WhatsApp number, with no website or email.

## Decision

- Geography: Luanda first. Lubango and Benguela come next.
- Sectors to start with: schools, clinics, service companies, stores. Outcomes decide which ones stay.
- Company size: small PMEs. Chains, banks, telecoms, government bodies, and multinationals are excluded.
- Seed clients for calibration: ajabalg, kintexia, horizontetourangola. A scoring model that doesn't rank them high is wrong.
- A usable lead needs a phone number. Email is a bonus, not a requirement.

## Consequences

Scoring and enrichment look for WhatsApp numbers, Instagram, and Facebook as much as for websites and emails. Pitches are written in Portuguese. Adding a new city is a change to campaign input, not to code.
