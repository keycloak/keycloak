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

package org.keycloak.tests.providers.federation;

import java.util.concurrent.atomic.AtomicInteger;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;
import org.keycloak.storage.user.UserLookupProvider;

public class RollbackOnlyUserStorageProviderFactory implements UserStorageProviderFactory<RollbackOnlyUserStorageProviderFactory.Provider> {

    public static final String PROVIDER_ID = "rollback-only-user-storage";
    public static final String USERNAME = "rollback-user";
    private static final AtomicInteger ROLLBACK_ONLY_FAILURES = new AtomicInteger();

    public static int getRollbackOnlyFailures() {
        return ROLLBACK_ONLY_FAILURES.get();
    }

    public static void resetRollbackOnlyFailures() {
        ROLLBACK_ONLY_FAILURES.set(0);
    }

    @Override
    public Provider create(KeycloakSession session, ComponentModel model) {
        return new Provider(session, model);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    public static class Provider implements UserStorageProvider, UserLookupProvider {

        private final KeycloakSession session;
        private final ComponentModel model;

        private Provider(KeycloakSession session, ComponentModel model) {
            this.session = session;
            this.model = model;
        }

        @Override
        public UserModel getUserById(RealmModel realm, String id) {
            failIfEnabled();
            return USERNAME.equals(StorageId.externalId(id)) ? createUser(realm) : null;
        }

        @Override
        public UserModel getUserByUsername(RealmModel realm, String username) {
            return USERNAME.equals(username) ? createUser(realm) : null;
        }

        @Override
        public UserModel getUserByEmail(RealmModel realm, String email) {
            return null;
        }

        private UserModel createUser(RealmModel realm) {
            return new AbstractUserAdapterFederatedStorage.Streams(session, realm, model) {
                @Override
                public String getUsername() {
                    return USERNAME;
                }

                @Override
                public void setUsername(String username) {
                }
            };
        }

        private void failIfEnabled() {
            if (Boolean.parseBoolean(model.getConfig().getFirst("fail"))) {
                ROLLBACK_ONLY_FAILURES.incrementAndGet();
                session.getTransactionManager().setRollbackOnly();
                throw new RuntimeException("FORCED ROLLBACK-ONLY USER LOOKUP FAILURE");
            }
        }

        @Override
        public void close() {
        }
    }
}
