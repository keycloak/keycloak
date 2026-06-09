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
package org.keycloak.testsuite.federation.storage;

import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageUtil;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.federation.ServiceAccountUserStorageProviderFactory;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.junit.Before;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ServiceAccountUserStorageTest extends AbstractKeycloakTest {

    private static final String REALM_NAME = "test";
    private static final String CLIENT_SECRET = "secret";
    private static final String DELETE_SERVICE_ACCOUNT_ROLE = "delete-service-account-role";

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(RealmBuilder.create().name(REALM_NAME).build());
    }

    @Before
    public void clearProviderState() {
        testingClient.server().run(session -> {
            ServiceAccountUserStorageProviderFactory factory = (ServiceAccountUserStorageProviderFactory) session.getKeycloakSessionFactory()
                    .getProviderFactory(UserStorageProvider.class, ServiceAccountUserStorageProviderFactory.PROVIDER_ID);
            factory.clear();
        });
    }

    @Test
    public void serviceAccountUsesExternalProvider() {
        String providerId = addServiceAccountProvider("service-account-provider", 0);
        String clientUuid = createServiceAccountClient("external-service-account-client");

        var serviceAccount = realm().clients().get(clientUuid).getServiceAccountUser();

        assertEquals(ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + "external-service-account-client", serviceAccount.getUsername());
        assertTrue(serviceAccount.getId().startsWith("f:" + providerId + ":"));

        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName(REALM_NAME);
            ClientModel client = realm.getClientByClientId("external-service-account-client");

            assertNull(UserStoragePrivateUtil.userLocalStorage(session).getServiceAccount(client));

            UserModel user = session.users().getServiceAccount(client);
            assertNotNull(user);
            assertEquals(providerId, user.getFederationLink());

            ServiceAccountUserStorageProviderFactory factory = (ServiceAccountUserStorageProviderFactory) session.getKeycloakSessionFactory()
                    .getProviderFactory(UserStorageProvider.class, ServiceAccountUserStorageProviderFactory.PROVIDER_ID);
            assertEquals(1, factory.getState(providerId).size());
        });
    }

    @Test
    public void clientCredentialsGrantWorksWithExternalServiceAccount() {
        addServiceAccountProvider("service-account-provider", 0);
        createServiceAccountClient("external-client-credentials-client");

        oauth.client("external-client-credentials-client", CLIENT_SECRET);
        AccessTokenResponse response = oauth.doClientCredentialsGrantAccessTokenRequest();

        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getAccessToken());
    }

    @Test
    public void localStorageIsUsedWhenNoServiceAccountProviderExists() {
        String clientUuid = createServiceAccountClient("local-service-account-client");

        var serviceAccount = realm().clients().get(clientUuid).getServiceAccountUser();

        assertFalse(serviceAccount.getId().startsWith("f:"));

        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName(REALM_NAME);
            ClientModel client = realm.getClientByClientId("local-service-account-client");

            assertNotNull(UserStoragePrivateUtil.userLocalStorage(session).getServiceAccount(client));
        });
    }

    @Test
    public void serviceAccountProvidersAreConsultedInPriorityOrder() {
        String lowPriorityProviderId = addServiceAccountProvider("low-priority-service-account-provider", 10);
        String highPriorityProviderId = addServiceAccountProvider("high-priority-service-account-provider", 0);
        String clientUuid = createServiceAccountClient("priority-service-account-client");

        var serviceAccount = realm().clients().get(clientUuid).getServiceAccountUser();

        assertTrue(serviceAccount.getId().startsWith("f:" + highPriorityProviderId + ":"));

        testingClient.server().run(session -> {
            ServiceAccountUserStorageProviderFactory factory = (ServiceAccountUserStorageProviderFactory) session.getKeycloakSessionFactory()
                    .getProviderFactory(UserStorageProvider.class, ServiceAccountUserStorageProviderFactory.PROVIDER_ID);
            assertEquals(1, factory.getState(highPriorityProviderId).size());
            assertEquals(0, factory.getState(lowPriorityProviderId).size());
        });
    }

    @Test
    public void disablingServiceAccountRemovesExternalUser() {
        String providerId = addServiceAccountProvider("service-account-provider", 0);
        String clientUuid = createServiceAccountClient("disable-service-account-client");

        assertNotNull(realm().clients().get(clientUuid).getServiceAccountUser());

        ClientResource clientResource = realm().clients().get(clientUuid);
        ClientRepresentation client = clientResource.toRepresentation();
        client.setServiceAccountsEnabled(false);
        clientResource.update(client);

        testingClient.server().run(session -> {
            ServiceAccountUserStorageProviderFactory factory = (ServiceAccountUserStorageProviderFactory) session.getKeycloakSessionFactory()
                    .getProviderFactory(UserStorageProvider.class, ServiceAccountUserStorageProviderFactory.PROVIDER_ID);
            assertEquals(0, factory.getState(providerId).size());
        });
    }

    @Test
    public void deletingClientRemovesExternalServiceAccountAndFederatedData() {
        String providerId = addServiceAccountProvider("service-account-provider", 0);
        String clientUuid = createServiceAccountClient("delete-service-account-client");

        assertNotNull(realm().clients().get(clientUuid).getServiceAccountUser());

        String userId = testingClient.server().fetchString(session -> {
            RealmModel realm = session.realms().getRealmByName(REALM_NAME);
            ClientModel client = realm.getClientByClientId("delete-service-account-client");
            UserModel user = session.users().getServiceAccount(client);
            RoleModel role = realm.addRole(DELETE_SERVICE_ACCOUNT_ROLE);

            user.grantRole(role);
            assertTrue(user.hasRole(role));

            return user.getId();
        });

        realm().clients().get(clientUuid).remove();

        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName(REALM_NAME);

            assertTrue(UserStorageUtil.userFederatedStorage(session).getRoleMappingsStream(realm, userId).findAny().isEmpty());

            ServiceAccountUserStorageProviderFactory factory = (ServiceAccountUserStorageProviderFactory) session.getKeycloakSessionFactory()
                    .getProviderFactory(UserStorageProvider.class, ServiceAccountUserStorageProviderFactory.PROVIDER_ID);
            assertEquals(0, factory.getState(providerId).size());

            RoleModel role = realm.getRole(DELETE_SERVICE_ACCOUNT_ROLE);
            if (role != null) {
                realm.removeRole(role);
            }
        });
    }

    private RealmResource realm() {
        return adminClient.realm(REALM_NAME);
    }

    private String addServiceAccountProvider(String name, int priority) {
        ComponentRepresentation provider = new ComponentRepresentation();
        provider.setName(name);
        provider.setProviderId(ServiceAccountUserStorageProviderFactory.PROVIDER_ID);
        provider.setProviderType(UserStorageProvider.class.getName());
        provider.setConfig(new MultivaluedHashMap<>());
        provider.getConfig().putSingle("priority", Integer.toString(priority));

        return UserStorageTest.addComponent(realm(), getCleanup(), provider);
    }

    private String createServiceAccountClient(String clientId) {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(clientId);
        client.setName(clientId);
        client.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        client.setPublicClient(false);
        client.setSecret(CLIENT_SECRET);
        client.setEnabled(true);
        client.setServiceAccountsEnabled(true);

        Response response = realm().clients().create(client);
        String id = ApiUtil.getCreatedId(response);
        response.close();

        getCleanup().addClientUuid(id);

        return id;
    }
}
