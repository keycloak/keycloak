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
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.ClientScopeBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import java.util.Objects;

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

        userId = KeycloakModelUtils.generateId();
        userName = ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + enabledAppWithSkipRefreshToken.getClientId();
        UserBuilder serviceAccountUser = UserBuilder.create()
                .username(userName)
                .serviceAccountId(enabledAppWithSkipRefreshToken.getClientId())
                .role(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN);
        realm.user(serviceAccountUser);

        UserBuilder defaultUser = UserBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .username("test-user@localhost")
                .password("password")
                .role(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN);
        realm.user(defaultUser);

        testRealms.add(realm.build());
    }

    @Override
    public void importRealm(RealmRepresentation realm) {
        super.importRealm(realm);
        if (Objects.equals(realm.getRealm(), realmName)) {
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
        }
        // without scope
        try (Keycloak adminClient = AdminClientUtil.createAdminClientWithClientCredentials(realmName,
            clientId, clientSecret, null)) {
            final AccessTokenResponse accessToken = adminClient.tokenManager().getAccessToken();
            Assert.assertFalse(accessToken.getScope().contains(scopeName));
        }
    }

    private void setClientEnabled(String clientId, boolean enabled) {
        ClientResource client = ApiUtil.findClientByClientId(adminClient.realms().realm(realmName), clientId);
        ClientRepresentation clientRep = client.toRepresentation();
        clientRep.setEnabled(enabled);
        client.update(clientRep);
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
