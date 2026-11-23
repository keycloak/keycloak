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
package org.keycloak.events.outbox;

import java.time.Duration;

/**
 * Per-kind tuning parameters for {@link OutboxDrainerTask} and the
 * accompanying retention purges. One {@code OutboxConfig} is supplied
 * per registered {@code entryKind}, so SSF and webhooks can pick
 * different batch sizes, backoff curves, and retention windows
 * independently.
 *
 * <p>{@code deadLetterRetention}, {@code deliveredRetention}, and
 * {@code pendingMaxAge} accept {@code null} or a non-positive
 * {@link Duration} to disable the corresponding purge or backstop
 * (kept retained indefinitely).
 *
 * <p>{@code pendingMaxAge} is a backstop that promotes {@code QUEUED}
 * rows older than this duration to {@code DEAD_LETTER}. Bounds the
 * worst case where rows would otherwise sit forever (e.g. handler
 * repeatedly skipping, no per-receiver age cap, no realm/owner
 * removal). Should be comfortably above
 * {@link OutboxBackoff#getMaxNaturalRetryDuration()} so rows in
 * legitimate backoff aren't prematurely promoted, and shorter than
 * {@code deadLetterRetention} so promoted rows retain a meaningful
 * forensic window before the dead-letter purge deletes them.
 *
 * <p>{@code claimLease} is how long a tick may hold a claimed row
 * before other ticks treat it as abandoned and re-claim it; it must
 * cover the tick budget plus the slowest single delivery. A
 * delivery that outlives its lease is still recorded if no other
 * tick re-claimed the row in the meantime, but may be duplicated
 * otherwise (at-least-once). {@code tickBudget} bounds how long one
 * tick keeps delivering; claimed rows it does not get to are handed
 * back for the next tick. {@code null} means unbounded.
 *
 * <p>{@code perOwnerBatchSize} switches the claim to owner-fair mode:
 * instead of the oldest {@code batchSize} due rows regardless of
 * owner, the tick visits the owners with due rows oldest-first and
 * takes at most this many rows from each per round, going round
 * again until the batch is full or nothing is due. One slow or
 * flooding destination then gets its share of a batch rather than
 * all of it. {@code null} keeps the plain oldest-first claim.
 *
 * <p>{@code deliveryConcurrency} is the number of delivery workers a
 * tick may use. Rows are grouped by owner; one owner's rows are always
 * delivered in order by one worker, different owners in parallel.
 * {@code null} or 1 delivers on the tick thread. Workers come from the
 * {@code outbox-delivery-<kind>} executor of the {@code ExecutorsProvider}.
 */
public record OutboxConfig(
        String entryKind,
        int batchSize,
        OutboxBackoff backoff,
        Duration deadLetterRetention,
        Duration deliveredRetention,
        Duration pendingMaxAge,
        Duration claimLease,
        Duration tickBudget,
        Integer perOwnerBatchSize,
        Integer deliveryConcurrency) {

    public static final Duration DEFAULT_CLAIM_LEASE = Duration.ofMinutes(5);

    /** Default lease, unbounded tick, plain oldest-first claim. */
    public OutboxConfig(String entryKind,
                        int batchSize,
                        OutboxBackoff backoff,
                        Duration deadLetterRetention,
                        Duration deliveredRetention,
                        Duration pendingMaxAge) {
        this(entryKind, batchSize, backoff, deadLetterRetention, deliveredRetention, pendingMaxAge,
                DEFAULT_CLAIM_LEASE, null, null, null);
    }

    /** Plain oldest-first claim. */
    public OutboxConfig(String entryKind,
                        int batchSize,
                        OutboxBackoff backoff,
                        Duration deadLetterRetention,
                        Duration deliveredRetention,
                        Duration pendingMaxAge,
                        Duration claimLease,
                        Duration tickBudget) {
        this(entryKind, batchSize, backoff, deadLetterRetention, deliveredRetention, pendingMaxAge,
                claimLease, tickBudget, null, null);
    }

    /** Serial delivery on the tick thread. */
    public OutboxConfig(String entryKind,
                        int batchSize,
                        OutboxBackoff backoff,
                        Duration deadLetterRetention,
                        Duration deliveredRetention,
                        Duration pendingMaxAge,
                        Duration claimLease,
                        Duration tickBudget,
                        Integer perOwnerBatchSize) {
        this(entryKind, batchSize, backoff, deadLetterRetention, deliveredRetention, pendingMaxAge,
                claimLease, tickBudget, perOwnerBatchSize, null);
    }

    public OutboxConfig {
        if (perOwnerBatchSize != null && perOwnerBatchSize <= 0) {
            throw new IllegalArgumentException("perOwnerBatchSize must be positive or null, got " + perOwnerBatchSize);
        }
        if (deliveryConcurrency != null && deliveryConcurrency <= 0) {
            throw new IllegalArgumentException("deliveryConcurrency must be positive or null, got " + deliveryConcurrency);
        }
        if (claimLease == null || claimLease.isZero() || claimLease.isNegative()) {
            throw new IllegalArgumentException("claimLease must be positive, got " + claimLease);
        }
        if (tickBudget != null && (tickBudget.isZero() || tickBudget.isNegative())) {
            throw new IllegalArgumentException("tickBudget must be positive or null, got " + tickBudget);
        }
        if (tickBudget != null && tickBudget.compareTo(claimLease) >= 0) {
            throw new IllegalArgumentException("tickBudget " + tickBudget + " must be shorter than claimLease " + claimLease);
        }
        if (entryKind == null || entryKind.isBlank()) {
            throw new IllegalArgumentException("entryKind must not be blank");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive, got " + batchSize);
        }
        if (backoff == null) {
            throw new IllegalArgumentException("backoff must not be null");
        }
    }
}
