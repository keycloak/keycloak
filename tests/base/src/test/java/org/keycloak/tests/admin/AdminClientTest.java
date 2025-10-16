/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.testsuite.admin;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.authenticators.client.X509ClientAuthenticator;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.ClientScopeBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.ServerURLs;
import org.keycloak.testsuite.util.UserBuilder;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test for the various "Advanced" scenarios of java admin-client
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AdminClientTest extends AbstractKeycloakTest {

    private static String realmName;

    private static String userId;
    private static String userName;

    private static String clientUUID;
    private static String clientId;
    private static String clientSecret;

    private static String x509ClientUUID;
    private static String x509ClientId;

    private static String x509UserName;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        realmName = "test";
        RealmBuilder realm = RealmBuilder.create().name(realmName)
                .testEventListener();

        clientId = "service-account-cl";
        clientSecret = "secret1";
        ClientRepresentation enabledAppWithSkipRefreshToken = ClientBuilder.create()
                .clientId(clientId)
                .secret(clientSecret)
                .serviceAccountsEnabled(true)
                .build();
        realm.client(enabledAppWithSkipRefreshToken);

        x509ClientId = "x509-client-sa";
        ClientRepresentation x509ServiceAccountClient = ClientBuilder.create()
              .clientId(x509ClientId)
              .serviceAccountsEnabled(true)
              .build();
        x509ServiceAccountClient.setClientAuthenticatorType(X509ClientAuthenticator.PROVIDER_ID);
        x509ServiceAccountClient.setAttributes(Map.of(
            X509ClientAuthenticator.ATTR_SUBJECT_DN, "(.*?)(?:$)",
            X509ClientAuthenticator.ATTR_ALLOW_REGEX_PATTERN_COMPARISON, "true"));
        realm.client(x509ServiceAccountClient);

        userId = KeycloakModelUtils.generateId();
        userName = ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + enabledAppWithSkipRefreshToken.getClientId();
        UserBuilder serviceAccountUser = UserBuilder.create()
                .username(userName)
                .serviceAccountId(enabledAppWithSkipRefreshToken.getClientId())
                .role(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN);
        realm.user(serviceAccountUser);

        // This user is associated with the x509-client-sa service account above and
        // give the service account a service account role "realm-management:realm-admin".
        // Without the "realm-management:realm-admin" role we won't be able to test any actual
        // admin call.
        x509UserName = ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + x509ServiceAccountClient.getClientId();
        UserBuilder x509ServiceAccountUser = UserBuilder.create()
            .username(x509UserName)
            .serviceAccountId(x509ServiceAccountClient.getClientId())
            .role(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN);
        realm.user(x509ServiceAccountUser);

        UserBuilder defaultUser = UserBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .username("test-user@localhost")
                .password("password")
                .role(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN)
                .addRoles(OAuth2Constants.OFFLINE_ACCESS);
        realm.user(defaultUser);

        testRealms.add(realm.build());
    }

    @Override
    public void importRealm(RealmRepresentation realm) {
        super.importRealm(realm);
        if (Objects.equals(realm.getRealm(), realmName)) {
            x509ClientUUID = adminClient.realm(realmName).clients().findByClientId(x509ClientId).get(0).getId();
            clientUUID = adminClient.realm(realmName).clients().findByClientId(clientId).get(0).getId();
            userId = adminClient.realm(realmName).users().searchByUsername(userName, true).get(0).getId();
        }
    }

    @Test
    public void clientCredentialsAuthSuccess() throws Exception {
        try (Keycloak adminClient = AdminClientUtil.createAdminClientWithClientCredentials(realmName, clientId, clientSecret, null)) {
            // Check possible to load the realm
            RealmRepresentation realm = adminClient.realm(realmName).toRepresentation();
            Assert.assertEquals(realmName, realm.getRealm());

            setTimeOffset(1000);

            // Check still possible to load the realm after original token expired (admin client should automatically re-authenticate)
            realm = adminClient.realm(realmName).toRepresentation();
            Assert.assertEquals(realmName, realm.getRealm());
        }
    }

    @Test
    public void clientCredentialsClientDisabled() throws Exception {
        try (Keycloak adminClient = AdminClientUtil.createAdminClientWithClientCredentials(realmName, clientId, clientSecret, null)) {
            // Check possible to load the realm
            RealmRepresentation realm = adminClient.realm(realmName).toRepresentation();
            Assert.assertEquals(realmName, realm.getRealm());

            // Disable client and check it should not be possible to load the realms anymore
            setClientEnabled(clientId, false);

            // Check not possible to invoke anymore
            try {
                realm = adminClient.realm(realmName).toRepresentation();
                Assert.fail("Not expected to successfully get realm");
            } catch (NotAuthorizedException nae) {
                // Expected
            }
        } finally {
            setClientEnabled(clientId, true);
        }
    }

    @Test
    public void adminAuthCloseUserSession() throws Exception {
        UserResource user = ApiUtil.findUserByUsernameId(adminClient.realm(realmName), "test-user@localhost");
        try (Keycloak keycloak = AdminClientUtil.createAdminClient(false, realmName, "test-user@localhost", "password", Constants.ADMIN_CLI_CLIENT_ID, null)) {
            // Check possible to load the realm
            RealmRepresentation realm = keycloak.realm(realmName).toRepresentation();
            Assert.assertEquals(realmName, realm.getRealm());

            Assert.assertEquals(1, user.getUserSessions().size());
        }
        Assert.assertEquals(0, user.getUserSessions().size());
    }

    @Test
    public void adminAuthClientDisabled() throws Exception {
        try (Keycloak adminClient = AdminClientUtil.createAdminClient(false, realmName, "test-user@localhost", "password", Constants.ADMIN_CLI_CLIENT_ID, null)) {
            // Check possible to load the realm
            RealmRepresentation realm = adminClient.realm(realmName).toRepresentation();
            Assert.assertEquals(realmName, realm.getRealm());

            // Disable client and check it should not be possible to load the realms anymore
            setClientEnabled(Constants.ADMIN_CLI_CLIENT_ID, false);

            // Check not possible to invoke anymore
            try {
                realm = adminClient.realm(realmName).toRepresentation();
                Assert.fail("Not expected to successfully get realm");
            } catch (NotAuthorizedException nae) {
                // Expected
            }
        } finally {
            setClientEnabled(Constants.ADMIN_CLI_CLIENT_ID, true);
        }
    }

    @Test
    public void adminAuthUserDisabled() throws Exception {
        try (Keycloak adminClient = AdminClientUtil.createAdminClient(false, realmName, "test-user@localhost", "password", Constants.ADMIN_CLI_CLIENT_ID, null);
             Keycloak adminClientOffline = AdminClientUtil.createAdminClient(false, ServerURLs.getAuthServerContextRoot(), realmName, "test-user@localhost", "password", Constants.ADMIN_CLI_CLIENT_ID, null, OAuth2Constants.OFFLINE_ACCESS, false);
        ) {
            // Check possible to load the realm
            RealmRepresentation realm = adminClient.realm(realmName).toRepresentation();
            Assert.assertEquals(realmName, realm.getRealm());
            realm = adminClientOffline.realm(realmName).toRepresentation();
            Assert.assertEquals(realmName, realm.getRealm());

            // Disable client and check it should not be possible to load the realms anymore
            setUserEnabled("test-user@localhost", false);

            // Check not possible to invoke anymore
            try {
                realm = adminClient.realm(realmName).toRepresentation();
                Assert.fail("Not expected to successfully get realm");
            } catch (NotAuthorizedException nae) {
                // Expected
            }
            try {
                realm = adminClientOffline.realm(realmName).toRepresentation();
                Assert.fail("Not expected to successfully get realm");
            } catch (NotAuthorizedException nae) {
                // Expected
            }
        } finally {
            setUserEnabled("test-user@localhost", true);
        }
    }

    @Test
    public void scopedClientCredentialsAuthSuccess() throws Exception {
        final RealmResource testRealm = adminClient.realm(realmName);

        // we need to create custom scope after import, otherwise the default scopes are missing.
        final String scopeName = "myScope";
        String scopeId = createScope(testRealm, scopeName, KeycloakModelUtils.generateId());
        testRealm.clients().get(clientUUID).addOptionalClientScope(scopeId);

        // with scope
        try (Keycloak adminClient = AdminClientUtil.createAdminClientWithClientCredentials(realmName,
            clientId, clientSecret, scopeName)) {
            final AccessTokenResponse accessToken = adminClient.tokenManager().getAccessToken();
            Assert.assertTrue(accessToken.getScope().contains(scopeName));
            Assert.assertNotNull(adminClient.realm(realmName).clientScopes().get(scopeId).toRepresentation());
        }
        // without scope
        try (Keycloak adminClient = AdminClientUtil.createAdminClientWithClientCredentials(realmName,
            clientId, clientSecret, null)) {
            final AccessTokenResponse accessToken = adminClient.tokenManager().getAccessToken();
            Assert.assertFalse(accessToken.getScope().contains(scopeName));
            Assert.assertNotNull(adminClient.realm(realmName).clientScopes().get(scopeId).toRepresentation());
        }
    }

    // A client secret in not necessary when authentication is
    // performed via X.509 authorizer.
    @Test
    public void noClientSecretWithClientCredentialsAuthSuccess() throws Exception {
        final RealmResource testRealm = adminClient.realm(realmName);

        final String scopeName = "dummyScope";
        String scopeId = createScope(testRealm, scopeName, KeycloakModelUtils.generateId());
        testRealm.clients().get(x509ClientUUID).addOptionalClientScope(scopeId);

        // with scope and no client secret
        try (Keycloak adminClient = AdminClientUtil.
            createMTlsAdminClientWithClientCredentialsWithoutSecret(realmName, x509ClientId, scopeName)) {
            final AccessTokenResponse accessToken = adminClient.tokenManager().getAccessToken();
            Assert.assertTrue(accessToken.getScope().contains(scopeName));
            Assert.assertNotNull(adminClient.realm(realmName).clientScopes().get(scopeId).toRepresentation());
        }
        // without scope and no client secret
        try (Keycloak adminClient = AdminClientUtil.
            createMTlsAdminClientWithClientCredentialsWithoutSecret(realmName, x509ClientId, null)) {
            final AccessTokenResponse accessToken = adminClient.tokenManager().getAccessToken();
            Assert.assertFalse(accessToken.getScope().contains(scopeName));
            Assert.assertNotNull(adminClient.realm(realmName).clientScopes().get(scopeId).toRepresentation());
        }
    }

    private void setClientEnabled(String clientId, boolean enabled) {
        ClientResource client = ApiUtil.findClientByClientId(adminClient.realms().realm(realmName), clientId);
        ClientRepresentation clientRep = client.toRepresentation();
        clientRep.setEnabled(enabled);
        client.update(clientRep);
    }

    private void setUserEnabled(String username, boolean enabled) {
        UserResource user = ApiUtil.findUserByUsernameId(adminClient.realms().realm(realmName), username);
        UserRepresentation userRep = user.toRepresentation();
        userRep.setEnabled(enabled);
        user.update(userRep);
    }

    private String createScope(RealmResource testRealm, String scopeName, String scopeId) {
        final ClientScopeRepresentation testScope =
            ClientScopeBuilder.create().name(scopeName).protocol("openid-connect").build();
        testScope.setId(scopeId);
        try (Response response = testRealm.clientScopes().create(testScope)) {
            Assert.assertEquals(201, response.getStatus());
            return ApiUtil.getCreatedId(response);
        }
    }
}
