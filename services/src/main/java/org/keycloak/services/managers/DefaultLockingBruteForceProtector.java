/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.services.managers;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;

import jakarta.ws.rs.core.UriInfo;

import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Time;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.UserModel;

/**
 * Brute force protector that serializes login attempts per user within a JVM using a {@link Semaphore}.
 *
 * <p>The database row for login failures is only locked with {@code PESSIMISTIC_WRITE} when a write occurs
 * (see {@link org.keycloak.loginfailures.jpa.UserLoginFailureAdapter#ensureLocked()}), not on reads.
 * Without additional coordination, parallel login attempts could all read the same (stale) failure count,
 * allowing more attempts than the configured limit. The JVM-level lock prevents this by ensuring only one
 * thread per user per node reads and updates the failure state at a time.
 *
 * <p>This is a performance trade-off: acquiring a database lock on every read (including
 * {@link #isTemporarilyDisabled} checks on every login) would be expensive. The JVM-level lock avoids that
 * cost while still providing correctness within a single node. In a cluster, the upper bound for concurrent
 * attempts that may bypass the check equals the number of nodes, as each node maintains its own lock map.
 * The database pessimistic write lock still guarantees that no failure count update is lost.
 *
 * <p>A database-only locking approach would also require inserting a login failure row on the first login
 * of every user (before knowing if it is a success or failure) in order to have a row to lock. This would
 * significantly increase the number of rows and the IOPS on the login failure table.
 *
 * <p>This improves on the base {@link DefaultBruteForceProtector}, which processes failures asynchronously
 * and would reject concurrent login attempts entirely during that window.
 */
public class DefaultLockingBruteForceProtector extends DefaultBruteForceProtector {

    static class UserLock {
        final Semaphore semaphore = new Semaphore(1);
        int refCount;
    }

    private final KeycloakSession session;
    private final ConcurrentMap<String, UserLock> userLocks;
    private final Set<String> lockedUserIds = new HashSet<>();
    private boolean transactionEnlisted;

    public DefaultLockingBruteForceProtector(KeycloakSessionFactory factory, KeycloakSession session, ConcurrentMap<String, UserLock> userLocks) {
        super(factory);
        this.session = session;
        this.userLocks = userLocks;
    }

    @Override
    protected void processLogin(RealmModel realm, UserModel user, ClientConnection clientConnection, UriInfo uriInfo, boolean success, Set<String> categories) {
        if (success) {
            success(session, realm, user.getId(), categories);
        } else {
            failure(session, realm, user.getId(), clientConnection.getRemoteHost(), Time.currentTimeMillis(), categories);
        }
    }

    @Override
    protected UserLoginFailureModel getUserFailureModel(KeycloakSession session, RealmModel realm, String userId) {
        if (realm == null) return null;
        acquireLock(session, userId);
        return super.getUserFailureModel(session, realm, userId);
    }

    private void acquireLock(KeycloakSession session, String userId) {
        if (lockedUserIds.contains(userId)) {
            return;
        }

        UserLock lock = userLocks.compute(userId, (k, existing) -> {
            if (existing == null) {
                existing = new UserLock();
            }
            existing.refCount++;
            return existing;
        });

        lock.semaphore.acquireUninterruptibly();
        lockedUserIds.add(userId);

        if (!transactionEnlisted) {
            transactionEnlisted = true;
            session.getTransactionManager().enlistAfterCompletion(new AbstractKeycloakTransaction() {
                @Override
                protected void commitImpl() {
                    releaseAllLocks();
                }

                @Override
                protected void rollbackImpl() {
                    releaseAllLocks();
                }
            });
        }
    }

    private void releaseAllLocks() {
        for (String userId : lockedUserIds) {
            userLocks.compute(userId, (k, lock) -> {
                if (lock == null) return null;
                lock.semaphore.release();
                lock.refCount--;
                return lock.refCount == 0 ? null : lock;
            });
        }
    }
}
