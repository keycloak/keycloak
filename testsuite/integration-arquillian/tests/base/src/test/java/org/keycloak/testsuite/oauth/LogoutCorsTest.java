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

import jakarta.ws.rs.core.Response;

import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.LogoutResponse;

import org.junit.Before;
import org.junit.Test;

import static org.keycloak.testsuite.AbstractAdminTest.loadJson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
        AccessTokenResponse tokenResponse = loginUser();
        String refreshTokenString = tokenResponse.getRefreshToken();
        oauth.origin(VALID_CORS_URL);

        LogoutResponse response = oauth.doLogout(refreshTokenString);
        assertTrue(response.isSuccess());
        assertCors(response);
    }

    @Test
    public void postLogout_validRequestWithInValidOriginShouldFail() throws Exception {
        AccessTokenResponse tokenResponse = loginUser();
        String refreshTokenString = tokenResponse.getRefreshToken();
        oauth.origin(INVALID_CORS_URL);

        LogoutResponse response = oauth.doLogout(refreshTokenString);
        assertTrue(response.isSuccess());
        assertNotCors(response);
    }

    @Test
    public void postLogout_invalidRequestWithValidOrigin() throws Exception {
        AccessTokenResponse tokenResponse = loginUser();
        String refreshTokenString = tokenResponse.getRefreshToken();
        oauth.origin(VALID_CORS_URL);

        // Logout with invalid refresh token
        LogoutResponse response = oauth.doLogout("invalid-refresh-token");
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertCors(response);

        // Logout with invalid client secret
        response = oauth.client(oauth.getClientId(), "invalid-secret").doLogout(refreshTokenString);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatusCode());
        assertCors(response);
    }

    private AccessTokenResponse loginUser() {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.parseLoginResponse().getCode();

        return oauth.doAccessTokenRequest(code);
    }


    private static void assertCors(LogoutResponse response) {
        assertEquals("true", response.getHeader("Access-Control-Allow-Credentials"));
        assertEquals(VALID_CORS_URL, response.getHeader("Access-Control-Allow-Origin"));
        assertEquals("Access-Control-Allow-Methods", response.getHeader("Access-Control-Expose-Headers"));
    }

    private static void assertNotCors(LogoutResponse response) {
        assertNull(response.getHeader("Access-Control-Allow-Credentials"));
        assertNull(response.getHeader("Access-Control-Allow-Origin"));
        assertNull(response.getHeader("Access-Control-Expose-Headers"));
    }


}
