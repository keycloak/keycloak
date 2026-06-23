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

/**
 * A periodic task that removes expired entries from the database.
 * <p>
 * Instances are created via the {@link ExpirationTaskBuilder} and registered with the Keycloak
 * {@link org.keycloak.timer.TimerProvider} by calling {@link #schedule()}.
 * <p>
 * Each invocation of {@link #run()} submits the cleanup work to an {@link java.util.concurrent.Executor}, ensuring the
 * timer thread is not blocked by long-running deletions. Concurrent runs are prevented by an internal guard: if a
 * previous run is still in progress, the new invocation is skipped.
 *
 * @see ExpirationTaskBuilder
 * @see ExpirationAction
 */
public interface ExpirationTask extends Runnable {

    /**
     * Registers this task with the {@link org.keycloak.timer.TimerProvider} to run periodically at the configured
     * interval.
     */
    void schedule();

    /**
     * Returns a new {@link ExpirationTaskBuilder} to configure and build an {@link ExpirationTask}.
     */
    static ExpirationTaskBuilder builder() {
        return new ExpirationTaskBuilder();
    }
}
