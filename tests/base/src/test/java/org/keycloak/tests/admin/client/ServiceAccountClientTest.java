/*
 * Copyright 2024 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.tests.admin.client;

import java.util.stream.Collectors;

import jakarta.ws.rs.ForbiddenException;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author rmartinc
 */
@KeycloakIntegrationTest
public class ServiceAccountClientTest {

    private static final String clientId = "service-account-client";
    private static final String VIEW_CLIENTS_USER = "view-clients-user";
    private static final String PASSWORD = "password";

    @InjectRealm(config = ServiceAccountRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectClient(config = ServiceAccountClientConfig.class)
    ManagedClient managedClient;

    @InjectOAuthClient
    OAuthClient oAuthClient;

    @InjectAdminClientFactory
    AdminClientFactory adminClientFactory;

    @Test
    public void testServiceAccountEnableDisable() {
        org.keycloak.admin.client.resource.ClientScopeResource serviceAccountScopeRsc = AdminApiUtil.findClientScopeByName(
                managedRealm.admin(), ServiceAccountConstants.SERVICE_ACCOUNT_SCOPE);
        Assertions.assertNotNull(serviceAccountScopeRsc);
        ClientScopeRepresentation serviceAccountScope = serviceAccountScopeRsc.toRepresentation();

        MatcherAssert.assertThat(
                managedClient.admin().getDefaultClientScopes().stream()
                        .map(ClientScopeRepresentation::getName)
                        .collect(Collectors.toList()),
                Matchers.hasItem("service_account")
        );

        // perform a login and check the claims are there
        oAuthClient.client(clientId, "password");
        AccessTokenResponse response = oAuthClient.doClientCredentialsGrantAccessTokenRequest();
        AccessToken accessToken = oAuthClient.verifyToken(response.getAccessToken());
        Assertions.assertEquals(clientId, accessToken.getOtherClaims().get(ServiceAccountConstants.CLIENT_ID));
        Assertions.assertNotNull(accessToken.getOtherClaims().get(ServiceAccountConstants.CLIENT_HOST));
        Assertions.assertNotNull(accessToken.getOtherClaims().get(ServiceAccountConstants.CLIENT_ADDRESS));

        // update the client to remove service account
        managedClient.updateWithCleanup(c -> c.serviceAccountsEnabled(false));
        MatcherAssert.assertThat(
                managedClient.admin().getDefaultClientScopes().stream()
                        .map(ClientScopeRepresentation::getName)
                        .collect(Collectors.toList()),
                Matchers.not(Matchers.hasItem(ServiceAccountConstants.SERVICE_ACCOUNT_SCOPE))
        );
        response = oAuthClient.doClientCredentialsGrantAccessTokenRequest();
        Assertions.assertEquals("unauthorized_client", response.getError());

        // re-enable sevice accounts
        managedClient.updateWithCleanup(c -> c.serviceAccountsEnabled(true));
        MatcherAssert.assertThat(
                managedClient.admin().getDefaultClientScopes().stream()
                        .map(ClientScopeRepresentation::getName)
                        .collect(Collectors.toList()),
                Matchers.hasItem(ServiceAccountConstants.SERVICE_ACCOUNT_SCOPE)
        );
        response = oAuthClient.doClientCredentialsGrantAccessTokenRequest();
        accessToken = oAuthClient.verifyToken(response.getAccessToken());
        Assertions.assertEquals("service-account-client", accessToken.getOtherClaims().get(ServiceAccountConstants.CLIENT_ID));
        Assertions.assertNotNull(accessToken.getOtherClaims().get(ServiceAccountConstants.CLIENT_HOST));
        Assertions.assertNotNull(accessToken.getOtherClaims().get(ServiceAccountConstants.CLIENT_ADDRESS));

        // assign the scope as optional
        managedClient.admin().removeDefaultClientScope(serviceAccountScope.getId());
        managedClient.admin().addOptionalClientScope(serviceAccountScope.getId());

        // re-enable service accounts, should assign the scope again as default
        managedClient.updateWithCleanup(c -> c.serviceAccountsEnabled(true));
        MatcherAssert.assertThat(
                managedClient.admin().getDefaultClientScopes().stream()
                        .map(ClientScopeRepresentation::getName)
                        .collect(Collectors.toList()),
                Matchers.hasItem(ServiceAccountConstants.SERVICE_ACCOUNT_SCOPE)
        );
        response = oAuthClient.doClientCredentialsGrantAccessTokenRequest();
        accessToken = oAuthClient.verifyToken(response.getAccessToken());
        Assertions.assertEquals("service-account-client", accessToken.getOtherClaims().get(ServiceAccountConstants.CLIENT_ID));
        Assertions.assertNotNull(accessToken.getOtherClaims().get(ServiceAccountConstants.CLIENT_HOST));
        Assertions.assertNotNull(accessToken.getOtherClaims().get(ServiceAccountConstants.CLIENT_ADDRESS));

        // remove the service account and client credentials should fail
        String serviceAccountUserId = managedClient.admin().toRepresentation().getServiceAccountUserId();
        managedRealm.admin().users().delete(serviceAccountUserId).close();
        response = oAuthClient.doClientCredentialsGrantAccessTokenRequest();
        Assertions.assertEquals("invalid_request", response.getError());
    }

    @Test
    public void testServiceAccountUserIdVisibleWithViewClientsOnly() {
        try (Keycloak viewClientsAdmin = adminClientFactory.create()
                .realm(managedRealm.getName())
                .username(VIEW_CLIENTS_USER)
                .password(PASSWORD)
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .build()) {

            ClientRepresentation client = viewClientsAdmin.realm(managedRealm.getName())
                    .clients().findByClientId(clientId).get(0);
            ClientResource clientResource = viewClientsAdmin.realm(managedRealm.getName())
                    .clients().get(client.getId());

            ClientRepresentation fullClient = clientResource.toRepresentation();
            Assertions.assertNotNull(fullClient.getServiceAccountUserId());

            Assertions.assertThrows(ForbiddenException.class, () ->
                    viewClientsAdmin.realm(managedRealm.getName())
                            .users().get(fullClient.getServiceAccountUserId()).toRepresentation());
        }
    }

    private static class ServiceAccountClientConfig implements ClientConfig {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client
                    .clientId(clientId)
                    .protocol("openid-connect")
                    .secret("password")
                    .serviceAccountsEnabled(true)
                    .authenticatorType("client-secret")
                    .publicClient(false);
        }
    }

    private static class ServiceAccountRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.users(UserBuilder.create(VIEW_CLIENTS_USER)
                    .password(PASSWORD)
                    .email(VIEW_CLIENTS_USER + "@localhost")
                    .firstName("View")
                    .lastName("Clients")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID,
                            AdminRoles.VIEW_CLIENTS));
            return realm;
        }
    }
}
