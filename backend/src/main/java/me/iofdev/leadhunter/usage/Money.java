package me.iofdev.leadhunter.usage;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {

    private Money() {
    }

    /** Dollars with 4 decimals. An amount that would round to zero shows as "<$0.0001" so small LLM costs stay visible. */
    public static String usd(BigDecimal amount) {
        if (amount == null) {
            return "unknown";
        }
        BigDecimal rounded = amount.setScale(4, RoundingMode.HALF_UP);
        if (rounded.signum() == 0 && amount.signum() > 0) {
            return "<$0.0001";
        }
        return "$" + rounded.toPlainString();
    }
}
