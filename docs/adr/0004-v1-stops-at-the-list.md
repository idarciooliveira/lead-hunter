# 0004. Version 1 stops at the ranked list; outreach stays manual

- Date: 2026-09-28
- Status: Accepted

## Context

The owner contacts leads by WhatsApp message or phone call, and can handle 30 to 40 contacts a week. Automated sending brings deliverability work, and WhatsApp bans numbers that send bulk messages to strangers.

## Decision

Version 1 produces a ranked list of leads with contact data, a written reason for the score, a call opener, and a WhatsApp or email draft. It does not send anything.

Each lead gets a `wa.me` link with the message pre-filled. The owner opens it and presses send on WhatsApp Business.

The main view is a daily queue of about 8 leads per weekday, sized to the 30 to 40 contacts a week the owner can handle.

## Consequences

No sending infrastructure, no ban risk from automation. The owner stays in the loop for every message, which caps volume. That cap matches the owner's capacity anyway.
