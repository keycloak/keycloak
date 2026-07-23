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
package org.keycloak.tests.federation.storage;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.common.CustomProvidersServerConfig;
import org.keycloak.tests.providers.federation.ServiceAccountUserStorageFactory;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests that a {@link UserStorageProvider} implementing
 * {@link org.keycloak.storage.user.UserServiceAccountProvider} receives service account
 * users instead of local JPA storage, and that realms without such a provider are unaffected.
 */
@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
public class ServiceAccountUserStorageTest {

    private static final String CLIENT_ID = "external-sa-client";
    private static final String CLIENT_SECRET = "secret";
    private static final String SA_USERNAME = ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + CLIENT_ID;

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectRunOnServer(permittedPackages = {"org.keycloak.tests", "org.keycloak.admin"})
    RunOnServerClient runOnServer;

    private String providerComponentId;

    @BeforeEach
    public void addProvider() {
        ComponentRepresentation provider = new ComponentRepresentation();
        provider.setName("service-account-storage");
        provider.setProviderId(ServiceAccountUserStorageFactory.PROVIDER_ID);
        provider.setProviderType(UserStorageProvider.class.getName());
        provider.setConfig(new MultivaluedHashMap<>());
        provider.getConfig().putSingle("priority", Integer.toString(0));

        providerComponentId = ApiUtil.getCreatedId(managedRealm.admin().components().add(provider));
    }

    @AfterEach
    public void clearProvider() {
        runOnServer.run(session -> {
            ServiceAccountUserStorageFactory factory = (ServiceAccountUserStorageFactory) session.getKeycloakSessionFactory()
                    .getProviderFactory(UserStorageProvider.class, ServiceAccountUserStorageFactory.PROVIDER_ID);
            factory.clear();
        });
    }

    private ClientResource createServiceAccountClient() {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(CLIENT_ID);
        client.setSecret(CLIENT_SECRET);
        client.setServiceAccountsEnabled(true);
        client.setPublicClient(false);
        client.setStandardFlowEnabled(false);
        client.setDirectAccessGrantsEnabled(false);

        String id = ApiUtil.getCreatedId(managedRealm.admin().clients().create(client));
        return managedRealm.admin().clients().get(id);
    }

    @Test
    public void serviceAccountStoredInExternalProvider() {
        ClientResource client = createServiceAccountClient();

        UserRepresentation serviceAccount = client.getServiceAccountUser();
        assertThat(serviceAccount, notNullValue());
        assertThat(serviceAccount.getUsername(), equalTo(SA_USERNAME));
        assertThat("service account user should be stored in the external provider",
                StorageId.isLocalStorage(serviceAccount.getId()), is(false));
        assertThat(StorageId.providerId(serviceAccount.getId()), equalTo(providerComponentId));

        // Verify on the server that the user is in the provider, the client link is persisted,
        // and local JPA storage has no record of it.
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            ClientModel clientModel = realm.getClientByClientId(CLIENT_ID);

            ServiceAccountUserStorageFactory factory = (ServiceAccountUserStorageFactory) session.getKeycloakSessionFactory()
                    .getProviderFactory(UserStorageProvider.class, ServiceAccountUserStorageFactory.PROVIDER_ID);
            Assertions.assertTrue(factory.contains(SA_USERNAME));

            UserModel sa = session.users().getServiceAccount(clientModel);
            Assertions.assertNotNull(sa);
            Assertions.assertEquals(clientModel.getId(), sa.getServiceAccountClientLink());
            Assertions.assertFalse(StorageId.isLocalStorage(sa.getId()));

            Assertions.assertNull(UserStoragePrivateUtil.userLocalStorage(session).getServiceAccount(clientModel));
            Assertions.assertNull(UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(realm, SA_USERNAME));
        });

        // client_credentials grant works against the externally-stored service account.
        oauth.client(CLIENT_ID, CLIENT_SECRET);
        AccessTokenResponse response = oauth.doClientCredentialsGrantAccessTokenRequest();
        assertThat(response.getStatusCode(), equalTo(200));
        AccessToken token = oauth.verifyToken(response.getAccessToken());
        assertThat(token.getOtherClaims().get(ServiceAccountConstants.CLIENT_ID), equalTo(CLIENT_ID));

        // Disabling service accounts removes the user from the external provider.
        ClientRepresentation rep = client.toRepresentation();
        rep.setServiceAccountsEnabled(false);
        client.update(rep);

        assertThrows(BadRequestException.class, client::getServiceAccountUser);

        runOnServer.run(session -> {
            ServiceAccountUserStorageFactory factory = (ServiceAccountUserStorageFactory) session.getKeycloakSessionFactory()
                    .getProviderFactory(UserStorageProvider.class, ServiceAccountUserStorageFactory.PROVIDER_ID);
            Assertions.assertFalse(factory.contains(SA_USERNAME));
        });
    }

    @Test
    public void serviceAccountFallsBackToLocalStorageWithoutProvider() {
        // Remove the provider for this test.
        managedRealm.admin().components().component(providerComponentId).remove();

        ClientResource client = createServiceAccountClient();

        UserRepresentation serviceAccount = client.getServiceAccountUser();
        assertThat(serviceAccount, notNullValue());
        assertThat("without a UserServiceAccountProvider, service accounts should remain in local storage",
                StorageId.isLocalStorage(serviceAccount.getId()), is(true));

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            ClientModel clientModel = realm.getClientByClientId(CLIENT_ID);
            Assertions.assertNotNull(UserStoragePrivateUtil.userLocalStorage(session).getServiceAccount(clientModel));
        });
    }

    @Test
    public void regularUsersNotRoutedToServiceAccountProvider() {
        UserRepresentation user = new UserRepresentation();
        user.setUsername("regular-user");
        user.setEnabled(true);

        String userId;
        try (Response resp = managedRealm.admin().users().create(user)) {
            userId = ApiUtil.getCreatedId(resp);
        }

        assertThat("regular users should not be intercepted by UserServiceAccountProvider",
                StorageId.isLocalStorage(userId), is(true));
    }
}
