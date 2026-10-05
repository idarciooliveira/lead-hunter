-- Contact outcomes on the lead. See ADR 0012 and ADR 0020.
-- The status column already exists; a lost lead needs one of the five reasons
-- and every mark can carry a free-text note. Transitions are validated in Java
-- (LeadRepository.updateOutcome) so the CLI and the API never drift.

alter table lead add column lost_reason text;
alter table lead add column outcome_note text;
