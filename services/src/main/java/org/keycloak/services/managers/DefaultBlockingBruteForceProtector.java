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

    private final Map<String, String> loginAttempts = Collections.synchronizedMap(new LinkedHashMap(100, DEFAULT_LOAD_FACTOR) {
        @Override
        protected boolean removeEldestEntry(Entry eldest) {
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

        if (isCurrentLoginAttempt(user)) {
            return !tryEnlistBlockingTransaction(session, user);
        }

        return true;
    }

    // Return true if this thread successfully enlisted itself
    private boolean tryEnlistBlockingTransaction(KeycloakSession session, UserModel user) {
        String threadInProgress = loginAttempts.computeIfAbsent(user.getId(), k -> getThreadName());

        // This means that this thread successfully added itself into the map. We can enlist transaction just in that case
        if (threadInProgress.equals(getThreadName())) {
            session.getTransactionManager().enlistAfterCompletion(new AbstractKeycloakTransaction() {
                @Override
                protected void commitImpl() {
                    unblock();
                }

                @Override
                protected void rollbackImpl() {
                    unblock();
                }

                private void unblock() {
                    loginAttempts.remove(user.getId());
                }
            });

            return true;
        } else {
            return false;
        }
    }

    private boolean isCurrentLoginAttempt(UserModel user) {
        return loginAttempts.getOrDefault(user.getId(), getThreadName()).equals(getThreadName());
    }

    private String getThreadName() {
        return Thread.currentThread().getName();
    }
}
