package me.iofdev.leadhunter.usage;

import java.time.OffsetDateTime;

import me.iofdev.leadhunter.auth.OrgId;

/**
 * Which spend to count: always one organization's, then optionally a time window and a campaign. {@code from} is inclusive and {@code to} is exclusive.
 */
public record UsageFilter(OrgId orgId, OffsetDateTime from, OffsetDateTime to, Long campaignId) {

    public static UsageFilter all(OrgId orgId) {
        return new UsageFilter(orgId, null, null, null);
    }
}
