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
package org.keycloak.tests.account;

import java.io.IOException;
import java.util.List;

import org.keycloak.common.Profile;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.IssuedVerifiableCredentialModel;
import org.keycloak.models.UserVerifiableCredentialModel;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.representations.idm.oid4vc.IssuedVerifiableCredentialRepresentation;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that the Account API delete endpoint for issued verifiable credentials
 * enforces ownership — a user cannot delete another user's credential.
 *
 * This test is intentionally self-contained (does not extend
 * IssuedVerifiableCredentialTest) to avoid sharing a realm/config with
 * unrelated wallet/client-scope test setup, and to keep its own ownership
 * fixtures isolated.
 */
@KeycloakIntegrationTest(config = AccountIssuedVerifiableCredentialOwnershipTest.OwnershipTestServerConfig.class)
public class AccountIssuedVerifiableCredentialOwnershipTest {

    private static final String PASSWORD = "password";
    private static final String USER_A = "user-a";
    private static final String USER_B = "user-b";
    private static final String CREDENTIAL_TYPE = "ownership-test-credential";

    @InjectRealm(config = OwnershipTestRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectHttpClient
    CloseableHttpClient httpClient;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    /**
     * Verifies that User A cannot delete a credential belonging to User B.
     * The endpoint should return 404 (not leaking that the credential exists
     * but belongs to someone else), and User B's credential should remain intact.
     */
    @Test
    public void testDeleteCredentialBelongingToAnotherUserIsRejected() throws IOException {
        String userBId = realm.admin().users().search(USER_B).get(0).getId();

        // Seed a credential for User B
        createIssuedVcViaModelLayer(userBId, CREDENTIAL_TYPE, "wallet-123", "rev-001");

        // Confirm User B has one credential
        List<IssuedVerifiableCredentialRepresentation> userBCreds =
                realm.admin().users().get(userBId).verifiableCredentials().getIssuedCredentials();
        assertThat("User B should have exactly one credential", userBCreds, hasSize(1));
        String userBCredentialId = userBCreds.get(0).getId();

        // Authenticate as User A
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(USER_A, PASSWORD);
        assertTrue(tokenResponse.isSuccess(), "Token request failed for user-a: " + tokenResponse.getErrorDescription());
        String userAToken = tokenResponse.getAccessToken();

        // User A attempts to delete User B's credential via the Account API
        HttpDelete deleteRequest = new HttpDelete(
                realm.getBaseUrl() + "/account/issued-verifiable-credentials/" + userBCredentialId);
        deleteRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken);

        try (CloseableHttpResponse response = httpClient.execute(deleteRequest)) {
            assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode(),
                    "Expected 404 when attempting to delete another user's credential");
        }

        // User B's credential must still exist
        List<IssuedVerifiableCredentialRepresentation> userBCredsAfter =
                realm.admin().users().get(userBId).verifiableCredentials().getIssuedCredentials();
        assertThat("User B's credential should still exist after unauthorized delete attempt",
                userBCredsAfter, hasSize(1));
    }

    /**
     * Verifies that a user can successfully delete their own credential.
     */
    @Test
    public void testDeleteOwnCredentialSucceeds() throws IOException {
        String userAId = realm.admin().users().search(USER_A).get(0).getId();

        // Seed a credential for User A
        createIssuedVcViaModelLayer(userAId, CREDENTIAL_TYPE, "wallet-123", "rev-001");

        // Confirm User A has one credential
        List<IssuedVerifiableCredentialRepresentation> userACreds =
                realm.admin().users().get(userAId).verifiableCredentials().getIssuedCredentials();
        assertThat("User A should have exactly one credential", userACreds, hasSize(1));
        String userACredentialId = userACreds.get(0).getId();

        // Authenticate as User A
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(USER_A, PASSWORD);
        assertTrue(tokenResponse.isSuccess(), "Token request failed for user-a: " + tokenResponse.getErrorDescription());
        String userAToken = tokenResponse.getAccessToken();

        // User A deletes their own credential
        HttpDelete deleteRequest = new HttpDelete(
                realm.getBaseUrl() + "/account/issued-verifiable-credentials/" + userACredentialId);
        deleteRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken);

        try (CloseableHttpResponse response = httpClient.execute(deleteRequest)) {
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusLine().getStatusCode(),
                    "Expected 204 when deleting own credential");
        }

        // Credential should be gone
        List<IssuedVerifiableCredentialRepresentation> userACredsAfter =
                realm.admin().users().get(userAId).verifiableCredentials().getIssuedCredentials();
        assertThat("User A's credential should be deleted", userACredsAfter, hasSize(0));
    }

    /**
     * Minimal, self-contained model-layer credential seeding helper.
     * Mirrors IssuedVerifiableCredentialTest#createIssuedVcViaModelLayer but
     * resolves the client scope against this test's own realm, avoiding any
     * dependency on a shared/inherited realm fixture.
     */
    private void createIssuedVcViaModelLayer(String userId, String credentialType,
                                              String clientId, String revision) {
        String clientScopeId = realm.admin().clientScopes().findAll().stream()
                .filter(s -> credentialType.equals(s.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Client scope not found: " + credentialType))
                .getId();
        runOnServer.run(session -> {
            UserVerifiableCredentialModel vcModel = new UserVerifiableCredentialModel(null, clientScopeId);
            vcModel.setRevision(revision);
            UserVerifiableCredentialModel added = session.users().addVerifiableCredential(userId, vcModel);
            IssuedVerifiableCredentialModel model = new IssuedVerifiableCredentialModel(userId, added.getId(), clientId);
            model.setRevision(revision);
            session.users().addIssuedVerifiableCredential(model);
        });
    }

    public static class OwnershipTestRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                    .verifiableCredentialsEnabled(true)
                    .clientScopes(
                            new CredentialScopeRepresentation(CREDENTIAL_TYPE)
                                    .setIncludeInTokenScope(true)
                                    .setCredentialConfigurationId(CREDENTIAL_TYPE))
                    .users(
                            UserBuilder.create(USER_A)
                                    .name("User", "A")
                                    .email("user-a@localhost")
                                    .password(PASSWORD)
                                    .clientRoles(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID,
                                            AccountRoles.MANAGE_ACCOUNT, AccountRoles.VIEW_PROFILE),
                            UserBuilder.create(USER_B)
                                    .name("User", "B")
                                    .email("user-b@localhost")
                                    .password(PASSWORD)
                                    .clientRoles(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID,
                                            AccountRoles.MANAGE_ACCOUNT, AccountRoles.VIEW_PROFILE));
        }
    }

    public static class OwnershipTestServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.OID4VC_VCI);
        }
    }
}
