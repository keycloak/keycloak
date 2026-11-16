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

import jakarta.ws.rs.core.Response;

import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.util.ApiUtil;

import org.infinispan.Cache;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@code UserCacheSession.getUserByFederatedIdentity()} and {@code getServiceAccount()} cache their
 * lookup query even when the resolved user was already managed in the session through a different, earlier lookup.
 * See https://github.com/keycloak/keycloak/issues/55237.
 */
@KeycloakIntegrationTest
public class UserCacheManagedUserQueryTest {

    private static final String IDP_ALIAS = "oidc";
    private static final String USERNAME = "broker-user";
    private static final String BROKER_USER_ID = "broker-user-id";

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void federatedIdentityQueryIsCachedWhenUserAlreadyManaged() {
        String realmName = managedRealm.getName();

        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setAlias(IDP_ALIAS);
        idp.setProviderId("oidc");
        idp.setEnabled(true);
        managedRealm.admin().identityProviders().create(idp).close();

        UserRepresentation user = new UserRepresentation();
        user.setUsername(USERNAME);
        String userId;
        try (Response response = managedRealm.admin().users().create(user)) {
            userId = ApiUtil.getCreatedId(response);
        }

        FederatedIdentityRepresentation link = new FederatedIdentityRepresentation();
        link.setIdentityProvider(IDP_ALIAS);
        link.setUserId(BROKER_USER_ID);
        link.setUserName(USERNAME);
        try (Response response = managedRealm.admin().users().get(userId).addFederatedIdentity(IDP_ALIAS, link)) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realm);

            // first lookup manages the user via the username key
            UserModel byUsername = session.users().getUserByUsername(realm, USERNAME);
            assertNotNull(byUsername, "user should be found by username");

            // second lookup resolves the same, already-managed user via the federated identity key
            FederatedIdentityModel socialLink = new FederatedIdentityModel(IDP_ALIAS, BROKER_USER_ID, USERNAME);
            UserModel byFederatedIdentity = session.users().getUserByFederatedIdentity(realm, socialLink);
            assertNotNull(byFederatedIdentity, "user should be found by its federated identity");
            assertEquals(byUsername.getId(), byFederatedIdentity.getId());

            String cacheKey = realm.getId() + ".idp." + IDP_ALIAS + "." + BROKER_USER_ID;
            Cache<Object, Object> cache = session.getProvider(InfinispanConnectionProvider.class)
                    .getCache(InfinispanConnectionProvider.USER_CACHE_NAME);
            assertTrue(cache.containsKey(cacheKey),
                    "federated identity query must be cached even though the user was already managed in the session");
        });
    }

    @Test
    public void serviceAccountQueryIsCachedWhenUserAlreadyManaged() {
        String realmName = managedRealm.getName();
        String clientId = "service-client";

        String createdClientId;
        String serviceAccountUserId;
        try (Response response = managedRealm.admin().clients().create(ClientBuilder.create(clientId).serviceAccountsEnabled().build())) {
            createdClientId = ApiUtil.getCreatedId(response);
        }
        managedRealm.cleanup().add(r -> r.clients().get(createdClientId).remove());
        serviceAccountUserId = managedRealm.admin().clients().get(createdClientId).toRepresentation().getServiceAccountUserId();

        String serviceAccountUsername = (ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + clientId).toLowerCase();

        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realm);

            ClientModel client = realm.getClientByClientId(clientId);
            assertNotNull(client, "client should exist");

            String cacheKey = realm.getId() + ".username." + serviceAccountUsername;
            Cache<Object, Object> cache = session.getProvider(InfinispanConnectionProvider.class)
                    .getCache(InfinispanConnectionProvider.USER_CACHE_NAME);
            // the client representation fetched above already called getServiceAccount() once and cached this
            // query; remove it so this test actually exercises the managed-user cache-miss path below
            cache.remove(cacheKey);

            // first lookup manages the user via its id, without touching the username cache key
            UserModel byId = session.users().getUserById(realm, serviceAccountUserId);
            assertNotNull(byId, "service account user should exist");

            // second lookup resolves the same, already-managed user via the service account username key
            UserModel byServiceAccount = session.users().getServiceAccount(client);
            assertNotNull(byServiceAccount, "service account user should be found via getServiceAccount");
            assertEquals(byId.getId(), byServiceAccount.getId());

            assertTrue(cache.containsKey(cacheKey),
                    "service account username query must be cached even though the user was already managed in the session");
        });
    }
}
