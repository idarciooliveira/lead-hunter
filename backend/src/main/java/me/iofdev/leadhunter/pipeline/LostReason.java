package me.iofdev.leadhunter.pipeline;

/**
 * Why a lead was lost. The first four come from ADR 0012, {@code NOT_NOW} from
 * ADR 0020: a lead who says "fale comigo em Março" is a future deal, not a rejection.
 */
public enum LostReason {
    NO_BUDGET,
    WRONG_PERSON,
    HAS_SUPPLIER,
    NOT_INTERESTED,
    NOT_NOW
}
