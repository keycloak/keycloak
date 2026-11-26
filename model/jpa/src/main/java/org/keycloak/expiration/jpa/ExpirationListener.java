/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
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

package org.keycloak.expiration.jpa;

import java.time.Duration;

/**
 * A listener notified after each expiration task run.
 * <p>
 * Implementations can use this callback for logging, alerting, or custom metrics. The built-in Micrometer metrics
 * listener is registered automatically when metrics are enabled via {@link ExpirationTaskBuilder#withMetrics(boolean)}.
 *
 * @see ExpirationTaskBuilder#withListener(ExpirationListener)
 */
@FunctionalInterface
public interface ExpirationListener {

    /**
     * Called after an expiration task run completes for a given scope (global or per-realm).
     *
     * @param realmId  the realm that was cleaned up, or {@code null} for non-realm-aware expiration tasks.
     * @param outcome  the outcome of the task run.
     * @param removed  the total number of expired entries removed across all batches.
     * @param duration the wall-clock duration of the task run.
     */
    void onTaskRun(String realmId, Outcome outcome, int removed, Duration duration);
}
