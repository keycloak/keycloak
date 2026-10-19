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

import org.keycloak.models.KeycloakSession;

/**
 * Per-kind plug-in that knows how to deliver an {@link OutboxEntry}'s
 * payload to its destination. The drainer is generic — for each
 * claimed row it calls {@link #prepare(KeycloakSession, OutboxEntry)}
 * inside a short transaction, runs the returned {@link OutboxDelivery}
 * outside any transaction, and then records the transition the result
 * asks for.
 *
 * <p>Two phases, so that no database connection or transaction is held
 * while a destination is being called:
 * <ol>
 *   <li>{@code prepare} runs with a session in a transaction. Resolve
 *       the realm, the destination's configuration, credentials and the
 *       HTTP client here, and capture them in the returned delivery.
 *       Outcomes known at this point — destination gone (orphaned),
 *       paused or backed off (defer) — are returned as
 *       {@link OutboxDelivery#settled(OutboxDeliveryResult)}.</li>
 *   <li>{@link OutboxDelivery#execute()} runs with no session bound to
 *       the thread and no transaction, possibly on a delivery worker
 *       thread when the kind is configured for parallel delivery.
 *       It must not touch the model; it performs the call and maps the
 *       response to an {@link OutboxDeliveryResult}.</li>
 * </ol>
 *
 * <p>One handler per registered {@code entryKind}. Implementations are
 * free to interpret the {@code payload} and {@code metadata} columns
 * however they like — the store treats both as opaque text.
 *
 * <p>Keep a delivery bounded well inside {@link OutboxConfig#claimLease()}
 * (HTTP connect and read timeouts): a delivery that outlives its lease
 * can be re-claimed by another tick and delivered a second time.
 *
 * <p>The returned result's {@code errorMessage} is persisted into the
 * row's {@code last_error} column ({@code VARCHAR(2048)}, truncated by
 * the store) and logged at DEBUG only, so it may carry the
 * destination's response text.
 */
public interface OutboxDeliveryHandler {

    /**
     * The {@code entryKind} this handler is responsible for. Must
     * match the {@code entry_kind} column of every row this handler
     * will be invoked for.
     */
    String entryKind();

    /**
     * Phase one, inside a transaction: resolve what the delivery needs
     * and return it as an {@link OutboxDelivery}, or a settled result.
     * A thrown {@link RuntimeException} or a {@code null} return is
     * treated as {@link OutboxDeliveryResult#retry(String)}.
     */
    OutboxDelivery prepare(KeycloakSession session, OutboxEntry row);
}
