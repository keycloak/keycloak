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
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.Response;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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

    @Rule
    public ExpectedException expectedException = ExpectedException.none();


    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        realmName = "test";
        RealmBuilder realm = RealmBuilder.create().name(realmName)
                .privateKey("MIICXAIBAAKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQABAoGAfmO8gVhyBxdqlxmIuglbz8bcjQbhXJLR2EoS8ngTXmN1bo2L90M0mUKSdc7qF10LgETBzqL8jYlQIbt+e6TH8fcEpKCjUlyq0Mf/vVbfZSNaVycY13nTzo27iPyWQHK5NLuJzn1xvxxrUeXI6A2WFpGEBLbHjwpx5WQG9A+2scECQQDvdn9NE75HPTVPxBqsEd2z10TKkl9CZxu10Qby3iQQmWLEJ9LNmy3acvKrE3gMiYNWb6xHPKiIqOR1as7L24aTAkEAtyvQOlCvr5kAjVqrEKXalj0Tzewjweuxc0pskvArTI2Oo070h65GpoIKLc9jf+UA69cRtquwP93aZKtW06U8dQJAF2Y44ks/mK5+eyDqik3koCI08qaC8HYq2wVl7G2QkJ6sbAaILtcvD92ToOvyGyeE0flvmDZxMYlvaZnaQ0lcSQJBAKZU6umJi3/xeEbkJqMfeLclD27XGEFoPeNrmdx0q10Azp4NfJAY+Z8KRyQCR2BEG+oNitBOZ+YXF9KCpH3cdmECQHEigJhYg+ykOvr1aiZUMFT72HU0jnmQe2FVekuG+LJUt2Tm7GtMjTFoGpf0JwrVuZN39fOYAlo+nTixgeW7X8Y=")
                .publicKey("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB")
                .testEventListener();

        clientUUID = KeycloakModelUtils.generateId();
        clientId = "service-account-cl";
        clientSecret = "secret1";
        ClientRepresentation enabledAppWithSkipRefreshToken = ClientBuilder.create()
                .id(clientUUID)
                .clientId(clientId)
                .secret(clientSecret)
                .serviceAccountsEnabled(true)
                .build();
        realm.client(enabledAppWithSkipRefreshToken);

        userId = KeycloakModelUtils.generateId();
        userName = ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + enabledAppWithSkipRefreshToken.getClientId();
        UserBuilder serviceAccountUser = UserBuilder.create()
                .id(userId)
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
        final String scopeId = KeycloakModelUtils.generateId();
        createScope(testRealm, scopeName, scopeId);
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

    private void createScope(RealmResource testRealm, String scopeName, String scopeId) {
        final ClientScopeRepresentation testScope =
            ClientScopeBuilder.create().name(scopeName).protocol("openid-connect").build();
        testScope.setId(scopeId);
        final Response scope = testRealm.clientScopes().create(testScope);
        Assert.assertEquals(201, scope.getStatus());
    }
}
