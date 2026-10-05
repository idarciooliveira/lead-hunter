package me.iofdev.leadhunter.pipeline;

/** Contact outcome, marked by the user. See ADR 0012. */
public enum LeadStatus {
    NEW, CONTACTED, NO_ANSWER, INTERESTED, MEETING, PROPOSAL_SENT, WON, LOST
}
