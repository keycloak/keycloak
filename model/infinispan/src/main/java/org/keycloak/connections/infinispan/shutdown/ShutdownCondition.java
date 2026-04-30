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

package org.keycloak.connections.infinispan.shutdown;

/**
 * Represents a condition that may block shutdown until it is resolved.
 * <p>
 * Implementations define what "in progress" means (e.g. a cache rehash), and receive callbacks when the condition is
 * resolved ({@link #complete()}) or when the shutdown timeout expires ({@link #onTimeout()}).
 */
public interface ShutdownCondition {

    /**
     * @return {@code true} if the operation is still in progress and shutdown should wait.
     */
    boolean inProgress();

    /**
     * Invoked when the shutdown timeout expires while the condition is still {@linkplain #inProgress() in progress}.
     */
    void onTimeout();

    /**
     * Invoked when the condition is no longer {@linkplain #inProgress() in progress} and shutdown can proceed,
     * including when the condition was not in progress at the time of shutdown.
     */
    void complete();

}
