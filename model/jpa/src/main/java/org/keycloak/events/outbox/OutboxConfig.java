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
 */
public record OutboxConfig(
        String entryKind,
        int batchSize,
        OutboxBackoff backoff,
        Duration deadLetterRetention,
        Duration deliveredRetention,
        Duration pendingMaxAge) {

    public OutboxConfig {
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
