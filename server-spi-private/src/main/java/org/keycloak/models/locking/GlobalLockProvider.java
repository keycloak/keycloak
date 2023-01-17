/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.locking;

import org.keycloak.models.KeycloakSessionTaskWithResult;
import org.keycloak.provider.Provider;

import java.time.Duration;

public interface GlobalLockProvider extends Provider {

    class Constants {
        public static final String KEYCLOAK_BOOT = "keycloak-boot";
    }

    /**
     * Acquires a new non-reentrant global lock that is visible to all Keycloak nodes.
     * Effectively the same as {@code withLock(lockName, null, task)}
     *
     * @param lockName Identifier used for acquiring lock. Can be any non-null string.
     * @param task The task that will be executed under the acquired lock
     * @param <V> Type of object returned by the {@code task}
     * @return Value returned by the {@code task}
     * @throws LockAcquiringTimeoutException When acquiring the global lock times out
     *                                       (see Javadoc of {@link #withLock(String, Duration, KeycloakSessionTaskWithResult)} for more details on how the time
     *                                       duration is determined)
     * @throws NullPointerException          When lockName is {@code null}.
     */
    default <V> V withLock(String lockName, KeycloakSessionTaskWithResult<V> task) throws LockAcquiringTimeoutException {
        return withLock(lockName, null, task);
    }

    /**
     * Acquires a new non-reentrant global lock that is visible to all Keycloak nodes. If the lock was successfully
     * acquired the method runs the {@code task} in a new transaction to ensure all data modified in {@code task}
     * is committed to the stores before releasing the lock and returning to the caller.
     * <p/>
     * If there is another global lock with the same identifier ({@code lockName}) already acquired, this method waits
     * until the lock is released, however, not more than {@code timeToWaitForLock} duration. If the lock is not
     * acquired after {@code timeToWaitForLock} duration, the method throws {@link LockAcquiringTimeoutException}.
     * <p/>
     * When the execution of the {@code task} finishes, the acquired lock must be released regardless of the result.
     * <p/>
     * <b>A note to implementors of the interface:</b>
     * <p/>
     * To make sure acquiring/releasing the lock is visible to all Keycloak nodes it may be needed to run the code that
     * acquires/releases the lock in a separate transactions. This means together the method can use 3 separate
     * transactions, for example:
     * <pre>
     *     try {
     *         KeycloakModelUtils.runJobInTransaction(factory,
     *                                innerSession -> /* run code that acquires the lock *\/)
     *
     *         KeycloakModelUtils.runJobInTransactionWithResult(factory, task)
     *     } finally {
     *         KeycloakModelUtils.runJobInTransaction(factory,
     *                                innerSession -> /* run code that releases the lock *\/)
     *     }
     * </pre>
     *
     * @param lockName Identifier used for acquiring lock. Can be any non-null string.
     * @param task The task that will be executed under the acquired lock
     * @param <V> Type of object returned by the {@code task}
     * @param timeToWaitForLock Duration this method waits until it gives up acquiring the lock. If {@code null},
     *                          each implementation should provide some default duration, for example, using
     *                          a configuration option.
     * @return Value returned by the {@code task}
     *
     * @throws LockAcquiringTimeoutException When the method waits for {@code timeToWaitForLock} duration and the lock is still
     *                                       not available to acquire.
     * @throws NullPointerException          When {@code lockName} is {@code null}.
     */
    <V> V withLock(String lockName, Duration timeToWaitForLock, KeycloakSessionTaskWithResult<V> task) throws LockAcquiringTimeoutException;

    /**
     * Releases all locks acquired by this GlobalLockProvider.
     * <p />
     * This method unlocks all existing locks acquired by this provider regardless of the thread
     * or Keycloak instance that originally acquired them.
     */
    void forceReleaseAllLocks();
}
