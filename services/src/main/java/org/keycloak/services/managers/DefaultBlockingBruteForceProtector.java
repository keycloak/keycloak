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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.ws.rs.core.UriInfo;

import org.keycloak.common.ClientConnection;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

public class DefaultBlockingBruteForceProtector extends DefaultBruteForceProtector {

    // make this configurable?
    private static final int DEFAULT_MAX_CONCURRENT_ATTEMPTS = 1000;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;
    private static final String OFF_THREAD_STARTED = "#brute_force_started";

    private final Map<String, String> loginAttempts = Collections.synchronizedMap(new LinkedHashMap<>(100, DEFAULT_LOAD_FACTOR) {
        @Override
        protected boolean removeEldestEntry(Entry<String, String> eldest) {
            return loginAttempts.size() > DEFAULT_MAX_CONCURRENT_ATTEMPTS;
        }
    });

    DefaultBlockingBruteForceProtector(KeycloakSessionFactory factory) {
        super(factory);
    }

    @Override
    public boolean isPermanentlyLockedOut(KeycloakSession session, RealmModel realm, UserModel user) {
        if (super.isPermanentlyLockedOut(session, realm, user)) {
            return true;
        }

        if (!realm.isPermanentLockout()) return false;

        return isLoginInProgress(session, user);
    }

    @Override
    public boolean isTemporarilyDisabled(KeycloakSession session, RealmModel realm, UserModel user) {
        if (super.isTemporarilyDisabled(session, realm, user)) {
            return true;
        }

        return isLoginInProgress(session, user);
    }

    private boolean isLoginInProgress(KeycloakSession session, UserModel user) {
        AuthenticationSessionModel authSession = session.getContext().getAuthenticationSession();

        if (authSession == null) {
            // not authenticating as there is no auth session bound to the session
            return false;
        }

        return !tryEnlistBlockingTransactionOrSameThread(session, user);
    }

    // Return true if this thread successfully enlisted itself or it was already done by the same thread
    private boolean tryEnlistBlockingTransactionOrSameThread(KeycloakSession session, UserModel user) {
        AtomicBoolean inserted = new AtomicBoolean(false);
        String threadInProgress = loginAttempts.computeIfAbsent(user.getId(), k -> {
            inserted.set(true);
            return getThreadName();
        });

        // This means that this thread successfully added itself into the map. We can enlist transaction just in that case
        if (inserted.get()) {
            session.getTransactionManager().enlistAfterCompletion(new AbstractKeycloakTransaction() {
                @Override
                protected void commitImpl() {
                    // remove or wait the brute force thread to finish
                    loginAttempts.computeIfPresent(user.getId(), (k, v) -> v.endsWith(OFF_THREAD_STARTED)? "" : null);
                }

                @Override
                protected void rollbackImpl() {
                    // remove on rollback
                    loginAttempts.remove(user.getId());
                }
            });

            return true;
        } else {
            return isCurrentThread(threadInProgress);
        }
    }

    private boolean isCurrentThread(String name) {
        return name.equals(getThreadName()) || name.equals(getThreadName() + OFF_THREAD_STARTED);
    }

    private String getThreadName() {
        return Thread.currentThread().getName();
    }

    private void enlistRemoval(KeycloakSession session, String userId) {
        session.getTransactionManager().enlistAfterCompletion(new AbstractKeycloakTransaction() {
            @Override
            protected void commitImpl() {
                // remove or wait the main thread to finish
                loginAttempts.computeIfPresent(userId, (k, v) -> v.isEmpty()? null : v.substring(0, v.length() - OFF_THREAD_STARTED.length()));
            }

            @Override
            protected void rollbackImpl() {
                loginAttempts.remove(userId);
            }
        });
    }

    @Override
    protected void processLogin(RealmModel realm, UserModel user, ClientConnection clientConnection, UriInfo uriInfo, boolean success) {
        // mark the off-thread is started for this request
        loginAttempts.computeIfPresent(user.getId(), (k, v) -> v + OFF_THREAD_STARTED);
        super.processLogin(realm, user, clientConnection, uriInfo, success);
    }

    @Override
    protected void failure(KeycloakSession session, RealmModel realm, String userId, String remoteAddr, long failureTime) {
        // remove the user from concurrent login attemps once it's processed
        enlistRemoval(session, userId);
        super.failure(session, realm, userId, remoteAddr, failureTime);
    }

    @Override
    protected void success(KeycloakSession session, RealmModel realm, String userId) {
        // remove the user from concurrent login attemps once it's processed
        enlistRemoval(session, userId);
        super.success(session, realm, userId);
    }
}
