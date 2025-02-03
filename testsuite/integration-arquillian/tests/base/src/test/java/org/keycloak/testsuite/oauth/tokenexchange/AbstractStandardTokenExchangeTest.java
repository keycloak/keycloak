/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.testsuite.oauth.tokenexchange;

import jakarta.ws.rs.core.Response.Status;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.common.Profile;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.arquillian.annotation.UncaughtServerErrorExpected;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import jakarta.ws.rs.core.Response;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

/**
 * Tests for standard token exchange. For now, this class provides set of same tests for token-exchange-v1 as well as for token-exchange-v2.
 *
 * The class may be removed/refactored once V2 implementation will start to differ from V1 (based on new capabilities, adjustments to the specification etc)
 *
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public abstract class AbstractStandardTokenExchangeTest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealmRep = new RealmRepresentation();
        testRealmRep.setId(TEST);
        testRealmRep.setRealm(TEST);
        testRealmRep.setEnabled(true);
        testRealms.add(testRealmRep);
    }

    @Override
    protected boolean isImportAfterEachMethod() {
        return true;
    }

    @Test
    @UncaughtServerErrorExpected
    public void testExchange() throws Exception {
        testingClient.server().run(TokenExchangeTestUtils::setupRealm);

        oauth.realm(TEST);
        oauth.clientId("client-exchanger");
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "user", "password");
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertNotNull(token.getSessionId());
        Assert.assertEquals(token.getPreferredUsername(), "user");
        assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        {
            response = oauth.doTokenExchange(TEST, accessToken, "target", "client-exchanger", "secret");
            Assert.assertEquals(OAuth2Constants.REFRESH_TOKEN_TYPE, response.getIssuedTokenType());
            String exchangedTokenString = response.getAccessToken();
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
            AccessToken exchangedToken = verifier.parse().getToken();
            Assert.assertEquals(token.getSessionId(), exchangedToken.getSessionId());
            Assert.assertEquals("client-exchanger", exchangedToken.getIssuedFor());
            Assert.assertEquals("target", exchangedToken.getAudience()[0]);
            Assert.assertEquals(exchangedToken.getPreferredUsername(), "user");
            assertTrue(exchangedToken.getRealmAccess().isUserInRole("example"));
        }

        {
            response = oauth.doTokenExchange(TEST, accessToken, "target", "legal", "secret");
            Assert.assertEquals(OAuth2Constants.REFRESH_TOKEN_TYPE, response.getIssuedTokenType());
            String exchangedTokenString = response.getAccessToken();
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
            AccessToken exchangedToken = verifier.parse().getToken();
            Assert.assertEquals(token.getSessionId(), exchangedToken.getSessionId());
            Assert.assertEquals("legal", exchangedToken.getIssuedFor());
            Assert.assertEquals("target", exchangedToken.getAudience()[0]);
            Assert.assertEquals(exchangedToken.getPreferredUsername(), "user");
            assertTrue(exchangedToken.getRealmAccess().isUserInRole("example"));
        }
        {
            response = oauth.doTokenExchange(TEST, accessToken, "target", "illegal", "secret");
            Assert.assertEquals(403, response.getStatusCode());
        }
    }

    @Test
    public void testExchangeRequestAccessTokenType() throws Exception {
        testingClient.server().run(TokenExchangeTestUtils::setupRealm);

        oauth.realm(TEST);
        oauth.clientId("client-exchanger");
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "user", "password");
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertNotNull(token.getSessionId());
        Assert.assertEquals(token.getPreferredUsername(), "user");
        assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        {
            response = oauth.doTokenExchange(TEST, accessToken, "target", "client-exchanger", "secret", Map.of(OAuth2Constants.REQUESTED_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE));
            Assert.assertEquals(OAuth2Constants.ACCESS_TOKEN_TYPE, response.getIssuedTokenType());
            String exchangedTokenString = response.getAccessToken();
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
            AccessToken exchangedToken = verifier.parse().getToken();
            Assert.assertEquals(token.getSessionId(), exchangedToken.getSessionId());
            Assert.assertEquals("client-exchanger", exchangedToken.getIssuedFor());
            Assert.assertEquals("target", exchangedToken.getAudience()[0]);
            Assert.assertEquals(exchangedToken.getPreferredUsername(), "user");
            assertTrue(exchangedToken.getRealmAccess().isUserInRole("example"));
        }
    }

    @Test
    @UncaughtServerErrorExpected
    public void testExchangeUsingServiceAccount() throws Exception {
        testingClient.server().run(TokenExchangeTestUtils::setupRealm);

        oauth.realm(TEST);
        oauth.clientId("my-service-account");
        OAuthClient.AccessTokenResponse response = oauth.doClientCredentialsGrantAccessTokenRequest("secret");
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertNull(token.getSessionId());

        {
            response = oauth.doTokenExchange(TEST, accessToken, "target", "my-service-account", "secret");
            Assert.assertEquals(OAuth2Constants.ACCESS_TOKEN_TYPE, response.getIssuedTokenType());
            String exchangedTokenString = response.getAccessToken();
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
            AccessToken exchangedToken = verifier.parse().getToken();
            Assert.assertNull(exchangedToken.getSessionId());
            Assert.assertEquals("my-service-account", exchangedToken.getIssuedFor());
            Assert.assertEquals("target", exchangedToken.getAudience()[0]);
            Assert.assertEquals(exchangedToken.getPreferredUsername(), "service-account-my-service-account");
        }
    }

    @Test
    @UncaughtServerErrorExpected
    public void testExchangeDifferentScopes() throws Exception {
        testingClient.server().run(TokenExchangeTestUtils::setupRealm);

        oauth.realm(TEST);
        oauth.clientId("client-exchanger");
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "user", "password");
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        Assert.assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        {
            response = oauth.doTokenExchange(TEST, accessToken, null, "different-scope-client", "secret");
            String exchangedTokenString = response.getAccessToken();
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
            AccessToken exchangedToken = verifier.parse().getToken();
            Assert.assertEquals("different-scope-client", exchangedToken.getIssuedFor());
            Assert.assertNull(exchangedToken.getAudience());
            Assert.assertEquals(exchangedToken.getPreferredUsername(), "user");
            Assert.assertNames(Arrays.asList(exchangedToken.getScope().split(" ")),"profile","openid");
            Assert.assertNull(exchangedToken.getEmailVerified());
        }

        {
            response = oauth.doTokenExchange(TEST, accessToken, "target", "different-scope-client", "secret");
            String exchangedTokenString = response.getAccessToken();
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
            AccessToken exchangedToken = verifier.parse().getToken();
            Assert.assertEquals("different-scope-client", exchangedToken.getIssuedFor());
            Assert.assertEquals("target", exchangedToken.getAudience()[0]);
            Assert.assertEquals(exchangedToken.getPreferredUsername(), "user");
            Assert.assertTrue(exchangedToken.getRealmAccess().isUserInRole("example"));
            Assert.assertNames(Arrays.asList(exchangedToken.getScope().split(" ")),"profile","email","openid");
            Assert.assertFalse(exchangedToken.getEmailVerified());
        }

    }

    @Test
    @UncaughtServerErrorExpected
    public void testExchangeDifferentScopesWithScopeParameter() throws Exception {
        testingClient.server().run(TokenExchangeTestUtils::setupRealm);

        oauth.realm(TEST);
        oauth.clientId("client-exchanger");
        oauth.scope("openid profile email phone");
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "user", "password");
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        Assert.assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));
        Assert.assertNames(Arrays.asList(token.getScope().split(" ")),"profile", "email", "openid", "phone");
        //change scopes for token exchange - profile,phone must be removed
        oauth.scope("openid profile email");

        {
            response = oauth.doTokenExchange(TEST, accessToken, null, "different-scope-client", "secret");
            String exchangedTokenString = response.getAccessToken();
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
            AccessToken exchangedToken = verifier.parse().getToken();
            Assert.assertEquals("different-scope-client", exchangedToken.getIssuedFor());
            Assert.assertNull(exchangedToken.getAudience());
            Assert.assertEquals(exchangedToken.getPreferredUsername(), "user");
            Assert.assertNames(Arrays.asList(exchangedToken.getScope().split(" ")),"profile", "openid");
            Assert.assertNull(exchangedToken.getEmailVerified());
        }

        {
            response = oauth.doTokenExchange(TEST, accessToken, "target", "different-scope-client", "secret");
            String exchangedTokenString = response.getAccessToken();
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
            AccessToken exchangedToken = verifier.parse().getToken();
            Assert.assertEquals("different-scope-client", exchangedToken.getIssuedFor());
            Assert.assertEquals("target", exchangedToken.getAudience()[0]);
            Assert.assertEquals(exchangedToken.getPreferredUsername(), "user");
            Assert.assertTrue(exchangedToken.getRealmAccess().isUserInRole("example"));
            Assert.assertNames(Arrays.asList(exchangedToken.getScope().split(" ")),"profile", "email","openid");
            Assert.assertFalse(exchangedToken.getEmailVerified());
        }
        oauth.scope(null);
    }

    @Test
    @UncaughtServerErrorExpected
    public void testExchangeFromPublicClient() throws Exception {
        testingClient.server().run(TokenExchangeTestUtils::setupRealm);

        oauth.realm(TEST);
        oauth.clientId("direct-public");
        OAuthClient.AuthorizationEndpointResponse authzResponse = oauth.doLogin("user", "password");
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(authzResponse.getCode(), "secret");

        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        response = oauth.doTokenExchange(TEST, accessToken, "target", "client-exchanger", "secret");
        String exchangedTokenString = response.getAccessToken();
        TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
        AccessToken exchangedToken = verifier.parse().getToken();
        Assert.assertEquals("client-exchanger", exchangedToken.getIssuedFor());
        Assert.assertEquals("target", exchangedToken.getAudience()[0]);
        Assert.assertEquals(exchangedToken.getPreferredUsername(), "user");
        assertTrue(exchangedToken.getRealmAccess().isUserInRole("example"));

        // can exchange to itself because the client is within the audience of the token issued to the public client
        response = oauth.doTokenExchange(TEST, accessToken, null, "client-exchanger", "secret");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());

        // can not exchange to itself because the client is not within the audience of the token issued to the public client
        response = oauth.doTokenExchange(TEST, accessToken, null, "direct-legal", "secret");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatusCode());

        response = oauth.doTokenExchange(TEST, accessToken, null, "direct-public", null);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
    }

    @Test
    @UncaughtServerErrorExpected
    public void testExchangeNoRefreshToken() throws Exception {
        testingClient.server().run(TokenExchangeTestUtils::setupRealm);

        oauth.realm(TEST);
        oauth.clientId("client-exchanger");

        ClientResource client = ApiUtil.findClientByClientId(adminClient.realm(TEST), "no-refresh-token");
        ClientRepresentation clientRepresentation = client.toRepresentation();
        clientRepresentation.getAttributes().put(OIDCConfigAttributes.USE_REFRESH_TOKEN, "false");
        client.update(clientRepresentation);

        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "user", "password");
        String accessToken = response.getAccessToken();

        {
            response = oauth.doTokenExchange(TEST, accessToken, "target", "client-exchanger", "secret");
            String exchangedTokenString = response.getAccessToken();
            String refreshTokenString = response.getRefreshToken();
            assertNotNull(exchangedTokenString);
            assertNotNull(refreshTokenString);
        }

        {
            response = oauth.doTokenExchange(TEST, accessToken, "target", "no-refresh-token", "secret");
            String exchangedTokenString = response.getAccessToken();
            String refreshTokenString = response.getRefreshToken();
            assertNotNull(exchangedTokenString);
            assertNull(refreshTokenString);
        }
        clientRepresentation.getAttributes().put(OIDCConfigAttributes.USE_REFRESH_TOKEN, "true");
        client.update(clientRepresentation);
    }

    @Test
    public void testClientExchangeToItself() throws Exception {
        testingClient.server().run(TokenExchangeTestUtils::setupRealm);

        oauth.realm(TEST);
        oauth.clientId("client-exchanger");
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "user", "password");
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        response = oauth.doTokenExchange(TEST, accessToken, null, "client-exchanger", "secret");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());

        response = oauth.doTokenExchange(TEST, accessToken, "client-exchanger", "client-exchanger", "secret");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
    }

    @Test
    public void testClientExchangeToItselfWithConsents() throws Exception {
        testingClient.server().run(TokenExchangeTestUtils::setupRealm);

        oauth.realm(TEST);
        oauth.clientId("client-exchanger");
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "user", "password");
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        ClientResource client = ApiUtil.findClientByClientId(adminClient.realm(TEST), "client-exchanger");
        ClientRepresentation clientRepresentation = client.toRepresentation();
        clientRepresentation.setConsentRequired(Boolean.TRUE);
        client.update(clientRepresentation);

        response = oauth.doTokenExchange(TEST, accessToken, null, "client-exchanger", "secret");
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_CLIENT, response.getError());
        assertEquals("Client requires user consent", response.getErrorDescription());

        response = oauth.doTokenExchange(TEST, accessToken, "client-exchanger", "client-exchanger", "secret");
        assertEquals(OAuthErrorException.INVALID_CLIENT, response.getError());
        assertEquals("Client requires user consent", response.getErrorDescription());
    }

    @Test
    public void testClientExchange() throws Exception {
        testingClient.server().run(TokenExchangeTestUtils::setupRealm);

        oauth.realm(TEST);
        oauth.clientId("direct-legal");
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "user", "password");
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        response = oauth.doTokenExchange(TEST, accessToken, "target", "direct-legal", "secret");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
    }

    @Test
    public void testClientExchangeWithMoreAudiencesNotBreak() throws Exception {
        testingClient.server().run(TokenExchangeTestUtils::setupRealm);

        oauth.realm(TEST);
        oauth.clientId("client-exchanger");
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "user", "password");
        String accessToken = response.getAccessToken();

        response = oauth.doTokenExchange(TEST, accessToken, List.of("target", "client-exchanger"), "client-exchanger", "secret", null);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
    }

    @Test
    public void testPublicClientNotAllowed() throws Exception {
        testingClient.server().run(TokenExchangeTestUtils::setupRealm);

        oauth.realm(TEST);
        oauth.clientId("direct-legal");
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "user", "password");
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        // public client has no permission to exchange with the client direct-legal to which the token was issued for
        // if not set, the audience is calculated based on the client to which the token was issued for
        response = oauth.doTokenExchange(TEST, accessToken, null, "direct-public", null);
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatusCode());
        assertEquals("Client is not the holder of the token", response.getErrorDescription());

        // public client has no permission to exchange
        response = oauth.doTokenExchange(TEST, accessToken, "target", "direct-public", null);
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatusCode());
        assertEquals("Client is not the holder of the token", response.getErrorDescription());

        response = oauth.doTokenExchange(TEST, accessToken, "direct-legal", "direct-public", null);
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatusCode());
        assertEquals("Client is not the holder of the token", response.getErrorDescription());

        // public client can not exchange a token to itself if the token was issued to another client
        response = oauth.doTokenExchange(TEST, accessToken, "direct-public", "direct-public", null);
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatusCode());
        assertEquals("Client is not the holder of the token", response.getErrorDescription());

        // client with access to exchange
        response = oauth.doTokenExchange(TEST, accessToken, "target", "client-exchanger", "secret");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());

        // client must pass the audience because the client has no permission to exchange with the calculated audience (direct-legal)
        response = oauth.doTokenExchange(TEST, accessToken, null, "client-exchanger", "secret");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatusCode());
        assertEquals("Client is not within the token audience", response.getErrorDescription());
    }

    @Test
    @EnableFeature(value = Profile.Feature.DYNAMIC_SCOPES, skipRestart = true)
    @UncaughtServerErrorExpected
    public void testExchangeWithDynamicScopesEnabled() throws Exception {
        testExchange();
    }

    @Test
    public void testSupportedTokenTypesWhenValidatingSubjectToken() throws Exception {
        testingClient.server().run(TokenExchangeTestUtils::setupRealm);
        oauth.realm(TEST);
        oauth.clientId("direct-legal");
        oauth.scope(OAuth2Constants.SCOPE_OPENID);
        ClientsResource clients = adminClient.realm(oauth.getRealm()).clients();
        ClientRepresentation rep = clients.findByClientId(oauth.getClientId()).get(0);
        rep.getAttributes().put(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL, oauth.APP_ROOT + "/admin/backchannelLogout");
        getCleanup().addCleanup(() -> {
            rep.getAttributes().put(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL, "");
            clients.get(rep.getId()).update(rep);
        });
        clients.get(rep.getId()).update(rep);
        String logoutToken;
        oauth.clientSessionState("client-session");
        oauth.doLogin("user", "password");
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "secret");
        String idTokenString = tokenResponse.getIdToken();
        String logoutUrl = oauth.getLogoutUrl().idTokenHint(idTokenString)
                .postLogoutRedirectUri(oauth.APP_AUTH_ROOT).build();
        driver.navigate().to(logoutUrl);
        logoutToken = testingClient.testApp().getBackChannelRawLogoutToken();
        Assert.assertNotNull(logoutToken);
        OAuthClient.AccessTokenResponse response = oauth.doTokenExchange(TEST, logoutToken, "target", "direct-legal", "secret");
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());

    }

    @Test
    public void testExchangeForDifferentClient() throws Exception {
        testingClient.server().run(TokenExchangeTestUtils::setupRealm);

        // generate the first token for a public client
        oauth.realm(TEST);
        oauth.clientId("direct-public");
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "user", "password");
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));
        Assert.assertNotNull(token.getSessionId());
        String sid = token.getSessionId();

        // perform token exchange with client-exchanger simulating it received the previous token
        response = oauth.doTokenExchange(TEST, accessToken, "target", "client-exchanger", "secret");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
        accessToken = response.getAccessToken();
        accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals("client-exchanger", token.getIssuedFor());
        Assert.assertEquals("target", token.getAudience()[0]);
        Assert.assertEquals(token.getPreferredUsername(), "user");
        Assert.assertEquals(sid, token.getSessionId());

        // perform a second token exchange just to check everything is OK
        response = oauth.doTokenExchange(TEST, accessToken, "target", "client-exchanger", "secret");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
        accessToken = response.getAccessToken();
        accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals("client-exchanger", token.getIssuedFor());
        Assert.assertEquals("target", token.getAudience()[0]);
        Assert.assertEquals(token.getPreferredUsername(), "user");
        Assert.assertEquals(sid, token.getSessionId());
    }
}
