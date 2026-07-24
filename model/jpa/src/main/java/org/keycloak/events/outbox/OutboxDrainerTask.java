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
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.jpa.entities.OutboxEntryEntity;
import org.keycloak.timer.ScheduledTask;
import org.keycloak.utils.KeycloakSessionUtil;

import org.jboss.logging.Logger;

/**
 * Drains the generic outbox for one registered {@code entryKind}: locks
 * due PENDING rows, hands them off to the kind's
 * {@link OutboxDeliveryHandler}, and transitions each row based on the
 * returned {@link OutboxDeliveryOutcome}.
 *
 * <p>One drainer instance per registered kind. Each is wrapped in a
 * {@code ClusterAwareScheduledTaskRunner} at scheduling time so in an
 * HA deployment only one node drains a given kind per interval, even
 * though every node schedules the timer.
 *
 * <p>Concurrency within a single tick is cheap because rows are locked
 * {@code PESSIMISTIC_WRITE} via {@code FOR UPDATE SKIP LOCKED} by the
 * store, and each row is transitioned to a terminal state (DELIVERED /
 * back to PENDING with a future {@code next_attempt_at} / DEAD_LETTER)
 * before the transaction commits.
 *
 * <p>Per-tick housekeeping after the drain pass:
 * <ul>
 *   <li>Promote rows whose {@code createdAt} is older than
 *       {@link OutboxConfig#pendingMaxAge()} to DEAD_LETTER (backstop
 *       so stuck rows can't sit forever).</li>
 *   <li>Purge DELIVERED rows past {@link OutboxConfig#deliveredRetention()}.</li>
 *   <li>Purge DEAD_LETTER rows past {@link OutboxConfig#deadLetterRetention()}.</li>
 * </ul>
 */
public class OutboxDrainerTask implements ScheduledTask {

    private static final Logger log = Logger.getLogger(OutboxDrainerTask.class);

    protected final OutboxConfig config;
    protected final OutboxDeliveryHandler handler;
    protected final Function<KeycloakSession, OutboxStore> storeFactory;

    public OutboxDrainerTask(OutboxConfig config,
                             OutboxDeliveryHandler handler,
                             Function<KeycloakSession, OutboxStore> storeFactory) {
        this.config = Objects.requireNonNull(config, "config");
        this.handler = Objects.requireNonNull(handler, "handler");
        this.storeFactory = Objects.requireNonNull(storeFactory, "storeFactory");
        if (!Objects.equals(config.entryKind(), handler.entryKind())) {
            throw new IllegalArgumentException(
                    "config.entryKind=" + config.entryKind()
                            + " does not match handler.entryKind=" + handler.entryKind());
        }
    }

    @Override
    public void run(KeycloakSession session) {
        // Publish the drainer's KeycloakSession into the thread-local
        // so handler collaborators that haven't been refactored to
        // take an explicit session parameter still resolve correctly.
        KeycloakSession previous = KeycloakSessionUtil.getKeycloakSession();
        KeycloakSessionUtil.setKeycloakSession(session);
        try {
            OutboxStore store = storeFactory.apply(session);
            drain(session, store);
            promoteStaleQueuedToDeadLetter(store);
            purgeDeliveredOlderThanRetention(store);
            purgeDeadLetterOlderThanRetention(store);
        } finally {
            KeycloakSessionUtil.setKeycloakSession(previous);
        }
    }

    protected void drain(KeycloakSession session, OutboxStore store) {
        List<OutboxEntryEntity> due = store.lockDueForDrain(config.entryKind(), config.batchSize());
        if (due.isEmpty()) {
            return;
        }
        log.debugf("Outbox drainer processing %d due row(s) for entryKind=%s", due.size(), config.entryKind());
        for (OutboxEntryEntity row : due) {
            processOne(session, store, row);
        }
    }

