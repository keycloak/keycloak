package org.keycloak.ssf.transmitter.support;

import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore;

/**
 * Stamps {@link ClientStreamStore#SSF_LAST_ACTIVITY_TIMESLOT_KEY} on the
 * receiver client whenever it touches the SSF transmitter in a way
 * the spec classifies as "eligible Receiver activity" (SSF 1.0 §8.1.1
 * inactivity_timeout definition): any stream-management API hit for
 * PUSH or POLL streams, and the poll itself for POLL streams.
 *
 * <p>Writes coalesce — a stamp is only persisted when the stored
 * value is older than {@link #STAMP_GRANULARITY_SECONDS}. Without
 * this, a busy POLL receiver pulling every few seconds would hammer
 * the client-attribute table and trigger a cluster-wide Infinispan
 * invalidation per request; the inactivity-timeout check tolerates a
 * few minutes of staleness since real timeouts are minutes-to-days.
 */
public final class SsfActivityTracker {

    /**
     * Persist the timeslot only when the stored value is older than
     * this many seconds. 300s (5 min) = writes at most once every
     * 5 minutes per receiver. Inactivity-timeout accuracy degrades by
     * the same amount (up to 5 minutes late) — negligible given the
     * UI exposes inactivity timeouts in minute / hour / day units.
     */
    public static final long STAMP_GRANULARITY_SECONDS = 300L;

    private SsfActivityTracker() {
    }

    /**
     * Records activity for the given receiver client. No-op when
     * {@code client} is {@code null} (e.g. unauthenticated request
     * that never resolved a caller) so call sites don't have to
     * null-guard before invoking this helper. Write-coalesces per
     * {@link #STAMP_GRANULARITY_SECONDS}.
     */
    public static void stamp(ClientModel client) {
        if (client == null) {
            return;
        }
        long now = Time.currentTime();
        String existing = client.getAttribute(ClientStreamStore.SSF_LAST_ACTIVITY_TIMESLOT_KEY);
        if (existing != null && !existing.isBlank()) {
            try {
                long stored = Long.parseLong(existing.trim());
                if (now - stored < STAMP_GRANULARITY_SECONDS) {
                    return;
                }
            } catch (NumberFormatException ignored) {
                // Malformed attribute — fall through and overwrite.
            }
        }
        client.setAttribute(ClientStreamStore.SSF_LAST_ACTIVITY_TIMESLOT_KEY, String.valueOf(now));
    }
}
