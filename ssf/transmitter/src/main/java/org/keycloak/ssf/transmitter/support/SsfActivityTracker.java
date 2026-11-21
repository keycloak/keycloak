package org.keycloak.ssf.transmitter.support;

import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore;

import org.jboss.logging.Logger;

/**
 * Write-coalesced timestamp stamps on the receiver client.
 *
 * <p>{@link #stamp(ClientModel)} records
 * {@link ClientStreamStore#SSF_LAST_ACTIVITY_TIMESLOT_KEY} whenever the
 * receiver touches the SSF transmitter in a way the spec classifies as
 * "eligible Receiver activity" (SSF 1.0 §8.1.1 inactivity_timeout
 * definition): any stream-management API hit for PUSH or POLL streams,
 * and the poll itself for POLL streams.
 *
 * <p>{@link #stampPollCompleted(ClientModel)} records
 * {@link ClientStreamStore#SSF_STREAM_LAST_POLL_COMPLETED_AT_KEY} at the
 * end of a successfully served RFC 8936 poll, for the admin console and
 * a future min-poll-interval check.
 *
 * <p>Both writes coalesce — a stamp is only persisted when the stored
 * value is older than the stamp's granularity. Without this, a busy
 * POLL receiver pulling every few seconds would hammer the
 * client-attribute table and trigger a cluster-wide Infinispan
 * invalidation per request; the consumers tolerate the resulting
 * staleness (see the per-constant notes).
 */
public final class SsfActivityTracker {

    private static final Logger log = Logger.getLogger(SsfActivityTracker.class);

    /**
     * Persist the activity timeslot only when the stored value is older
     * than this many seconds. 300s (5 min) = writes at most once every
     * 5 minutes per receiver. Inactivity-timeout accuracy degrades by
     * the same amount (up to 5 minutes late) — negligible given the
     * UI exposes inactivity timeouts in minute / hour / day units.
     */
    public static final long STAMP_GRANULARITY_SECONDS = 300L;

    /**
     * Persist the last-poll-completed timestamp only when the stored
     * value is older than this many seconds. Deliberately equal to
     * {@link #STAMP_GRANULARITY_SECONDS}: every poll already stamps the
     * activity timeslot (via {@code SsfAuthUtil.canRead}) on the same
     * request, and both writes land on the same client entity — so with
     * matching granularity they coalesce onto the same requests and a
     * steadily polling receiver causes no additional client-cache
     * invalidations beyond the ones the activity stamp already costs. A
     * "last poll" reading accurate to within 5 minutes is plenty to
     * tell a live receiver from a dead one. A future min-poll-interval
     * check that rejects without stamping and always stamps on accepted
     * polls can at most <em>under</em>-enforce by this granularity,
     * never over-enforce.
     */
    public static final long POLL_STAMP_GRANULARITY_SECONDS = STAMP_GRANULARITY_SECONDS;

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
        stamp(client, ClientStreamStore.SSF_LAST_ACTIVITY_TIMESLOT_KEY, STAMP_GRANULARITY_SECONDS);
    }

    /**
     * Records the completion of a successfully served poll for the
     * given receiver client. Call after the poll response has been
     * assembled — after all transmitter-side work, not at request
     * arrival — and only on the success path;
     * rejected polls (bad request, ownership mismatch) must not count.
     * Write-coalesces per {@link #POLL_STAMP_GRANULARITY_SECONDS}.
     */
    public static void stampPollCompleted(ClientModel client) {
        stamp(client, ClientStreamStore.SSF_STREAM_LAST_POLL_COMPLETED_AT_KEY, POLL_STAMP_GRANULARITY_SECONDS);
    }

    /**
     * Stamps the current time (epoch seconds) into {@code attributeKey}
     * on {@code client} unless the stored value is younger than
     * {@code granularitySeconds}. No-op on a {@code null} client. A
     * malformed stored value is overwritten, and so is a value that
     * lies in the future: the coalescing window only applies to a
     * stored value in the past ({@code 0 <= age < granularity}). Without
     * the lower bound a future-dated value — a backward clock step on
     * the node (NTP correction, VM migration, host resume), or a value
     * written through the plain client-attributes API / a realm
     * import — would make {@code age} negative on every call and freeze
     * the stamp until wall-clock time caught up with it.
     */
    static void stamp(ClientModel client, String attributeKey, long granularitySeconds) {
        if (client == null) {
            return;
        }
        long now = Time.currentTimeSeconds();
        String existing = client.getAttribute(attributeKey);
        if (existing != null && !existing.isBlank()) {
            try {
                long stored = Long.parseLong(existing.trim());
                long age = now - stored;
                if (age >= 0 && age < granularitySeconds) {
                    log.tracef("Skipping %s stamp for client %s: stored=%d is within %ds of now=%d",
                            attributeKey, client.getClientId(), stored, granularitySeconds, now);
                    return;
                }
                if (age < 0) {
                    log.debugf("Stored %s=%d on client %s lies in the future (now=%d); treating as stale",
                            attributeKey, stored, client.getClientId(), now);
                }
            } catch (NumberFormatException ignored) {
                // Malformed attribute — fall through and overwrite.
            }
        }
        log.debugf("Stamping %s=%d on client %s", attributeKey, now, client.getClientId());
        client.setAttribute(attributeKey, String.valueOf(now));
    }
}
