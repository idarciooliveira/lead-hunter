package me.iofdev.leadhunter.usage;

import java.time.OffsetDateTime;

/**
 * Which spend to count. Every field is optional. {@code from} is inclusive and {@code to} is exclusive.
 */
public record UsageFilter(OffsetDateTime from, OffsetDateTime to, Long campaignId) {

    public static UsageFilter all() {
        return new UsageFilter(null, null, null);
    }
}
