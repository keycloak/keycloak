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
package org.keycloak.tests.admin;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
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
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.https.CertificatesConfig;
import org.keycloak.testframework.https.CertificatesConfigBuilder;
import org.keycloak.testframework.https.InjectCertificates;
import org.keycloak.testframework.https.ManagedCertificates;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.admin.AdminApiUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for the various "Advanced" scenarios of java admin-client
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@KeycloakIntegrationTest
public class AdminClientTest {

    @InjectRealm(config = TestRealmConfig.class)
    ManagedRealm testRealm;

    @InjectAdminClientFactory
    AdminClientFactory adminClientFactory;

    @InjectCertificates(config = MTlsCertificatesEnabled.class)
    ManagedCertificates managedCertificates;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    private static final String TEST_USER_USERNAME  = "test-user@localhost";
    private static final String TEST_USER_PASSWORD = "password";

    private static final String CLIENT_ID  = "service-account-cl";
    private static final String CLIENT_SECRET  = "secret1";

    private static final String X509_CLIENT_ID  = "x509-client-sa";

    @Test
    public void clientCredentialsAuthSuccess() {
        Keycloak adminClient = adminClientFactory.create()
                .realm(testRealm.getName())
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .autoClose()
                .build();

            // Check possible to load the realm
            RealmRepresentation realm = adminClient.realm(testRealm.getName()).toRepresentation();
            Assertions.assertEquals(testRealm.getName(), realm.getRealm());

            timeOffSet.set(1000);

            // Check still possible to load the realm after original token expired (admin client should automatically re-authenticate)
            realm = adminClient.realm(testRealm.getName()).toRepresentation();
            Assertions.assertEquals(testRealm.getName(), realm.getRealm());
    }

    @Test
    public void clientCredentialsClientDisabled() {
        Keycloak adminClient = adminClientFactory.create()
                .realm(testRealm.getName())
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .autoClose()
                .build();

        // Check possible to load the realm
        RealmRepresentation realm = adminClient.realm(testRealm.getName()).toRepresentation();
        Assertions.assertEquals(testRealm.getName(), realm.getRealm());

        // Disable client and check it should not be possible to load the realms anymore
        setClientEnabled(CLIENT_ID, false);

        // Check not possible to invoke anymore
        try {
            realm = adminClient.realm(testRealm.getName()).toRepresentation();
            Assertions.fail("Not expected to successfully get realm");
        } catch (NotAuthorizedException nae) {
            // Expected
        } finally {
            setClientEnabled(CLIENT_ID, true);
        }
    }

    @Test
    public void adminAuthCloseUserSession() {
        UserResource user = AdminApiUtil.findUserByUsernameId(testRealm.admin(), TEST_USER_USERNAME);
        try(Keycloak keycloak = adminClientFactory.create()
                .realm(testRealm.getName())
                .username(TEST_USER_USERNAME)
                .password(TEST_USER_PASSWORD)
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .build()
        ) {

            // Check possible to load the realm
            RealmRepresentation realm = keycloak.realm(testRealm.getName()).toRepresentation();
            Assertions.assertEquals(testRealm.getName(), realm.getRealm());

            Assertions.assertEquals(1, user.getUserSessions().size());
        }

        Assertions.assertEquals(0, user.getUserSessions().size());
    }

    @Test
    public void adminAuthClientDisabled() {
        Keycloak adminClient = adminClientFactory.create()
                .realm(testRealm.getName())
                .username(TEST_USER_USERNAME)
                .password(TEST_USER_PASSWORD)
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .build();

        // Check possible to load the realm
        RealmRepresentation realm = adminClient.realm(testRealm.getName()).toRepresentation();
        Assertions.assertEquals(testRealm.getName(), realm.getRealm());

        // Disable client and check it should not be possible to load the realms anymore
        setClientEnabled(Constants.ADMIN_CLI_CLIENT_ID, false);

        // Check not possible to invoke anymore
        try {
            realm = adminClient.realm(testRealm.getName()).toRepresentation();
            Assertions.fail("Not expected to successfully get realm");
        } catch (NotAuthorizedException nae) {
            // Expected
        } finally {
            setClientEnabled(Constants.ADMIN_CLI_CLIENT_ID, true);
            adminClient.close();
        }
    }

    @Test
    public void adminAuthUserDisabled() {
        Keycloak adminClient = adminClientFactory.create()
                .realm(testRealm.getName())
                .username(TEST_USER_USERNAME)
                .password(TEST_USER_PASSWORD)
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .build();

        Keycloak adminClientOffline = adminClientFactory.create()
                .realm(testRealm.getName())
                .username(TEST_USER_USERNAME)
                .password(TEST_USER_PASSWORD)
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .scope(OAuth2Constants.OFFLINE_ACCESS)
                .build();

        // Check possible to load the realm
        RealmRepresentation realm = adminClient.realm(testRealm.getName()).toRepresentation();
        Assertions.assertEquals(testRealm.getName(), realm.getRealm());
        realm = adminClientOffline.realm(testRealm.getName()).toRepresentation();
        Assertions.assertEquals(testRealm.getName(), realm.getRealm());

        // Disable user and check it should not be possible to load the realms anymore
        setUserEnabled(TEST_USER_USERNAME, false);

        // Check not possible to invoke anymore
        try {
            realm = adminClient.realm(testRealm.getName()).toRepresentation();
            Assertions.fail("Not expected to successfully get realm");
        } catch (NotAuthorizedException nae) {
            // Expected
        }
        try {
            realm = adminClientOffline.realm(testRealm.getName()).toRepresentation();
            Assertions.fail("Not expected to successfully get realm");
        } catch (NotAuthorizedException nae) {
            // Expected
        } finally {
            setUserEnabled(TEST_USER_USERNAME, true);
            adminClient.close();
            adminClientOffline.close();
        }
    }

