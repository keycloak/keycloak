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
package org.keycloak.tests.model;

import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.providers.federation.UserMapStorageFactory;

import org.infinispan.Cache;
import org.junit.jupiter.api.Test;

import static org.keycloak.storage.UserStorageProviderModel.IMPORT_ENABLED;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@code UserCacheSession.updateFederatedIdentity()} invalidates both the old and new reverse lookup
 * cache keys when the broker user id of a federated identity link changes for an externally stored user.
 * <p>
 * Unlike local users, whose broker user id is immutable through {@code UserProvider.updateFederatedIdentity()} (see
 * {@code JpaUserProvider#updateFederatedIdentity}), users stored via {@code UserFederatedStorageProvider} can have
 * their broker user id changed through that same call (see
 * {@code JpaUserFederatedStorageProvider#updateFederatedIdentity}).
 * See https://github.com/keycloak/keycloak/pull/55240.
 */
@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
public class UserFederatedStorageUpdateFederatedIdentityTest {

    private static final String IDP_ALIAS_COMMIT = "oidc-fed-update-commit";
    private static final String USERNAME_COMMIT = "fed-user-update-commit";
    private static final String OLD_BROKER_USER_ID_COMMIT = "old-broker-id-commit";
    private static final String NEW_BROKER_USER_ID_COMMIT = "new-broker-id-commit";

    private static final String IDP_ALIAS_ROLLBACK = "oidc-fed-update-rollback";
    private static final String USERNAME_ROLLBACK = "fed-user-update-rollback";
    private static final String OLD_BROKER_USER_ID_ROLLBACK = "old-broker-id-rollback";
    private static final String NEW_BROKER_USER_ID_ROLLBACK = "new-broker-id-rollback";

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void updateFederatedIdentityInvalidatesOldAndNewReverseLookupKeysWhenBrokerIdChanges() {
        String realmName = managedRealm.getName();
        createIdp(IDP_ALIAS_COMMIT);

        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realm);

            UserModel user = addExternallyStoredUser(session, realm, "fed-storage-commit", USERNAME_COMMIT);
            session.users().addFederatedIdentity(realm, user,
                    new FederatedIdentityModel(IDP_ALIAS_COMMIT, OLD_BROKER_USER_ID_COMMIT, USERNAME_COMMIT));
        });

        // populate and commit the old reverse lookup cache entry in its own transaction
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realm);

            UserModel byOldId = session.users().getUserByFederatedIdentity(realm,
                    new FederatedIdentityModel(IDP_ALIAS_COMMIT, OLD_BROKER_USER_ID_COMMIT, USERNAME_COMMIT));
            assertNotNull(byOldId, "user should be found by its original broker id");

            assertTrue(userCache(session).containsKey(realm.getId() + ".idp." + IDP_ALIAS_COMMIT + "." + OLD_BROKER_USER_ID_COMMIT),
                    "reverse lookup query for the original broker id should be cached");
        });

        // the IDP now reports a different id for the same external account; none of Keycloak's own callers trigger
        // this through updateFederatedIdentity (they go through remove+add instead, see
        // IdentityBrokerService#migrateFederatedIdentityId), but the UserFederatedStorageProvider contract allows it
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realm);

            UserModel user = session.users().getUserByFederatedIdentity(realm,
                    new FederatedIdentityModel(IDP_ALIAS_COMMIT, OLD_BROKER_USER_ID_COMMIT, USERNAME_COMMIT));
            session.users().updateFederatedIdentity(realm, user,
                    new FederatedIdentityModel(IDP_ALIAS_COMMIT, NEW_BROKER_USER_ID_COMMIT, USERNAME_COMMIT));
        });

        // the invalidation triggered by the update is only applied to the cache once its own transaction commits
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realm);

            UserModel byOldId = session.users().getUserByFederatedIdentity(realm,
                    new FederatedIdentityModel(IDP_ALIAS_COMMIT, OLD_BROKER_USER_ID_COMMIT, USERNAME_COMMIT));
            assertNull(byOldId, "stale reverse lookup for the old broker id must not be served from cache");

            UserModel byNewId = session.users().getUserByFederatedIdentity(realm,
                    new FederatedIdentityModel(IDP_ALIAS_COMMIT, NEW_BROKER_USER_ID_COMMIT, USERNAME_COMMIT));
            assertNotNull(byNewId, "user should be found by its new broker id");
        });
    }

    @Test
    public void updateFederatedIdentityRollbackDoesNotLeaveStaleNewReverseLookupKey() {
        String realmName = managedRealm.getName();
        createIdp(IDP_ALIAS_ROLLBACK);

        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realm);

            UserModel user = addExternallyStoredUser(session, realm, "fed-storage-rollback", USERNAME_ROLLBACK);
            session.users().addFederatedIdentity(realm, user,
                    new FederatedIdentityModel(IDP_ALIAS_ROLLBACK, OLD_BROKER_USER_ID_ROLLBACK, USERNAME_ROLLBACK));
        });

        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realm);

            // first lookup manages the user via the old broker id key
            UserModel user = session.users().getUserByFederatedIdentity(realm,
                    new FederatedIdentityModel(IDP_ALIAS_ROLLBACK, OLD_BROKER_USER_ID_ROLLBACK, USERNAME_ROLLBACK));
            assertNotNull(user, "user should be found by its original broker id");

            session.users().updateFederatedIdentity(realm, user,
                    new FederatedIdentityModel(IDP_ALIAS_ROLLBACK, NEW_BROKER_USER_ID_ROLLBACK, USERNAME_ROLLBACK));

            // looking the new broker id up while its updating transaction is still open must not leave a stale
            // cache entry behind once the transaction is rolled back
            UserModel byNewId = session.users().getUserByFederatedIdentity(realm,
                    new FederatedIdentityModel(IDP_ALIAS_ROLLBACK, NEW_BROKER_USER_ID_ROLLBACK, USERNAME_ROLLBACK));
            assertNotNull(byNewId, "updated link should be visible within its own transaction");

            session.getTransactionManager().setRollbackOnly();
        });

        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realm);

            UserModel byNewId = session.users().getUserByFederatedIdentity(realm,
                    new FederatedIdentityModel(IDP_ALIAS_ROLLBACK, NEW_BROKER_USER_ID_ROLLBACK, USERNAME_ROLLBACK));
            assertNull(byNewId, "new broker id must not resolve after the updating transaction was rolled back");

            UserModel byOldId = session.users().getUserByFederatedIdentity(realm,
                    new FederatedIdentityModel(IDP_ALIAS_ROLLBACK, OLD_BROKER_USER_ID_ROLLBACK, USERNAME_ROLLBACK));
            assertNotNull(byOldId, "original link should still resolve since the update was rolled back");
        });
    }

    private void createIdp(String alias) {
        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setAlias(alias);
        idp.setProviderId("oidc");
        idp.setEnabled(true);
        managedRealm.admin().identityProviders().create(idp).close();
    }

    private static UserModel addExternallyStoredUser(KeycloakSession session, RealmModel realm, String componentName, String username) {
        UserStorageProviderModel model = new UserStorageProviderModel();
        model.setName(componentName);
        model.setProviderId(UserMapStorageFactory.PROVIDER_ID);
        model.setParentId(realm.getId());
        model.getConfig().putSingle(IMPORT_ENABLED, Boolean.toString(false));
        realm.addComponentModel(model);

        UserModel user = session.users().addUser(realm, username);
        assertFalse(StorageId.isLocalStorage(user.getId()), "user should be externally stored");
        return user;
    }

    private static Cache<Object, Object> userCache(KeycloakSession session) {
        return session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.USER_CACHE_NAME);
    }
}
