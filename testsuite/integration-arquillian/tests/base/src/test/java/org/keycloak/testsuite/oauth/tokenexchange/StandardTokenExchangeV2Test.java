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

import jakarta.ws.rs.core.Response;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.resource.ClientResource;
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
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@EnableFeature(value = Profile.Feature.TOKEN_EXCHANGE_STANDARD_V2, skipRestart = true)
public class StandardTokenExchangeV2Test extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealm = loadJson(getClass().getResourceAsStream("/token-exchange/testrealm-token-exchange-v2.json"), RealmRepresentation.class);
        testRealms.add(testRealm);
    }

    protected String getSessionIdFromToken(String accessToken) throws Exception {
        return TokenVerifier.create(accessToken, AccessToken.class)
                .parse()
                .getToken()
                .getSessionId();
    }

    private String resourceOwnerLogin(String username, String password, String clientId, String secret) throws Exception {
        oauth.realm(TEST);
        oauth.client(clientId, secret);
        oauth.scope(null);
        oauth.openid(false);
        AccessTokenResponse response = oauth.doGrantAccessTokenRequest(username, password);
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(response.getAccessToken(), AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        return response.getAccessToken();
    }

    private AccessTokenResponse tokenExchange(String subjectToken, String clientId, String secret, List<String> audience, Map<String, String> additionalParams) {
        return oauth.tokenExchangeRequest(subjectToken).client(clientId, secret).audience(audience).additionalParams(additionalParams).send();
    }

    @Test
    @UncaughtServerErrorExpected
    public void testExchange() throws Exception {
        oauth.realm(TEST);
        String accessToken = resourceOwnerLogin("john", "password", "subject-client", "secret");
        {
            AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret", null, null);
            assertEquals(OAuth2Constants.REFRESH_TOKEN_TYPE, response.getIssuedTokenType());
            String exchangedTokenString = response.getAccessToken();
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
            AccessToken exchangedToken = verifier.parse().getToken();
            assertEquals(getSessionIdFromToken(accessToken), exchangedToken.getSessionId());
            assertEquals("requester-client", exchangedToken.getIssuedFor());
        }
        {
            //exchange not allowed due the invalid client is not in the subject-client audience
            AccessTokenResponse response = tokenExchange(accessToken, "invalid-requester-client", "secret", null, null);
            assertEquals(403, response.getStatusCode());
        }
    }

    @Test
    public void testExchangeRequestAccessTokenType() throws Exception {
        oauth.realm(TEST);
        String accessToken = resourceOwnerLogin("john", "password","subject-client", "secret");
        AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret", null, Map.of(OAuth2Constants.REQUESTED_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE));
        assertEquals(OAuth2Constants.ACCESS_TOKEN_TYPE, response.getIssuedTokenType());
        String exchangedTokenString = response.getAccessToken();
        TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
        AccessToken exchangedToken = verifier.parse().getToken();
        assertEquals(getSessionIdFromToken(accessToken), exchangedToken.getSessionId());
        assertEquals("requester-client", exchangedToken.getIssuedFor());
    }

    @Test
    @UncaughtServerErrorExpected
    public void testExchangeUsingServiceAccount() throws Exception {
        oauth.realm(TEST);
        oauth.client("subject-client", "secret");
        AccessTokenResponse response = oauth.doClientCredentialsGrantAccessTokenRequest();
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertNull(token.getSessionId());
        response = tokenExchange(accessToken, "requester-client", "secret", null, null);
        assertEquals(OAuth2Constants.ACCESS_TOKEN_TYPE, response.getIssuedTokenType());
        String exchangedTokenString = response.getAccessToken();
        TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
        AccessToken exchangedToken = verifier.parse().getToken();
        Assert.assertNull(exchangedToken.getSessionId());
        assertEquals("requester-client", exchangedToken.getIssuedFor());

    }

    @Test
    @UncaughtServerErrorExpected
    public void testExchangeNoRefreshToken() throws Exception {


        String accessToken = resourceOwnerLogin("john", "password","subject-client", "secret");
        {
            AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret", null, null);
            String exchangedTokenString = response.getAccessToken();
            String refreshTokenString = response.getRefreshToken();
            assertNotNull(exchangedTokenString);
            assertNotNull(refreshTokenString);
        }

        ClientResource client = ApiUtil.findClientByClientId(adminClient.realm(TEST), "requester-client");
        ClientRepresentation clientRepresentation = client.toRepresentation();
        clientRepresentation.getAttributes().put(OIDCConfigAttributes.USE_REFRESH_TOKEN, "false");
        client.update(clientRepresentation);

        {
            AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret", null, null);
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
        oauth.realm(TEST);
        String accessToken = resourceOwnerLogin("john", "password","subject-client", "secret");

        AccessTokenResponse response = tokenExchange(accessToken, "subject-client", "secret", null, null);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());

        response = tokenExchange(accessToken, "subject-client", "secret", List.of("subject-client"), null);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
    }

    @Test
    public void testClientExchangeToItselfWithConsents() throws Exception {
        oauth.realm(TEST);
        String accessToken = resourceOwnerLogin("john", "password","subject-client", "secret");

        ClientResource client = ApiUtil.findClientByClientId(adminClient.realm(TEST), "subject-client");
        ClientRepresentation clientRepresentation = client.toRepresentation();
        clientRepresentation.setConsentRequired(Boolean.TRUE);
        client.update(clientRepresentation);

        AccessTokenResponse response = tokenExchange(accessToken, "subject-client", "secret", null, null);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_CLIENT, response.getError());
        assertEquals("Client requires user consent", response.getErrorDescription());

        response = tokenExchange(accessToken, "subject-client", "secret", List.of("subject-client"), null);
        assertEquals(OAuthErrorException.INVALID_CLIENT, response.getError());
        assertEquals("Client requires user consent", response.getErrorDescription());

        clientRepresentation.setConsentRequired(Boolean.FALSE);
        client.update(clientRepresentation);
    }

    @Test
    public void testOptionalScopeParamRequestedWithoutAudience() throws Exception {
        String accessToken = resourceOwnerLogin("john", "password","subject-client", "secret");
        oauth.scope("optional-scope2");
        AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret", null, null);
        assertAudiencesAndScopes(response, List.of("target-client1", "target-client2"), List.of("default-scope1", "optional-scope2"));
    }

    @Test
    public void testAudienceRequested() throws Exception {
        String accessToken = resourceOwnerLogin("john", "password","subject-client", "secret");
        AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret", List.of("target-client1"), null);
        assertAudiencesAndScopes(response, List.of("target-client1"), List.of("default-scope1"));
    }

    @Test
    public void testUnavailableAudienceRequested() throws Exception {
        String accessToken = resourceOwnerLogin("john", "password","subject-client", "secret");
        // request invalid client audience
        AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret",  List.of("target-client1", "invalid-client"), null);
        org.junit.Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        org.junit.Assert.assertEquals(OAuthErrorException.INVALID_CLIENT, response.getError());
        org.junit.Assert.assertEquals("Audience not found", response.getErrorDescription());
        // The "target-client3" is valid client, but unavailable to the user. Request allowed, but "target-client3" audience will not be available
        response = tokenExchange(accessToken, "requester-client", "secret",  List.of("target-client1", "target-client3"), null);
        org.junit.Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        org.junit.Assert.assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        org.junit.Assert.assertEquals("Requested audience not available: target-client3", response.getErrorDescription());
    }

    @Test
    public void testScopeNotAllowed() throws Exception {
        String accessToken = resourceOwnerLogin("john", "password", "subject-client", "secret");

        //scope not allowed
        oauth.scope("optional-scope3");
        AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret",  List.of("target-client1", "target-client3"), null);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        org.junit.Assert.assertEquals(OAuthErrorException.INVALID_SCOPE, response.getError());
        org.junit.Assert.assertEquals("Invalid scopes: optional-scope3", response.getErrorDescription());

        //scope that doesn't exist
        oauth.scope("bad-scope");
        response = tokenExchange(accessToken, "requester-client", "secret",  List.of("target-client1", "target-client3"), null);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        org.junit.Assert.assertEquals(OAuthErrorException.INVALID_SCOPE, response.getError());
        org.junit.Assert.assertEquals("Invalid scopes: bad-scope", response.getErrorDescription());
    }

    @Test
    public void testScopeFilter() throws Exception {
        String accessToken = resourceOwnerLogin("john", "password","subject-client", "secret");
        AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret",  List.of("target-client2"), null);
        org.junit.Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        org.junit.Assert.assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        org.junit.Assert.assertEquals("Requested audience not available: target-client2", response.getErrorDescription());

        oauth.scope("optional-scope2");
        response = tokenExchange(accessToken, "requester-client", "secret",  List.of("target-client2"), null);
        assertAudiencesAndScopes(response, List.of("target-client2"), List.of("optional-scope2"));

        oauth.scope("optional-scope2");
        response = tokenExchange(accessToken, "requester-client", "secret",  List.of("target-client1", "target-client2"), null);
        assertAudiencesAndScopes(response, List.of("target-client1", "target-client2"), List.of("default-scope1", "optional-scope2"));

        //just check that the exchanged token contains the optional-scope2 mapped by the realm role
        accessToken = resourceOwnerLogin("mike", "password","subject-client", "secret");
        oauth.scope("optional-scope2");
        response = tokenExchange(accessToken, "requester-client", "secret",  null, null);
        assertAudiencesAndScopes(response, List.of("target-client1"), List.of("default-scope1", "optional-scope2"));

        accessToken = resourceOwnerLogin("mike", "password","subject-client", "secret");
        oauth.scope("optional-scope2");
        response = tokenExchange(accessToken, "requester-client", "secret",  List.of("target-client1"), null);
        assertAudiencesAndScopes(response,  List.of("target-client1"), List.of("default-scope1", "optional-scope2"));
    }

    @Test
    public void testScopeParamIncludedAudienceIncludedRefreshToken() throws Exception {
        String accessToken = resourceOwnerLogin("mike", "password","subject-client", "secret");
        oauth.scope("optional-scope2");
        AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret",  List.of("target-client1"), Collections.singletonMap(OAuth2Constants.REQUESTED_TOKEN_TYPE, OAuth2Constants.REFRESH_TOKEN_TYPE));
        assertAudiencesAndScopes(response, List.of("target-client1"), List.of("default-scope1", "optional-scope2"));
        org.junit.Assert.assertNotNull(response.getRefreshToken());

        oauth.client("requester-client", "secret");
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertAudiencesAndScopes(response, List.of("target-client1"), List.of("default-scope1", "optional-scope2"));

        oauth.client("requester-client", "secret");
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertAudiencesAndScopes(response, List.of("target-client1"), List.of("default-scope1", "optional-scope2"));
    }

    @Test
    @UncaughtServerErrorExpected
    public void testExchangeWithDynamicScopesEnabled() throws Exception {
        testingClient.enableFeature(Profile.Feature.DYNAMIC_SCOPES);
        testExchange();
        testingClient.disableFeature(Profile.Feature.DYNAMIC_SCOPES);
    }

    private void assertAudiences(AccessToken token, List<String> expectedAudiences) {
        MatcherAssert.assertThat("Incompatible audiences", token.getAudience() == null ? List.of() : List.of(token.getAudience()), containsInAnyOrder(expectedAudiences.toArray()));
        MatcherAssert.assertThat("Incompatible resource access", token.getResourceAccess().keySet(), containsInAnyOrder(expectedAudiences.toArray()));
    }

    private void assertScopes(AccessToken token, List<String> expectedScopes) {
        MatcherAssert.assertThat("Incompatible scopes", token.getScope().isEmpty() ? List.of() : List.of(token.getScope().split(" ")), containsInAnyOrder(expectedScopes.toArray()));
    }

    private void assertAudiencesAndScopes(AccessTokenResponse tokenExchangeResponse, List<String> expectedAudiences, List<String> expectedScopes) throws Exception {
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(tokenExchangeResponse.getAccessToken(), AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        if (expectedAudiences == null) {
            org.junit.Assert.assertNull("Expected token to not contain audience", token.getAudience());
        } else {
            assertAudiences(token, expectedAudiences);
        }
        assertScopes(token, expectedScopes);
    }

}
