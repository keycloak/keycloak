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

package org.keycloak.testsuite.oauth;

import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LogoutCorsTest extends AbstractKeycloakTest {

    private static final String VALID_CORS_URL = "http://localtest.me:8180";
    private static final String INVALID_CORS_URL = "http://invalid.localtest.me:8180";

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
    }

    @Before
    public void clientConfiguration() {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").addWebOrigins(VALID_CORS_URL);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realmRepresentation = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        RealmBuilder realm = RealmBuilder.edit(realmRepresentation).testEventListener();

        testRealms.add(realm.build());
    }

    @Test
    public void postLogout_validRequestWithValidOrigin() throws Exception {
        OAuthClient.AccessTokenResponse tokenResponse = loginUser();
        String refreshTokenString = tokenResponse.getRefreshToken();
        oauth.origin(VALID_CORS_URL);

        try (CloseableHttpResponse response = oauth.doLogout(refreshTokenString, "password")) {
            assertThat(response, Matchers.statusCodeIsHC(Response.Status.NO_CONTENT));
            assertCors(response);
        }
    }

    @Test
    public void postLogout_validRequestWithInValidOriginShouldFail() throws Exception {
        OAuthClient.AccessTokenResponse tokenResponse = loginUser();
        String refreshTokenString = tokenResponse.getRefreshToken();
        oauth.origin(INVALID_CORS_URL);

        try (CloseableHttpResponse response = oauth.doLogout(refreshTokenString, "password")) {
            assertThat(response, Matchers.statusCodeIsHC(Response.Status.NO_CONTENT));
            assertNotCors(response);
        }
    }

    @Test
    public void postLogout_invalidRequestWithValidOrigin() throws Exception {
        OAuthClient.AccessTokenResponse tokenResponse = loginUser();
        String refreshTokenString = tokenResponse.getRefreshToken();
        oauth.origin(VALID_CORS_URL);

        // Logout with invalid refresh token
        try (CloseableHttpResponse response = oauth.doLogout("invalid-refresh-token", "password")) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusLine().getStatusCode());
            assertCors(response);
        }

        // Logout with invalid client secret
        try (CloseableHttpResponse response = oauth.doLogout(refreshTokenString, "invalid-secret")) {
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatusLine().getStatusCode());
            assertCors(response);
        }
    }

    private OAuthClient.AccessTokenResponse loginUser() {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        oauth.clientSessionState("client-session");
        return oauth.doAccessTokenRequest(code, "password");
    }


    private static void assertCors(CloseableHttpResponse response) {
        assertEquals("true", response.getFirstHeader("Access-Control-Allow-Credentials").getValue());
        assertEquals(VALID_CORS_URL, response.getFirstHeader("Access-Control-Allow-Origin").getValue());
        assertEquals("Access-Control-Allow-Methods", response.getFirstHeader("Access-Control-Expose-Headers").getValue());
    }

    private static void assertNotCors(CloseableHttpResponse response) {
        assertNull(response.getFirstHeader("Access-Control-Allow-Credentials"));
        assertNull(response.getFirstHeader("Access-Control-Allow-Origin"));
        assertNull(response.getFirstHeader("Access-Control-Expose-Headers"));
    }


}
