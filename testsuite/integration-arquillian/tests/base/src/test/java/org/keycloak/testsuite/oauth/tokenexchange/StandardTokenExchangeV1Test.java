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

import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.core.Response;

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
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.arquillian.annotation.UncaughtServerErrorExpected;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for standard token exchange (internal-internal) and token-exchange-v1
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@EnableFeature(value = Profile.Feature.TOKEN_EXCHANGE, skipRestart = true)
@EnableFeature(value = Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ, skipRestart = true)
@DisableFeature(value = Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ_V2, skipRestart = true)
public class StandardTokenExchangeV1Test extends AbstractKeycloakTest {

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

    protected void setupRealm() {
        testingClient.server().run(TokenExchangeTestUtils::setupRealm);
    }

    protected String getInitialAccessTokenForClientExchanger() throws Exception {
        oauth.client("client-exchanger", "secret");
        AccessTokenResponse response = oauth.doPasswordGrantRequest("user", "password");
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertNotNull(token.getSessionId());
        Assert.assertEquals(token.getPreferredUsername(), "user");
        assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));
        return accessToken;
    }

    protected String getSessionIdFromToken(String accessToken) throws Exception {
        return TokenVerifier.create(accessToken, AccessToken.class)
                .parse()
                .getToken()
                .getSessionId();
    }

    @Test
    @UncaughtServerErrorExpected
    public void testExchange() throws Exception {
        setupRealm();

        oauth.realm(TEST);
        String accessToken = getInitialAccessTokenForClientExchanger();

        {
            oauth.client("client-exchanger", "secret");
            AccessTokenResponse response = oauth.tokenExchangeRequest(accessToken).audience("target").send();
            Assert.assertEquals(OAuth2Constants.REFRESH_TOKEN_TYPE, response.getIssuedTokenType());
            String exchangedTokenString = response.getAccessToken();
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
            AccessToken exchangedToken = verifier.parse().getToken();
            Assert.assertEquals(getSessionIdFromToken(accessToken), exchangedToken.getSessionId());
            Assert.assertEquals("client-exchanger", exchangedToken.getIssuedFor());
            Assert.assertEquals("target", exchangedToken.getAudience()[0]);
            Assert.assertEquals(exchangedToken.getPreferredUsername(), "user");
            assertTrue(exchangedToken.getRealmAccess().isUserInRole("example"));
        }

        {
            oauth.client("legal", "secret");
            AccessTokenResponse response = oauth.tokenExchangeRequest(accessToken).audience("target").send();
            Assert.assertEquals(OAuth2Constants.REFRESH_TOKEN_TYPE, response.getIssuedTokenType());
            String exchangedTokenString = response.getAccessToken();
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
            AccessToken exchangedToken = verifier.parse().getToken();
            Assert.assertEquals(getSessionIdFromToken(accessToken), exchangedToken.getSessionId());
            Assert.assertEquals("legal", exchangedToken.getIssuedFor());
            Assert.assertEquals("target", exchangedToken.getAudience()[0]);
            Assert.assertEquals(exchangedToken.getPreferredUsername(), "user");
            assertTrue(exchangedToken.getRealmAccess().isUserInRole("example"));
        }
        {
            oauth.client("illegal", "secret");
            AccessTokenResponse response = oauth.tokenExchangeRequest(accessToken).audience("target").send();
            Assert.assertEquals(403, response.getStatusCode());
        }
    }

    @Test
    public void testExchangeRequestAccessTokenType() throws Exception {
        setupRealm();

        oauth.realm(TEST);
        String accessToken = getInitialAccessTokenForClientExchanger();

        {
            oauth.client("client-exchanger", "secret");
            AccessTokenResponse response = oauth.tokenExchangeRequest(accessToken).audience("target").requestedTokenType(OAuth2Constants.ACCESS_TOKEN_TYPE).send();
            Assert.assertEquals(OAuth2Constants.ACCESS_TOKEN_TYPE, response.getIssuedTokenType());
            String exchangedTokenString = response.getAccessToken();
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
            AccessToken exchangedToken = verifier.parse().getToken();
            Assert.assertEquals(getSessionIdFromToken(accessToken), exchangedToken.getSessionId());
            Assert.assertEquals("client-exchanger", exchangedToken.getIssuedFor());
            Assert.assertEquals("target", exchangedToken.getAudience()[0]);
            Assert.assertEquals(exchangedToken.getPreferredUsername(), "user");
            assertTrue(exchangedToken.getRealmAccess().isUserInRole("example"));
        }
    }

    @Test
    @UncaughtServerErrorExpected
    public void testExchangeUsingServiceAccount() throws Exception {
        setupRealm();

        oauth.realm(TEST);
        oauth.client("my-service-account", "secret");
        AccessTokenResponse response = oauth.doClientCredentialsGrantAccessTokenRequest();
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertNull(token.getSessionId());

        {
            response = oauth.tokenExchangeRequest(accessToken).audience("target").send();
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
        setupRealm();

        oauth.realm(TEST);
        String accessToken = getInitialAccessTokenForClientExchanger();

        oauth.client("different-scope-client", "secret");
        {
            AccessTokenResponse response = oauth.doTokenExchange(accessToken);
            String exchangedTokenString = response.getAccessToken();
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
            AccessToken exchangedToken = verifier.parse().getToken();
            Assert.assertEquals("different-scope-client", exchangedToken.getIssuedFor());
            Assert.assertNull(exchangedToken.getAudience());
            Assert.assertEquals(exchangedToken.getPreferredUsername(), "user");
            String[] expectedScopes = new String[] { "profile", "openid" };
            Assert.assertNames(Arrays.asList(exchangedToken.getScope().split(" ")), expectedScopes);
            Assert.assertNull(exchangedToken.getEmailVerified());
        }

        {
            AccessTokenResponse response = oauth.tokenExchangeRequest(accessToken).audience("target").send();
            String exchangedTokenString = response.getAccessToken();
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
            AccessToken exchangedToken = verifier.parse().getToken();
            Assert.assertEquals("different-scope-client", exchangedToken.getIssuedFor());
            Assert.assertEquals("target", exchangedToken.getAudience()[0]);
            Assert.assertEquals(exchangedToken.getPreferredUsername(), "user");
            Assert.assertTrue(exchangedToken.getRealmAccess().isUserInRole("example"));
            String[] expectedScopes = new String[] { "profile", "email", "openid" };
            Assert.assertNames(Arrays.asList(exchangedToken.getScope().split(" ")),expectedScopes);
            Assert.assertFalse(exchangedToken.getEmailVerified());
        }

    }

    @Test
    @UncaughtServerErrorExpected
    public void testExchangeDifferentScopesWithScopeParameter() throws Exception {
        setupRealm();

        oauth.realm(TEST);
        oauth.client("client-exchanger", "secret");
        oauth.scope("openid profile email phone");
        AccessTokenResponse response = oauth.doPasswordGrantRequest("user", "password");
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        Assert.assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));
        Assert.assertNames(Arrays.asList(token.getScope().split(" ")),"profile", "email", "openid", "phone");
        //change scopes for token exchange - profile,phone must be removed
        oauth.scope("openid profile email");
        oauth.client("different-scope-client", "secret");
        {
            response = oauth.doTokenExchange(accessToken);
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
            response = oauth.tokenExchangeRequest(accessToken).audience("target").send();
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
        setupRealm();

        oauth.realm(TEST);
        oauth.client("direct-public", "secret");
        AuthorizationEndpointResponse authzResponse = oauth.doLogin("user", "password");
        AccessTokenResponse response = oauth.doAccessTokenRequest(authzResponse.getCode());

        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        oauth.client("client-exchanger", "secret");

        response = oauth.tokenExchangeRequest(accessToken).audience("target").send();
        String exchangedTokenString = response.getAccessToken();
        TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
        AccessToken exchangedToken = verifier.parse().getToken();
        Assert.assertEquals("client-exchanger", exchangedToken.getIssuedFor());
        Assert.assertEquals("target", exchangedToken.getAudience()[0]);
        Assert.assertEquals(exchangedToken.getPreferredUsername(), "user");
        assertTrue(exchangedToken.getRealmAccess().isUserInRole("example"));

        // can exchange to itself because the client is within the audience of the token issued to the public client
        response = oauth.doTokenExchange(accessToken);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());

        oauth.client("direct-legal", "secret");

        // can not exchange to itself because the client is not within the audience of the token issued to the public client
        response = oauth.doTokenExchange(accessToken);
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatusCode());

        oauth.client("direct-public");

        response = oauth.doTokenExchange(accessToken);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
    }

    @Test
    @UncaughtServerErrorExpected
    public void testExchangeNoRefreshToken() throws Exception {
        setupRealm();

        oauth.realm(TEST);
        oauth.client("client-exchanger", "secret");

        ClientResource client = ApiUtil.findClientByClientId(adminClient.realm(TEST), "no-refresh-token");
        ClientRepresentation clientRepresentation = client.toRepresentation();
        clientRepresentation.getAttributes().put(OIDCConfigAttributes.USE_REFRESH_TOKEN, "false");
        client.update(clientRepresentation);

        AccessTokenResponse response = oauth.doPasswordGrantRequest("user", "password");
        String accessToken = response.getAccessToken();

        {
            response = oauth.tokenExchangeRequest(accessToken).audience("target").send();
            String exchangedTokenString = response.getAccessToken();
            String refreshTokenString = response.getRefreshToken();
            assertNotNull(exchangedTokenString);
            assertNotNull(refreshTokenString);
        }

        {
            oauth.client("no-refresh-token", "secret");
            response = oauth.tokenExchangeRequest(accessToken).audience("target").send();
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
        setupRealm();

        oauth.realm(TEST);
        oauth.client("client-exchanger", "secret");
        String accessToken = getInitialAccessTokenForClientExchanger();

        AccessTokenResponse response = oauth.doTokenExchange(accessToken);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());

        response = oauth.tokenExchangeRequest(accessToken).audience("client-exchanger").send();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
    }

    @Test
    public void testClientExchangeToItselfWithConsents() throws Exception {
        setupRealm();

        oauth.realm(TEST);
        String accessToken = getInitialAccessTokenForClientExchanger();

        ClientResource client = ApiUtil.findClientByClientId(adminClient.realm(TEST), "client-exchanger");
        ClientRepresentation clientRepresentation = client.toRepresentation();
        clientRepresentation.setConsentRequired(Boolean.TRUE);
        client.update(clientRepresentation);

        oauth.client("client-exchanger", "secret");

        AccessTokenResponse response = oauth.doTokenExchange(accessToken);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_CLIENT, response.getError());
        assertEquals("Client requires user consent", response.getErrorDescription());

        response = oauth.tokenExchangeRequest(accessToken).audience("client-exchanger").send();
        assertEquals(OAuthErrorException.INVALID_CLIENT, response.getError());
        assertEquals("Client requires user consent", response.getErrorDescription());
    }

    @Test
    public void testClientExchange() throws Exception {
        setupRealm();

        oauth.realm(TEST);
        oauth.client("direct-legal", "secret");
        AccessTokenResponse response = oauth.doPasswordGrantRequest("user", "password");
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        response = oauth.tokenExchangeRequest(accessToken).audience("target").send();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
    }

    @Test
    public void testClientExchangeWithMoreAudiencesNotBreak() throws Exception {
        setupRealm();

        oauth.realm(TEST);
        oauth.client("client-exchanger", "secret");
        AccessTokenResponse response = oauth.doPasswordGrantRequest("user", "password");
        String accessToken = response.getAccessToken();

        response = oauth.tokenExchangeRequest(accessToken).audience("target", "client-exchanger").send();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
    }

    @Test
    public void testPublicClientNotAllowed() throws Exception {
        setupRealm();

        oauth.realm(TEST);
        oauth.client("direct-legal", "secret");
        AccessTokenResponse response = oauth.doPasswordGrantRequest("user", "password");
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        // public client has no permission to exchange with the client direct-legal to which the token was issued for
        // if not set, the audience is calculated based on the client to which the token was issued for
        oauth.client("direct-public");

        response = oauth.doTokenExchange(accessToken);
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatusCode());
        assertEquals("Client is not the holder of the token", response.getErrorDescription());

        // public client has no permission to exchange
        response = oauth.tokenExchangeRequest(accessToken).audience("target").send();
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatusCode());
        assertEquals("Client is not the holder of the token", response.getErrorDescription());

        response = oauth.tokenExchangeRequest(accessToken).audience("direct-legal").send();
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatusCode());
        assertEquals("Client is not the holder of the token", response.getErrorDescription());

        // public client can not exchange a token to itself if the token was issued to another client
        response = oauth.tokenExchangeRequest(accessToken).audience("direct-public").send();
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatusCode());
        assertEquals("Client is not the holder of the token", response.getErrorDescription());

        oauth.client("client-exchanger", "secret");

        // client with access to exchange
        response = oauth.tokenExchangeRequest(accessToken).audience("target").send();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());

        // client must pass the audience because the client has no permission to exchange with the calculated audience (direct-legal)
        response = oauth.doTokenExchange(accessToken);
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
        setupRealm();
        oauth.realm(TEST);
        oauth.client("direct-legal", "secret");
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
        oauth.doLogin("user", "password");
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        String idTokenString = tokenResponse.getIdToken();
        oauth.logoutForm().idTokenHint(idTokenString)
                .postLogoutRedirectUri(oauth.APP_AUTH_ROOT).open();
        logoutToken = testingClient.testApp().getBackChannelRawLogoutToken();
        Assert.assertNotNull(logoutToken);
        AccessTokenResponse response = oauth.tokenExchangeRequest(logoutToken).audience("target").send();
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());

    }

    @Test
    public void testExchangeForDifferentClient() throws Exception {
        setupRealm();

        // generate the first token for a public client
        oauth.realm(TEST);
        oauth.client("direct-public");
        AccessTokenResponse response = oauth.doPasswordGrantRequest("user", "password");
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));
        Assert.assertNotNull(token.getSessionId());
        String sid = token.getSessionId();

        oauth.client("client-exchanger", "secret");

        // perform token exchange with client-exchanger simulating it received the previous token
        response = oauth.tokenExchangeRequest(accessToken).audience("target").send();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
        accessToken = response.getAccessToken();
        accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals("client-exchanger", token.getIssuedFor());
        Assert.assertEquals("target", token.getAudience()[0]);
        Assert.assertEquals(token.getPreferredUsername(), "user");
        Assert.assertEquals(sid, token.getSessionId());

        // perform a second token exchange just to check everything is OK
        response = oauth.tokenExchangeRequest(accessToken).audience("target").send();
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
