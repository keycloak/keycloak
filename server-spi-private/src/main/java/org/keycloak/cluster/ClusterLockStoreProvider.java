/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.cluster;

import org.keycloak.provider.Provider;

/**
 * Database-backed distributed lock store. Uses a row-level lock (SELECT FOR UPDATE SKIP LOCKED)
 * to provide cross-cluster mutual exclusion within the caller's transaction.
 */
public interface ClusterLockStoreProvider extends Provider {

    /**
     * Attempt to acquire a distributed lock for the given task key within the current transaction.
     * Uses INSERT ON CONFLICT DO NOTHING to ensure the lock row exists, then SELECT FOR UPDATE SKIP LOCKED
     * to non-blockingly claim it. The lock is held until the current transaction commits or rolls back.
     *
     * @param taskKey the lock identifier
     * @param taskTimeoutInSeconds used to compute the row expiry (10x) for garbage collection
     * @return true if the lock was acquired, false if it is already held
     */
    boolean tryLock(String taskKey, int taskTimeoutInSeconds);
}
