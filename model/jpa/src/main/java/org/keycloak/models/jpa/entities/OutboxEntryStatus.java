/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.models.jpa.entities;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle status of a row in the generic outbox ({@code OUTBOX_ENTRY}).
 *
 * <p>The state machine is:
 * <pre>
 *     PENDING - delivery succeeds -> DELIVERED
 *     PENDING - retries exhausted -> DEAD_LETTER
 *     PENDING - upstream paused -> HELD
 *     HELD    - upstream resumed -> PENDING
 *     PENDING / HELD - admin "purge queued" -> (deleted)
 *     DEAD_LETTER - admin "retry" -> PENDING (resets attempts, next_attempt_at)
 * </pre>
 *
 * <p>Features that don't pause never produce {@link #HELD} rows; the
 * status is generic so the drainer doesn't need to know which feature
 * uses pause/resume.
 *
 * <p>Rows in {@link #DELIVERED} are kept briefly for audit / idempotency
 * (correlation-id dedup). Rows in {@link #DEAD_LETTER} are retained for
 * the configured per-feature dead-letter retention window before being
 * purged. The {@link #QUEUED} set captures the pre-terminal states the
 * "purge queued" admin operation targets.
 */
public enum OutboxEntryStatus {

    /**
     * Entry is queued for delivery. The drainer picks up rows in this
     * state whose {@code next_attempt_at} is due.
     */
    PENDING,

    /**
     * Entry was accepted by the destination (handler returned
     * {@code DELIVERED}) and needs no further action.
     */
    DELIVERED,

    /**
     * All retry attempts have been exhausted (or the row aged beyond
     * the configured pendingMaxAge backstop) without successful
     * delivery. Requires admin intervention.
     */
    DEAD_LETTER,

    /**
     * Entry is parked because the upstream (e.g. SSF stream) is in a
     * paused state. Drainers must skip rows in this state until the
     * upstream resumes and the rows are bulk-transitioned back to
     * {@link #PENDING} in original arrival order.
     */
    HELD;

    /**
     * Statuses representing entries that are queued — i.e. waiting to
     * reach a terminal state. Single source of truth for the
     * {@code DELETE .../events/queued} admin endpoints (and the
     * disable-on-save cleanup that drives them) so future additions
     * to the pre-terminal state set automatically extend the
     * "purge queued" semantics without API changes.
     */
    public static final Set<OutboxEntryStatus> QUEUED = EnumSet.of(PENDING, HELD);
}
