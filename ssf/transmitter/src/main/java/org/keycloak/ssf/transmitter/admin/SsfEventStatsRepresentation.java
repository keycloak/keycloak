package org.keycloak.ssf.transmitter.admin;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Slim summary of SSF event state for a realm or a single receiver
 * client — counts and the oldest {@code createdAt} per status. Drives
 * the {@code GET /admin/realms/{realm}/ssf/events/stats} and
 * {@code GET /admin/realms/{realm}/ssf/clients/{clientId}/events/stats}
 * endpoints that operators use to answer "is the outbox draining or
 * accumulating?" without scraping Prometheus or hitting the database
 * directly.
 *
 * <p>Statuses with zero rows are omitted from the {@code statuses} map
 * — the SQL {@code GROUP BY} that drives the underlying query doesn't
 * synthesize zero-rows, and there's no point inflating the wire shape.
 */
public class SsfEventStatsRepresentation {

    /**
     * Per-status snapshot. Keys are the wire form of
     * {@link org.keycloak.models.jpa.entities.OutboxEntryStatus} —
     * {@code PENDING}, {@code DELIVERED}, {@code DEAD_LETTER},
     * {@code HELD}.
     */
    private Map<String, StatusEntry> statuses = new LinkedHashMap<>();

    public Map<String, StatusEntry> getStatuses() {
        return statuses;
    }

    public void setStatuses(Map<String, StatusEntry> statuses) {
        this.statuses = statuses;
    }

    public static class StatusEntry {

        private long count;

        /**
         * Earliest {@code createdAt} across rows in this status, or
         * {@code null} if no rows exist (in which case the entry would
         * normally be omitted from the parent map entirely).
         */
        private Instant oldestCreatedAt;

        public StatusEntry() {
        }

        public StatusEntry(long count, Instant oldestCreatedAt) {
            this.count = count;
            this.oldestCreatedAt = oldestCreatedAt;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public Instant getOldestCreatedAt() {
            return oldestCreatedAt;
        }

        public void setOldestCreatedAt(Instant oldestCreatedAt) {
            this.oldestCreatedAt = oldestCreatedAt;
        }
    }
}
