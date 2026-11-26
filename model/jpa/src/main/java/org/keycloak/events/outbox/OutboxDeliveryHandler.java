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
import org.keycloak.models.jpa.entities.OutboxEntryEntity;

/**
 * Per-kind plug-in that knows how to actually deliver an
 * {@link OutboxEntryEntity}'s payload to its destination. The drainer
 * is generic — for each due row it calls {@link #deliver(KeycloakSession, OutboxEntryEntity)}
 * and transitions the row based on the returned {@link OutboxDeliveryResult}.
 *
 * <p>One handler per registered {@code entryKind}; the drainer locates
 * a handler by the row's {@code entryKind} value. Implementations are
 * free to interpret the {@code payload} and {@code metadata} columns
 * however they like — the store treats both as opaque text.
 *
 * <p>Synchronous by design — the handler returns when delivery has
 * either succeeded, failed retryably, or failed terminally. Long-poll
 * or fire-and-forget delivery semantics should be modelled by
 * returning {@link OutboxDeliveryResult#delivered()} as soon as the
 * payload has been handed off (e.g. enqueued in an external broker).
 */
public interface OutboxDeliveryHandler {

    /**
     * The {@code entryKind} this handler is responsible for. Must
     * match the {@code entry_kind} column of every row this handler
     * will be invoked for; the drainer uses this to map locked rows
     * back to a handler.
     */
    String entryKind();

    /**
     * Attempts delivery for one outbox row. The drainer holds a
     * pessimistic write lock on the row for the duration of the call;
     * implementations should keep the call bounded (no indefinite
     * blocking) and avoid touching unrelated database rows so the
     * lock window stays tight.
     *
     * <p>Implementations may throw {@link RuntimeException}; the
     * drainer treats an uncaught exception as
     * {@link OutboxDeliveryOutcome#RETRY} and records the exception
     * class + message in {@code last_error}.
     *
     * <p>The returned {@link OutboxDeliveryResult}'s
     * {@code errorMessage} (if any) is persisted into the row's
     * {@code last_error} column. Handlers should pack as much
     * diagnostic detail (HTTP status, response body excerpt, exception
     * class) into that single string as fits the column
     * ({@code VARCHAR(2048)}).
     */
    OutboxDeliveryResult deliver(KeycloakSession session, OutboxEntryEntity row);
}
