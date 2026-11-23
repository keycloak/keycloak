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
import java.util.Objects;
import java.util.function.Consumer;

import org.keycloak.models.KeycloakSession;

/**
 * Per-row result returned by an {@link OutboxDeliveryHandler#deliver}
 * invocation. Bundles the {@link OutboxDeliveryOutcome outcome} with a
 * single operator-facing {@code errorMessage} that the drainer
 * persists into the row's {@code last_error} column, and — for
 * {@link OutboxDeliveryOutcome#DEFER} only — the {@code notBefore}
 * instant the row should not be drained before.
 *
 * <p>Handlers should put as much diagnostic detail into
 * {@code errorMessage} as fits the column ({@code VARCHAR(2048)}) —
 * status code, response body excerpt, exception class — so admin
 * dashboards and log scans show the failure cause without having to
 * cross-reference timestamps in server logs.
 *
 * <p>{@link #delivered()} / {@link #orphaned()} return null for the
 * message; the drainer clears {@code last_error} on a successful
 * delivery.
 *
 * <p>An optional {@code effect} is consumer state that must commit
 * together with the transition: the drainer runs it inside the row's
 * record transaction, after the transition has been applied, with
 * that transaction's session. If the effect throws, the record is
 * rolled back, nothing of the transition is kept, and the row is
 * delivered again once its lease expires; so an effect must be
 * idempotent, like the delivery itself. Effects are for writes that
 * belong to the outcome (a remote id, a destination's health); plain
 * observation belongs in an {@link OutboxDrainerListener}, which runs
 * after the commit and cannot undo it.
 */
public record OutboxDeliveryResult(OutboxDeliveryOutcome outcome,
                                   String errorMessage,
                                   Instant notBefore,
                                   Consumer<KeycloakSession> effect) {

    public OutboxDeliveryResult {
        Objects.requireNonNull(outcome, "outcome");
        if (outcome == OutboxDeliveryOutcome.DEFER && notBefore == null) {
            throw new IllegalArgumentException("DEFER requires a notBefore instant");
        }
    }

    public OutboxDeliveryResult(OutboxDeliveryOutcome outcome, String errorMessage, Instant notBefore) {
        this(outcome, errorMessage, notBefore, null);
    }

    /**
     * The same result with {@code effect} to run in the record
     * transaction; {@code null} removes a previously set effect.
     */
    public OutboxDeliveryResult withEffect(Consumer<KeycloakSession> effect) {
        return new OutboxDeliveryResult(outcome, errorMessage, notBefore, effect);
    }

    public static OutboxDeliveryResult delivered() {
        return new OutboxDeliveryResult(OutboxDeliveryOutcome.DELIVERED, null, null);
    }

    public static OutboxDeliveryResult retry(String errorMessage) {
        return new OutboxDeliveryResult(OutboxDeliveryOutcome.RETRY, errorMessage, null);
    }

    /**
     * Reschedules the row to {@code notBefore} without counting a
     * delivery attempt. The drainer clamps instants in the past to
     * "now", so a handler can pass a destination-supplied timestamp
     * (e.g. parsed from a {@code Retry-After} header) unmodified.
     *
     * @param reason operator-facing explanation persisted into
     *               {@code last_error}; may be {@code null}.
     */
    public static OutboxDeliveryResult defer(Instant notBefore, String reason) {
        Objects.requireNonNull(notBefore, "notBefore");
        return new OutboxDeliveryResult(OutboxDeliveryOutcome.DEFER, reason, notBefore);
    }

    /**
     * Convenience for {@link #defer(Instant, String)} with a relative
     * delay from now.
     */
    public static OutboxDeliveryResult defer(Duration delay, String reason) {
        Objects.requireNonNull(delay, "delay");
        return defer(Instant.now().plus(delay), reason);
    }

    public static OutboxDeliveryResult deadLetter(String errorMessage) {
        return new OutboxDeliveryResult(OutboxDeliveryOutcome.DEAD_LETTER, errorMessage, null);
    }

    public static OutboxDeliveryResult orphaned() {
        return new OutboxDeliveryResult(OutboxDeliveryOutcome.ORPHANED, null, null);
    }

    public static OutboxDeliveryResult orphaned(String errorMessage) {
        return new OutboxDeliveryResult(OutboxDeliveryOutcome.ORPHANED, errorMessage, null);
    }
}
