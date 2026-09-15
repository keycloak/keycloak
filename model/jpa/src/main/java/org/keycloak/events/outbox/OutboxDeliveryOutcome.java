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

/**
 * Per-row result of an {@link OutboxDeliveryHandler#deliver} invocation.
 * The drainer maps each outcome to a row transition:
 *
 * <ul>
 *   <li>{@link #DELIVERED} → status {@code DELIVERED}, {@code deliveredAt} stamped.</li>
 *   <li>{@link #RETRY} → attempts++, {@code next_attempt_at} pushed forward
 *       per the kind's backoff curve. Promoted to {@link #DEAD_LETTER} once
 *       attempts are exhausted.</li>
 *   <li>{@link #DEAD_LETTER} → terminal failure regardless of remaining
 *       attempt budget (e.g. permanent destination error). Status set to
 *       {@code DEAD_LETTER} immediately.</li>
 *   <li>{@link #ORPHANED} → handler decided the row is no longer
 *       deliverable because the destination doesn't exist (e.g. the
 *       receiver client was deleted). Treated as a non-retryable
 *       terminal failure and recorded distinctly in metrics so
 *       operators can spot stream/owner leakage.</li>
 * </ul>
 */
public enum OutboxDeliveryOutcome {
    DELIVERED,
    RETRY,
    DEAD_LETTER,
    ORPHANED
}