    protected void processOne(KeycloakSession session, OutboxStore store, OutboxEntryEntity row) {
        OutboxDeliveryResult result;
        try {
            result = handler.deliver(session, row);
            if (result == null) {
                result = OutboxDeliveryResult.retry("delivery handler returned null result");
            }
        } catch (RuntimeException e) {
            log.warnf(e, "Outbox delivery handler threw — treating as RETRY. id=%s entryKind=%s correlationId=%s",
                    row.getId(), row.getEntryKind(), row.getCorrelationId());
            String message = e.getMessage() == null
                    ? e.getClass().getSimpleName()
                    : e.getClass().getSimpleName() + ": " + e.getMessage();
            result = OutboxDeliveryResult.retry(message);
        }

        switch (result.outcome()) {
            case DELIVERED -> {
                store.markDelivered(row);
                log.debugf("Outbox delivered. id=%s entryKind=%s correlationId=%s attempts=%d",
                        row.getId(), row.getEntryKind(), row.getCorrelationId(), row.getAttempts());
            }
            case RETRY -> handleRetry(store, row, result.errorMessage());
            case DEAD_LETTER -> {
                String reason = result.errorMessage() != null ? result.errorMessage()
                        : "handler returned DEAD_LETTER (attempt " + (row.getAttempts() + 1) + ")";
                store.markDeadLetter(row, reason);
                log.warnf("Outbox dead-lettered by handler. id=%s entryKind=%s correlationId=%s reason=%s",
                        row.getId(), row.getEntryKind(), row.getCorrelationId(), reason);
            }
            case ORPHANED -> {
                String reason = result.errorMessage() != null ? result.errorMessage()
                        : "handler returned ORPHANED (destination no longer exists)";
                store.markDeadLetter(row, reason);
                log.warnf("Outbox dead-lettered as orphan. id=%s entryKind=%s correlationId=%s",
                        row.getId(), row.getEntryKind(), row.getCorrelationId());
            }
        }
    }

    protected void handleRetry(OutboxStore store, OutboxEntryEntity row, String errorMessage) {
        int nextAttempts = row.getAttempts() + 1;
        String reason = errorMessage != null ? errorMessage : "delivery failed";
        if (config.backoff().isExhausted(nextAttempts)) {
            log.warnf("Outbox dead-lettered after %d attempts. id=%s entryKind=%s correlationId=%s",
                    nextAttempts, row.getId(), row.getEntryKind(), row.getCorrelationId());
            store.markDeadLetter(row, reason);
            return;
        }
        Instant nextAttemptAt = config.backoff().computeNextAttemptAt(Instant.now(), nextAttempts);
        log.debugf("Outbox scheduling retry. id=%s attempts=%d nextAttemptAt=%s",
                row.getId(), nextAttempts, nextAttemptAt);
        store.recordFailure(row, nextAttemptAt, reason);
    }

    protected void promoteStaleQueuedToDeadLetter(OutboxStore store) {
        Duration pendingMaxAge = config.pendingMaxAge();
        if (pendingMaxAge == null || pendingMaxAge.isZero() || pendingMaxAge.isNegative()) {
            return;
        }
        Instant cutoff = Instant.now().minus(pendingMaxAge);
        int promoted = store.promoteStaleQueuedToDeadLetter(config.entryKind(), cutoff,
                "queued exceeded pendingMaxAge");
        if (promoted > 0) {
            log.infof("Outbox promoted %d stale queued row(s) to DEAD_LETTER (entryKind=%s, pendingMaxAge=%s)",
                    promoted, config.entryKind(), pendingMaxAge);
        }
    }

    protected void purgeDeliveredOlderThanRetention(OutboxStore store) {
        Duration retention = config.deliveredRetention();
        if (retention == null || retention.isZero() || retention.isNegative()) {
            return;
        }
        store.purgeDeliveredOlderThan(config.entryKind(), Instant.now().minus(retention));
    }

    protected void purgeDeadLetterOlderThanRetention(OutboxStore store) {
        Duration retention = config.deadLetterRetention();
        if (retention == null || retention.isZero() || retention.isNegative()) {
            return;
        }
        store.purgeDeadLetterOlderThan(config.entryKind(), Instant.now().minus(retention));
    }
}
