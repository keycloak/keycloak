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

import org.keycloak.provider.Provider;

import java.time.Duration;

public interface GlobalLockProvider extends Provider {

    /**
     * Effectively the same as {@code acquire(lockName, null)}
     * <p />
     * This method is intended to be used in a {@code try}-with-resources block.
     *
     * @param lockName Identifier used for acquiring lock. Can be any non-null string.
     * @return Instance of {@link GlobalLock} representing successfully acquired global lock.
     * @throws LockAcquiringTimeoutException When acquiring the global lock times out
     *                          (see Javadoc of {@link #acquire(String, Duration)} for more details on how the time
     *                          duration is determined)
     * @throws NullPointerException When lockName is {@code null}.
     */
    default GlobalLock acquireLock(String lockName) throws LockAcquiringTimeoutException {
        return acquire(lockName, null);
    }

    /**
     * Acquires a new global lock that is visible to all Keycloak nodes. The lock is non-reentrant.
     * <p />
     * The lock is guaranteed to be kept until the returned {@link GlobalLock} is closed
     * using the {@link GlobalLock#close} method.
     * <p />
     * Some implementations may benefit from locks that are released at the end of transaction.
     * For this purpose, the lifespan of the returned lock is limited by the transaction lifespan
     * of the session which acquired this lock.
     * <p />
     * This method is intended to be used in a {@code try}-with-resources block.
     * <p />
     * If there is another global lock with the same identifier ({@code lockName}) already acquired, this method waits
     * until the lock is released, however, not more than {@code timeToWaitForLock} duration. If the lock is not
     * acquired after {@code timeToWaitForLock} duration, the method throws {@link LockAcquiringTimeoutException}
     * <p />
     * Releasing of the lock is done using instance of {@link GlobalLock} returned by this method.
     *
     * @param lockName Identifier used for acquiring lock. Can be any non-null string.
     * @param timeToWaitForLock Duration this method waits until it gives up acquiring the lock. If {@code null},
     *                          each implementation should provide some default duration, for example using
     *                          configuration option.
     * @return Instance of {@link GlobalLock} representing successfully acquired global lock.
     *
     * @throws LockAcquiringTimeoutException When the method waits for {@code timeToWaitForLock} duration and the lock is still
     *                          not available to acquire.
     * @throws NullPointerException When {@code lockName} is {@code null}.
     */
    GlobalLock acquire(String lockName, Duration timeToWaitForLock) throws LockAcquiringTimeoutException;

    /**
     * Releases all locks acquired by this GlobalLockProvider.
     * <p />
     * This method must unlock all existing locks acquired by this provider regardless of the thread
     * or Keycloak instance that originally acquired them.
     */
    void forceReleaseAllLocks();
}
