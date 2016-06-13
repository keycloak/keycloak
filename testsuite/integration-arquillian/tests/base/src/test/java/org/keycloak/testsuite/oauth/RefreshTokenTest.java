/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.RealmManager;
import org.keycloak.util.BasicAuthHelper;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.admin.ApiUtil.findUserByUsername;
import static org.keycloak.testsuite.util.OAuthClient.AUTH_SERVER_ROOT;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RefreshTokenTest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
    }

    @Before
    public void clientConfiguration() {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").directAccessGrant(true);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {

        RealmRepresentation realmRepresentation = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);

        RealmBuilder realm = RealmBuilder.edit(realmRepresentation)
                .testEventListener();

        testRealms.add(realm.build());

    }


    /**
     * KEYCLOAK-547
     *
     * @throws Exception
     */
    @Test
    public void nullRefreshToken() throws Exception {
        Client client = ClientBuilder.newClient();
        UriBuilder builder = UriBuilder.fromUri(AUTH_SERVER_ROOT);
        URI uri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
        WebTarget target = client.target(uri);

        org.keycloak.representations.AccessTokenResponse tokenResponse = null;
        {
            String header = BasicAuthHelper.createHeader("test-app", "password");
            Form form = new Form();
            Response response = target.request()
                    .header(HttpHeaders.AUTHORIZATION, header)
                    .post(Entity.form(form));
            assertEquals(400, response.getStatus());
            response.close();
        }
        events.clear();


    }

    @Test
    public void invalidRefreshToken() throws Exception {
        OAuthClient.AccessTokenResponse response = oauth.doRefreshTokenRequest("invalid", "password");
        assertEquals(400, response.getStatusCode());
        assertEquals("invalid_grant", response.getError());
        events.clear();
    }

    @Test
    public void refreshTokenRequest() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        String refreshTokenString = tokenResponse.getRefreshToken();
        RefreshToken refreshToken = oauth.verifyRefreshToken(refreshTokenString);

        EventRepresentation tokenEvent = events.expectCodeToToken(codeId, sessionId).assertEvent();

        Assert.assertNotNull(refreshTokenString);

        assertEquals("bearer", tokenResponse.getTokenType());

        Assert.assertThat(token.getExpiration() - getCurrentTime(), allOf(greaterThanOrEqualTo(200), lessThanOrEqualTo(350)));
        int actual = refreshToken.getExpiration() - getCurrentTime();
        Assert.assertThat(actual, allOf(greaterThanOrEqualTo(1799), lessThanOrEqualTo(1800)));

        assertEquals(sessionId, refreshToken.getSessionState());

        setTimeOffset(2);

        OAuthClient.AccessTokenResponse response = oauth.doRefreshTokenRequest(refreshTokenString, "password");
        AccessToken refreshedToken = oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshedRefreshToken = oauth.verifyRefreshToken(response.getRefreshToken());

        assertEquals(200, response.getStatusCode());

        assertEquals(sessionId, refreshedToken.getSessionState());
        assertEquals(sessionId, refreshedRefreshToken.getSessionState());

        Assert.assertThat(response.getExpiresIn(), allOf(greaterThanOrEqualTo(250), lessThanOrEqualTo(300)));
        Assert.assertThat(refreshedToken.getExpiration() - getCurrentTime(), allOf(greaterThanOrEqualTo(250), lessThanOrEqualTo(300)));

        Assert.assertThat(refreshedToken.getExpiration() - token.getExpiration(), allOf(greaterThanOrEqualTo(1), lessThanOrEqualTo(10)));
        Assert.assertThat(refreshedRefreshToken.getExpiration() - refreshToken.getExpiration(), allOf(greaterThanOrEqualTo(1), lessThanOrEqualTo(10)));

        Assert.assertNotEquals(token.getId(), refreshedToken.getId());
        Assert.assertNotEquals(refreshToken.getId(), refreshedRefreshToken.getId());

        assertEquals("bearer", response.getTokenType());

        assertEquals(findUserByUsername(adminClient.realm("test"), "test-user@localhost").getId(), refreshedToken.getSubject());
        Assert.assertNotEquals("test-user@localhost", refreshedToken.getSubject());

        assertEquals(1, refreshedToken.getRealmAccess().getRoles().size());
        Assert.assertTrue(refreshedToken.getRealmAccess().isUserInRole("user"));

        assertEquals(1, refreshedToken.getResourceAccess(oauth.getClientId()).getRoles().size());
        Assert.assertTrue(refreshedToken.getResourceAccess(oauth.getClientId()).isUserInRole("customer-user"));

        EventRepresentation refreshEvent = events.expectRefresh(tokenEvent.getDetails().get(Details.REFRESH_TOKEN_ID), sessionId).assertEvent();
        Assert.assertNotEquals(tokenEvent.getDetails().get(Details.TOKEN_ID), refreshEvent.getDetails().get(Details.TOKEN_ID));
        Assert.assertNotEquals(tokenEvent.getDetails().get(Details.REFRESH_TOKEN_ID), refreshEvent.getDetails().get(Details.UPDATED_REFRESH_TOKEN_ID));

        setTimeOffset(0);
    }

    @Test
    public void refreshTokenReuseTokenWithoutRefreshTokensRevoked() throws Exception {
        try {
            oauth.doLogin("test-user@localhost", "password");

            EventRepresentation loginEvent = events.expectLogin().assertEvent();

            String sessionId = loginEvent.getSessionId();
            String codeId = loginEvent.getDetails().get(Details.CODE_ID);

            String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

            OAuthClient.AccessTokenResponse response1 = oauth.doAccessTokenRequest(code, "password");
            RefreshToken refreshToken1 = oauth.verifyRefreshToken(response1.getRefreshToken());

            events.expectCodeToToken(codeId, sessionId).assertEvent();

            setTimeOffset(2);

            OAuthClient.AccessTokenResponse response2 = oauth.doRefreshTokenRequest(response1.getRefreshToken(), "password");
            assertEquals(200, response2.getStatusCode());

            events.expectRefresh(refreshToken1.getId(), sessionId).assertEvent();

            OAuthClient.AccessTokenResponse response3 = oauth.doRefreshTokenRequest(response1.getRefreshToken(), "password");

            assertEquals(200, response3.getStatusCode());

            events.expectRefresh(refreshToken1.getId(), sessionId).assertEvent();
        } finally {
            setTimeOffset(0);
        }
    }

    @Test
    public void refreshTokenReuseTokenWithRefreshTokensRevoked() throws Exception {
        try {

            RealmManager.realm(adminClient.realm("test")).revokeRefreshToken(true);

            oauth.doLogin("test-user@localhost", "password");

            EventRepresentation loginEvent = events.expectLogin().assertEvent();

            String sessionId = loginEvent.getSessionId();
            String codeId = loginEvent.getDetails().get(Details.CODE_ID);

            String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

            OAuthClient.AccessTokenResponse response1 = oauth.doAccessTokenRequest(code, "password");
            RefreshToken refreshToken1 = oauth.verifyRefreshToken(response1.getRefreshToken());

            events.expectCodeToToken(codeId, sessionId).assertEvent();

            setTimeOffset(2);

            OAuthClient.AccessTokenResponse response2 = oauth.doRefreshTokenRequest(response1.getRefreshToken(), "password");
            RefreshToken refreshToken2 = oauth.verifyRefreshToken(response2.getRefreshToken());

            assertEquals(200, response2.getStatusCode());

            events.expectRefresh(refreshToken1.getId(), sessionId).assertEvent();

            OAuthClient.AccessTokenResponse response3 = oauth.doRefreshTokenRequest(response1.getRefreshToken(), "password");

            assertEquals(400, response3.getStatusCode());

            events.expectRefresh(refreshToken1.getId(), sessionId).removeDetail(Details.TOKEN_ID).removeDetail(Details.UPDATED_REFRESH_TOKEN_ID).error("invalid_token").assertEvent();

            oauth.doRefreshTokenRequest(response2.getRefreshToken(), "password");

            events.expectRefresh(refreshToken2.getId(), sessionId).assertEvent();
        } finally {
            setTimeOffset(0);
            RealmManager.realm(adminClient.realm("test")).revokeRefreshToken(false);
        }
    }

    String privateKey;
    String publicKey;

    @Test
    public void refreshTokenRealmKeysChanged() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
        String refreshTokenString = response.getRefreshToken();
        RefreshToken refreshToken = oauth.verifyRefreshToken(refreshTokenString);

        events.expectCodeToToken(codeId, sessionId).assertEvent();

        try {

            RealmManager.realm(adminClient.realm("test")).generateKeys();

            response = oauth.doRefreshTokenRequest(refreshTokenString, "password");

            assertEquals(400, response.getStatusCode());
            assertEquals("invalid_grant", response.getError());

            events.expectRefresh(refreshToken.getId(), sessionId).user((String) null).session((String) null).clearDetails().error(Errors.INVALID_TOKEN).assertEvent();
        } finally {
            RealmManager.realm(adminClient.realm("test")).keyPair(privateKey, publicKey);
        }
    }

    @Test
    public void refreshTokenClientDisabled() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
        String refreshTokenString = response.getRefreshToken();
        RefreshToken refreshToken = oauth.verifyRefreshToken(refreshTokenString);

        events.expectCodeToToken(codeId, sessionId).assertEvent();

        try {
            ClientManager.realm(adminClient.realm("test")).clientId(oauth.getClientId()).enabled(false);

            response = oauth.doRefreshTokenRequest(refreshTokenString, "password");

            assertEquals(400, response.getStatusCode());
            assertEquals("invalid_client", response.getError());

            events.expectRefresh(refreshToken.getId(), sessionId).user((String) null).session((String) null).clearDetails().error(Errors.CLIENT_DISABLED).assertEvent();
        } finally {
            ClientManager.realm(adminClient.realm("test")).clientId(oauth.getClientId()).enabled(true);
        }
    }

    @Test
    public void refreshTokenUserSessionExpired() {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");

        events.poll();

        String refreshId = oauth.verifyRefreshToken(tokenResponse.getRefreshToken()).getId();

        testingClient.testing().removeUserSession("test", sessionId);

        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken(), "password");

        assertEquals(400, tokenResponse.getStatusCode());
        assertNull(tokenResponse.getAccessToken());
        assertNull(tokenResponse.getRefreshToken());

        events.expectRefresh(refreshId, sessionId).error(Errors.INVALID_TOKEN);

        events.clear();
    }

    @Test
    public void testUserSessionRefreshAndIdle() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");

        events.poll();

        String refreshId = oauth.verifyRefreshToken(tokenResponse.getRefreshToken()).getId();

        int last = testingClient.testing().getLastSessionRefresh("test", sessionId);

        setTimeOffset(2);

        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken(), "password");

        AccessToken refreshedToken = oauth.verifyToken(tokenResponse.getAccessToken());
        RefreshToken refreshedRefreshToken = oauth.verifyRefreshToken(tokenResponse.getRefreshToken());

        assertEquals(200, tokenResponse.getStatusCode());

        int next = testingClient.testing().getLastSessionRefresh("test", sessionId);

        Assert.assertNotEquals(last, next);

        RealmResource realmResource = adminClient.realm("test");
        int lastAccessTokenLifespan = realmResource.toRepresentation().getAccessTokenLifespan();
        RealmManager.realm(realmResource).accessTokenLifespan(100000);

        setTimeOffset(4);
        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken(), "password");

        next = testingClient.testing().getLastSessionRefresh("test", sessionId);

        // lastSEssionRefresh should be updated because access code lifespan is higher than sso idle timeout
        Assert.assertThat(next, allOf(greaterThan(last), lessThan(last + 50)));

        int originalIdle = realmResource.toRepresentation().getSsoSessionIdleTimeout();
        RealmManager.realm(realmResource).ssoSessionIdleTimeout(1);

        events.clear();
        setTimeOffset(6);
        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken(), "password");

        // test idle timeout
        assertEquals(400, tokenResponse.getStatusCode());
        assertNull(tokenResponse.getAccessToken());
        assertNull(tokenResponse.getRefreshToken());

        events.expectRefresh(refreshId, sessionId).error(Errors.INVALID_TOKEN);

        RealmManager.realm(realmResource).ssoSessionIdleTimeout(originalIdle).accessTokenLifespan(lastAccessTokenLifespan);

        events.clear();

        setTimeOffset(0);
    }

    @Test
    public void refreshTokenUserSessionMaxLifespan() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");

        events.poll();

        String refreshId = oauth.verifyRefreshToken(tokenResponse.getRefreshToken()).getId();

        RealmResource realmResource = adminClient.realm("test");
        Integer maxLifespan = realmResource.toRepresentation().getSsoSessionMaxLifespan();
        RealmManager.realm(realmResource).ssoSessionMaxLifespan(1);

        setTimeOffset(1);

        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken(), "password");

        assertEquals(400, tokenResponse.getStatusCode());
        assertNull(tokenResponse.getAccessToken());
        assertNull(tokenResponse.getRefreshToken());

        RealmManager.realm(realmResource).ssoSessionMaxLifespan(maxLifespan);

        events.expectRefresh(refreshId, sessionId).error(Errors.INVALID_TOKEN);

        events.clear();

        setTimeOffset(0);
    }

    @Test
    public void testCheckSsl() throws Exception {
        Client client = ClientBuilder.newClient();
        UriBuilder builder = UriBuilder.fromUri(AUTH_SERVER_ROOT);
        URI grantUri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
        WebTarget grantTarget = client.target(grantUri);
        builder = UriBuilder.fromUri(AUTH_SERVER_ROOT);
        URI uri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
        WebTarget refreshTarget = client.target(uri);

        String refreshToken = null;
        {
            Response response = executeGrantAccessTokenRequest(grantTarget);
            assertEquals(200, response.getStatus());
            org.keycloak.representations.AccessTokenResponse tokenResponse = response.readEntity(org.keycloak.representations.AccessTokenResponse.class);
            refreshToken = tokenResponse.getRefreshToken();
            response.close();
        }

        {
            Response response = executeRefreshToken(refreshTarget, refreshToken);
            assertEquals(200, response.getStatus());
            org.keycloak.representations.AccessTokenResponse tokenResponse = response.readEntity(org.keycloak.representations.AccessTokenResponse.class);
            refreshToken = tokenResponse.getRefreshToken();
            response.close();
        }

        {   // test checkSsl
            RealmResource realmResource = adminClient.realm("test");
            {
                RealmManager.realm(realmResource).sslRequired(SslRequired.ALL.toString());
            }

            Response response = executeRefreshToken(refreshTarget, refreshToken);
            assertEquals(403, response.getStatus());
            response.close();

            {
                RealmManager.realm(realmResource).sslRequired(SslRequired.EXTERNAL.toString());
            }

        }

        {
            Response response = executeRefreshToken(refreshTarget, refreshToken);
            assertEquals(200, response.getStatus());
            org.keycloak.representations.AccessTokenResponse tokenResponse = response.readEntity(org.keycloak.representations.AccessTokenResponse.class);
            refreshToken = tokenResponse.getRefreshToken();
            response.close();
        }


        client.close();
        events.clear();

    }

    protected Response executeRefreshToken(WebTarget refreshTarget, String refreshToken) {
        String header = BasicAuthHelper.createHeader("test-app", "password");
        Form form = new Form();
        form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.REFRESH_TOKEN);
        form.param("refresh_token", refreshToken);
        return refreshTarget.request()
                .header(HttpHeaders.AUTHORIZATION, header)
                .post(Entity.form(form));
    }

    protected Response executeGrantAccessTokenRequest(WebTarget grantTarget) {
        String header = BasicAuthHelper.createHeader("test-app", "password");
        Form form = new Form();
        form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .param("username", "test-user@localhost")
                .param("password", "password");
        return grantTarget.request()
                .header(HttpHeaders.AUTHORIZATION, header)
                .post(Entity.form(form));
    }


}
