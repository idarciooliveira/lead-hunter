package me.iofdev.leadhunter.api;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/** A lead with its position among the organization's non-excluded leads; null when excluded (ADR 0049). */
record RankedLeadDto(@JsonUnwrapped LeadDto lead, Integer rank) {
}
