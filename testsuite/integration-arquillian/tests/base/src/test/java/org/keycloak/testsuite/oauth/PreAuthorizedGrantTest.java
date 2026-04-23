/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.oauth;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.OAuth2Constants;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedCodeGrant;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Before;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnableFeature(value = Profile.Feature.OID4VC_VCI, skipRestart = true)
@EnableFeature(value = Profile.Feature.OID4VC_VCI_PREAUTH_CODE, skipRestart = true)
public class PreAuthorizedGrantTest extends AbstractTestRealmKeycloakTest {

    private CloseableHttpClient httpClient;

    @Before
    public void setup() {
        httpClient = HttpClientBuilder.create().build();
    }

    @Test
    public void testPreAuthorizedGrant() throws Exception {
        String userSessionId = getUserSession();
        String preAuthorizedCode = getTestingClient().testing(TEST_REALM_NAME).getPreAuthorizedCode(TEST_REALM_NAME, userSessionId, "test-app", Time.currentTime() + 30);
        AccessTokenResponse accessTokenResponse = postCode(preAuthorizedCode);

        assertEquals(HttpStatus.SC_OK, accessTokenResponse.getStatusCode(), "An access token should have successfully been returned.");
    }

    @Test
    public void testPreAuthorizedGrantExpired() throws Exception {
        String userSessionId = getUserSession();
        String preAuthorizedCode = getTestingClient().testing(TEST_REALM_NAME).getPreAuthorizedCode(TEST_REALM_NAME, userSessionId, "test-app", Time.currentTime() - 30);
        AccessTokenResponse accessTokenResponse = postCode(preAuthorizedCode);
        assertEquals(HttpStatus.SC_BAD_REQUEST, accessTokenResponse.getStatusCode(), "An expired code should not get an access token.");
    }

    @Test
    public void testPreAuthorizedGrantInvalidCode() throws Exception {
        // assure that a session exists.
        getUserSession();
        AccessTokenResponse accessTokenResponse = postCode("invalid-code");
        assertEquals(HttpStatus.SC_BAD_REQUEST, accessTokenResponse.getStatusCode(), "An invalid code should not get an access token.");
    }

    @Test
    public void testPreAuthorizedGrantNoCode() throws Exception {
        // assure that a session exists.
        getUserSession();
        HttpPost post = new HttpPost(getTokenEndpoint());
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, PreAuthorizedCodeGrant.PRE_AUTH_GRANT_TYPE));
        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        post.setEntity(formEntity);

        AccessTokenResponse accessTokenResponse = new AccessTokenResponse(httpClient.execute(post));
        assertEquals(HttpStatus.SC_BAD_REQUEST, accessTokenResponse.getStatusCode(), "If no code is provided, no access token should be returned.");
    }

    /**
     * When verifiable credentials are disabled for the realm, the pre-authorized code
     * grant (used by OID4VCI flows) must be rejected with 403 Forbidden.
     */
    @Test
    public void testPreAuthorizedGrantRealmDisabled() throws Exception {
        // Disable verifiable credentials for the test realm
        RealmRepresentation realmRep = managedRealm.admin().toRepresentation();
        realmRep.setVerifiableCredentialsEnabled(false);
        managedRealm.admin().update(realmRep);

        try {
            String userSessionId = getUserSession();
            String preAuthorizedCode = getTestingClient().testing()
                    .getPreAuthorizedCode(TEST_REALM_NAME, userSessionId, "test-app", Time.currentTime() + 30);

            AccessTokenResponse accessTokenResponse = postCode(preAuthorizedCode);
            assertEquals(HttpStatus.SC_FORBIDDEN, accessTokenResponse.getStatusCode(), "Pre-authorized grant should be forbidden when verifiable credentials are disabled.");
        } finally {
            // Re-enable verifiable credentials so other tests see the default behavior
            RealmRepresentation realmRepReset = managedRealm.admin().toRepresentation();
            realmRepReset.setVerifiableCredentialsEnabled(true);
            managedRealm.admin().update(realmRepReset);
        }
    }

    private AccessTokenResponse postCode(String preAuthorizedCode) throws Exception {
        HttpPost post = new HttpPost(getTokenEndpoint());
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, PreAuthorizedCodeGrant.PRE_AUTH_GRANT_TYPE));
        parameters.add(new BasicNameValuePair("pre-authorized_code", preAuthorizedCode));
        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        post.setEntity(formEntity);

        return new AccessTokenResponse(httpClient.execute(post));
    }

    private String getTokenEndpoint() {
        return OIDCLoginProtocolService
                .tokenUrl(UriBuilder.fromUri(suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth"))
                .build(TEST_REALM_NAME)
                .toString();
    }

    private String getUserSession() {
        // create a session
        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.doLogin("john", "password");
        return authorizationEndpointResponse.getSessionState();
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        testRealm.setVerifiableCredentialsEnabled(true);
        
        UserRepresentation user = UserBuilder.create()
                .id("user-id")
                .username("john")
                .enabled(true)
                .email("john@email.cz")
                .emailVerified(true)
                .password("password").build();
        if (testRealm.getUsers() != null) {
            testRealm.getUsers().add(user);
        } else {
            testRealm.setUsers(List.of(user));
        }
    }


}