    @Test
    public void scopedClientCredentialsAuthSuccess() {
        // we need to create custom scope after import, otherwise the default scopes are missing.
        final String scopeName = "myScope";
        String scopeId = createScope(scopeName, KeycloakModelUtils.generateId());
        AdminApiUtil.findClientByClientId(testRealm.admin(), CLIENT_ID).addOptionalClientScope(scopeId);

        // with scope
        try (Keycloak adminClient = adminClientFactory.create()
                .realm(testRealm.getName())
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .scope(scopeName)
                .build()) {
            final AccessTokenResponse accessToken = adminClient.tokenManager().getAccessToken();
            Assertions.assertTrue(accessToken.getScope().contains(scopeName));
            Assertions.assertNotNull(adminClient.realm(testRealm.getName()).clientScopes().get(scopeId).toRepresentation());
        }
        // without scope
        try (Keycloak adminClient = adminClientFactory.create()
                .realm(testRealm.getName())
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .build()) {
            final AccessTokenResponse accessToken = adminClient.tokenManager().getAccessToken();
            Assertions.assertFalse(accessToken.getScope().contains(scopeName));
            Assertions.assertNotNull(adminClient.realm(testRealm.getName()).clientScopes().get(scopeId).toRepresentation());
        }
    }

    // A client secret is not necessary when authentication is
    // performed via X.509 authorizer.
    @Test
    public void noClientSecretWithClientCredentialsAuthSuccess() {
        final String scopeName = "dummyScope";
        String scopeId = createScope(scopeName, KeycloakModelUtils.generateId());
        testRealm.admin().clients().get(testRealm.admin().clients().findByClientId(X509_CLIENT_ID).get(0).getId()).addOptionalClientScope(scopeId);

        // with scope and no client secret
        try (Keycloak adminClient = adminClientFactory.create().realm(testRealm.getName()).grantType(OAuth2Constants.CLIENT_CREDENTIALS).clientId(X509_CLIENT_ID).scope(scopeName).build()) {
            final AccessTokenResponse accessToken = adminClient.tokenManager().getAccessToken();
            Assertions.assertTrue(accessToken.getScope().contains(scopeName));
            Assertions.assertNotNull(adminClient.realm(testRealm.getName()).clientScopes().get(scopeId).toRepresentation());
        }
        // without scope and no client secret
        try (Keycloak adminClient = adminClientFactory.create().realm(testRealm.getName()).grantType(OAuth2Constants.CLIENT_CREDENTIALS).clientId(X509_CLIENT_ID).scope(null).build()) {
            final AccessTokenResponse accessToken = adminClient.tokenManager().getAccessToken();
            Assertions.assertFalse(accessToken.getScope().contains(scopeName));
            Assertions.assertNotNull(adminClient.realm(testRealm.getName()).clientScopes().get(scopeId).toRepresentation());
        }
    }

    private void setUserEnabled(String username, boolean enabled) {
        UserResource user = AdminApiUtil.findUserByUsernameId(testRealm.admin(), username);
        UserRepresentation userRep = user.toRepresentation();
        userRep.setEnabled(enabled);
        user.update(userRep);
    }

    private void setClientEnabled(String clientId, boolean enabled) {
        ClientResource client = AdminApiUtil.findClientByClientId(testRealm.admin(), clientId);
        ClientRepresentation clientRep = client.toRepresentation();
        clientRep.setEnabled(enabled);
        client.update(clientRep);
    }

    private String createScope(String scopeName, String scopeId) {
        final ClientScopeRepresentation testScope = new ClientScopeRepresentation();
        testScope.setId(scopeId);
        testScope.setName(scopeName);
        testScope.setProtocol("openid-connect");

        Response response = testRealm.admin().clientScopes().create(testScope);
        Assertions.assertEquals(201, response.getStatus());
        return ApiUtil.getCreatedId(response);
    }

    private static class TestRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.addUser(TEST_USER_USERNAME)
                    .name("test", "user")
                    .email("testuser@localhost.com")
                    .emailVerified(true)
                    .password(TEST_USER_PASSWORD)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN)
                    .roles(OAuth2Constants.OFFLINE_ACCESS);

            realm.addClient(CLIENT_ID)
                    .secret(CLIENT_SECRET)
                    .serviceAccountsEnabled(true);

            realm.addUser(ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + CLIENT_ID)
                    .name("serviceAccount", "user")
                    .email("serviceAccountUser@localhost.com")
                    .serviceAccountId(CLIENT_ID)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN);

            realm.addClient(X509_CLIENT_ID)
                    .serviceAccountsEnabled(true)
                    .authenticatorType(X509ClientAuthenticator.PROVIDER_ID)
                    .attribute(X509ClientAuthenticator.ATTR_SUBJECT_DN, "(.*?)(?:$)")
                    .attribute(X509ClientAuthenticator.ATTR_ALLOW_REGEX_PATTERN_COMPARISON, "true");

            // This user is associated with the x509-client-sa service account above and
            // give the service account a service account role "realm-management:realm-admin".
            // Without the "realm-management:realm-admin" role we won't be able to test any actual
            // admin call.
            realm.addUser(ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + X509_CLIENT_ID)
                    .name("x509ServiceAccount", "user")
                    .email("x509ServiceAccountUser@localhost.com")
                    .emailVerified(true)
                    .serviceAccountId(X509_CLIENT_ID)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN);

            return realm;
        }
    }

    private static class MTlsCertificatesEnabled implements CertificatesConfig {

        @Override
        public CertificatesConfigBuilder configure(CertificatesConfigBuilder config) {
            return config.tlsEnabled(true).mTlsEnabled(true);
        }
    }
}
