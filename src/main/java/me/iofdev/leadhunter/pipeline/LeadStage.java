package me.iofdev.leadhunter.pipeline;

/** Where a lead is in the two-stage pipeline from ADR 0006. */
public enum LeadStage {
    /** Hit a hard filter. Never shown in the working list. */
    EXCLUDED,
    /** Passed the filters but ranked below the stage 1 cut. */
    BELOW_CUT,
    /** In the top share of stage 1, ready for enrichment. */
    QUALIFIED
}
