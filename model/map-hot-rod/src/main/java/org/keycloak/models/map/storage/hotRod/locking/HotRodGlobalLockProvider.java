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

package org.keycloak.models.map.storage.hotRod.locking;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionTaskWithResult;
import org.keycloak.models.locking.GlobalLockProvider;
import org.keycloak.models.locking.LockAcquiringTimeoutException;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.time.Duration;
import java.util.Objects;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;


public class HotRodGlobalLockProvider implements GlobalLockProvider {

    private static final Logger LOG = Logger.getLogger(HotRodGlobalLockProvider.class);
    private final KeycloakSession session;
    private final RemoteCache<String, String> locksCache;
    private final long defaultTimeoutMilliseconds;

    public HotRodGlobalLockProvider(KeycloakSession session, RemoteCache<String, String> locksCache, long defaultTimeoutMilliseconds) {
        this.locksCache = locksCache;
        this.defaultTimeoutMilliseconds = defaultTimeoutMilliseconds;
        this.session = session;
    }

    @Override
    public <V> V withLock(String lockName, Duration timeToWaitForLock, KeycloakSessionTaskWithResult<V> task) throws LockAcquiringTimeoutException {
        Objects.requireNonNull(lockName, "lockName cannot be null");

        if (timeToWaitForLock == null) {
            // Set default timeout if null provided
            timeToWaitForLock = Duration.ofMillis(defaultTimeoutMilliseconds);
        }

        try {
            LOG.debugf("Acquiring lock [%s].%s", lockName, getShortStackTrace());
            HotRodLocksUtils.repeatPutIfAbsent(locksCache, lockName, timeToWaitForLock, 50);
            LOG.debugf("Lock acquired [%s]. Continuing with task execution.", lockName);

            return KeycloakModelUtils.runJobInTransactionWithResult(session.getKeycloakSessionFactory(), task);
        } finally {
            LOG.debugf("Releasing lock [%s].%s", lockName, getShortStackTrace());
            boolean result = HotRodLocksUtils.removeWithInstanceIdentifier(locksCache, lockName);
            LOG.debugf("Lock [%s] release resulted with %s", lockName, result);
        }
    }

    @Override
    public void forceReleaseAllLocks() {
        locksCache.clear();
    }

    @Override
    public void close() {
    }
}
