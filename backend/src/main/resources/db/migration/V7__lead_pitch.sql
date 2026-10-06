-- The WhatsApp pitch written for a lead and the model that wrote it. See ADR 0040.
alter table lead add column pitch text;
alter table lead add column pitch_model text;
