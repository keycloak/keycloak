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

import java.io.IOException;
import java.util.List;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.encode.AccessTokenContext;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.oidc.TokenMetadataRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.UncaughtServerErrorExpected;
import org.keycloak.testsuite.client.policies.AbstractClientPoliciesTest;
import org.keycloak.testsuite.pages.ConsentPage;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.UserInfoResponse;

import org.hamcrest.MatcherAssert;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;

import static org.keycloak.testsuite.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * This class has been migrated to the new testsuite at:
 * {@link org.keycloak.tests.oauth.tokenexchange.StandardBaseTokenExchangeV2Test}
 *
 * <p>It is kept as abstract to prevent duplicate test execution.
 * New token exchange tests should be added to the new testsuite, not here.</p>
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @deprecated Migrated to new testsuite. Use {@link org.keycloak.tests.oauth.tokenexchange.StandardBaseTokenExchangeV2Test} instead.
 */
@Deprecated
public abstract class StandardTokenExchangeV2Test extends AbstractClientPoliciesTest {

    @Page
    protected ConsentPage consentPage;

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

    protected AccessTokenResponse resourceOwnerLogin(String username, String password, String clientId, String secret) throws Exception {
        return resourceOwnerLogin(username, password, clientId, secret, null);
    }

    private AccessTokenResponse resourceOwnerLogin(String username, String password, String clientId, String secret, String scope) throws Exception {
        oauth.realm(TEST);
        oauth.client(clientId, secret);
        oauth.scope(scope);
        oauth.openid(false);
        events.clear();
        AccessTokenResponse response = oauth.doPasswordGrantRequest(username, password);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(response.getAccessToken(), AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        events.expect(EventType.LOGIN)
                .client(clientId)
                .user(token.getSubject())
                .session(token.getSessionId())
                .detail(Details.USERNAME, username)
                .assertEvent();
        return response;
    }

    private String loginWithConsents(UserRepresentation user, String password, String clientId, String secret) throws Exception {
        oauth.client(clientId, secret).doLogin(user.getUsername(), password);
        consentPage.assertCurrent();
        consentPage.confirm();
        assertNotNull(oauth.parseLoginResponse().getCode());
        AccessTokenResponse response = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(response.getAccessToken(), AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        final EventRepresentation loginEvent = events.expectLogin()
                .client(clientId)
                .user(user.getId())
                .session(token.getSessionId())
                .detail(Details.USERNAME, user.getUsername())
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .assertEvent();
        final String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        events.expectCodeToToken(codeId, token.getSessionId())
                .client(clientId)
                .user(user.getId())
                .assertEvent();
        return response.getAccessToken();
    }

    protected AccessTokenResponse tokenExchange(String subjectToken, String clientId, String secret, List<String> audience, String requestedTokenType) {
        return oauth.tokenExchangeRequest(subjectToken).client(clientId, secret).audience(audience).requestedTokenType(requestedTokenType).send();
    }

    @Test
    @UncaughtServerErrorExpected
    public void testExchange() throws Exception {
        final UserRepresentation john = ApiUtil.findUserByUsername(adminClient.realm(TEST), "john");
        oauth.realm(TEST);
        String accessToken = resourceOwnerLogin("john", "password", "subject-client", "secret").getAccessToken();
        {
            AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret", null, null);
            assertEquals(OAuth2Constants.ACCESS_TOKEN_TYPE, response.getIssuedTokenType());
            String exchangedTokenString = response.getAccessToken();
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
            AccessToken exchangedToken = verifier.parse().getToken();
            assertEquals(getSessionIdFromToken(accessToken), exchangedToken.getSessionId());
            assertEquals("requester-client", exchangedToken.getIssuedFor());
            events.expect(EventType.TOKEN_EXCHANGE)
                    .client(exchangedToken.getIssuedFor())
                    .user(john.getId())
                    .session(exchangedToken.getSessionId())
                    .detail(Details.USERNAME, john.getUsername())
                    .assertEvent();
        }
        {
            //exchange not allowed due the invalid client is not in the subject-client audience
            AccessTokenResponse response = tokenExchange(accessToken, "invalid-requester-client", "secret", null, null);
            assertEquals(403, response.getStatusCode());
            events.expect(EventType.TOKEN_EXCHANGE_ERROR)
                    .client("invalid-requester-client")
                    .error(Errors.NOT_ALLOWED)
                    .user(john.getId())
                    .session(AssertEvents.isSessionId())
                    .detail(Details.REASON, "client is not within the token audience")
                    .assertEvent();
        }
    }

    @Test
    @UncaughtServerErrorExpected
    public void testExchangeDisabledOnClient() throws Exception {
        oauth.realm(TEST);
        String accessToken = resourceOwnerLogin("john", "password", "subject-client", "secret").getAccessToken();
        {
            AccessTokenResponse response = tokenExchange(accessToken, "disabled-requester-client", "secret", null, null);
            org.junit.Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
            org.junit.Assert.assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
            org.junit.Assert.assertEquals("Standard token exchange is not enabled for the requested client", response.getErrorDescription());
        }
    }

    private void isAccessTokenEnabled(String accessToken, String clientId, String secret) throws IOException {
        oauth.client(clientId, secret);
        TokenMetadataRepresentation rep = oauth.doIntrospectionAccessTokenRequest(accessToken).asTokenMetadata();
        assertTrue(rep.isActive());
        events.expect(EventType.INTROSPECT_TOKEN)
                .user(AssertEvents.isUUID())
                .session(AssertEvents.isSessionId())
                .client(clientId)
                .assertEvent();
    }

    private void isAccessTokenDisabled(String accessTokenString, String clientId, String secret) throws IOException {
        // Test introspection endpoint not possible
        oauth.client(clientId, secret);
        TokenMetadataRepresentation rep = oauth.doIntrospectionAccessTokenRequest(accessTokenString).asTokenMetadata();
        assertFalse(rep.isActive());
    }

    private void isTokenEnabled(AccessTokenResponse tokenResponse, String clientId, String secret) throws IOException {
        isAccessTokenEnabled(tokenResponse.getAccessToken(), clientId, secret);
        AccessTokenResponse tokenRefreshResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
        assertEquals(Response.Status.OK.getStatusCode(), tokenRefreshResponse.getStatusCode());
    }

    private void isTokenDisabled(AccessTokenResponse tokenResponse, String clientId, String secret) throws IOException {
        isAccessTokenDisabled(tokenResponse.getAccessToken(), clientId, secret);

        oauth.client(clientId, secret);
        AccessTokenResponse tokenRefreshResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), tokenRefreshResponse.getStatusCode());
    }

    private void assertAudiences(AccessToken token, List<String> expectedAudiences) {
        MatcherAssert.assertThat("Incompatible audiences", token.getAudience() == null ? List.of() : List.of(token.getAudience()), containsInAnyOrder(expectedAudiences.toArray()));
        MatcherAssert.assertThat("Incompatible resource access", token.getResourceAccess().keySet(), containsInAnyOrder(expectedAudiences.toArray()));
    }

    private void assertScopes(AccessToken token, List<String> expectedScopes) {
        MatcherAssert.assertThat("Incompatible scopes", token.getScope().isEmpty() ? List.of() : List.of(token.getScope().split(" ")), containsInAnyOrder(expectedScopes.toArray()));
    }

    private AccessToken assertAudiencesAndScopes(AccessTokenResponse tokenExchangeResponse, List<String> expectedAudiences, List<String> expectedScopes) throws Exception {
        assertEquals(Response.Status.OK.getStatusCode(), tokenExchangeResponse.getStatusCode());
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(tokenExchangeResponse.getAccessToken(), AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        if (expectedAudiences == null) {
            assertNull("Expected token to not contain audience", token.getAudience());
        } else {
            assertAudiences(token, expectedAudiences);
        }
        assertScopes(token, expectedScopes);
        return token;
    }

    private AccessToken assertAudiencesAndScopes(AccessTokenResponse tokenExchangeResponse, UserRepresentation user, List<String> expectedAudiences, List<String> expectedScopes) throws Exception {
        return assertAudiencesAndScopes(tokenExchangeResponse, user, expectedAudiences, expectedScopes, OAuth2Constants.ACCESS_TOKEN_TYPE, "subject-client");
    }

    private AccessToken assertAudiencesAndScopes(AccessTokenResponse tokenExchangeResponse, UserRepresentation user,
                                                 List<String> expectedAudiences, List<String> expectedScopes, String expectedTokenType, String expectedSubjectTokenClientId) throws Exception {
        AccessToken token = assertAudiencesAndScopes(tokenExchangeResponse, expectedAudiences, expectedScopes);
        events.expect(EventType.TOKEN_EXCHANGE)
                .client(token.getIssuedFor())
                .user(user.getId())
                .session(token.getSessionId())
                .detail(Details.AUDIENCE, CollectionUtil.join(expectedAudiences, " "))
                .detail(Details.SCOPE, CollectionUtil.join(expectedScopes, " "))
                .detail(Details.USERNAME, user.getUsername())
                .detail(Details.REQUESTED_TOKEN_TYPE, expectedTokenType)
                .detail(Details.SUBJECT_TOKEN_CLIENT_ID, expectedSubjectTokenClientId)
                .assertEvent();
        return token;
    }

    private void createClientScopeForRole(RealmResource realm, String clientId, String clientRoleName, String clientScopeName) {
        final ClientResource client = ApiUtil.findClientByClientId(realm, clientId);
        createClientScopeForRole(realm, client, clientRoleName, clientScopeName);
    }

    private void createClientScopeForRole(RealmResource realm, ClientResource client, String clientRoleName, String clientScopeName) {
        final String clientUUID = client.toRepresentation().getId();
        final RoleRepresentation clientRole = ApiUtil.findClientRoleByName(client, clientRoleName).toRepresentation();

        final ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName(clientScopeName);
        clientScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        final String clientScopeId = ApiUtil.getCreatedId(realm.clientScopes().create(clientScope));
        getCleanup().addClientScopeId(clientScopeId);
        realm.clientScopes().get(clientScopeId).getScopeMappings().clientLevel(clientUUID).add(List.of(clientRole));
    }

    private void assertIntrospectSuccess(String token, String clientId, String clientSecret, String userId) throws IOException {
        TokenMetadataRepresentation rep = oauth.client(clientId, clientSecret).introspectionRequest(token).tokenTypeHint("access_token").send().asTokenMetadata();
        assertTrue(rep.isActive());
        assertEquals(userId, rep.getSubject());
    }

    private void assertIntrospectError(String token, String clientId, String clientSecret) throws IOException {
        TokenMetadataRepresentation rep = oauth.client(clientId, clientSecret).introspectionRequest(token).tokenTypeHint("access_token").send().asTokenMetadata();
        assertFalse(rep.isActive());
    }

    private void assertUserInfoSuccess(String token, String clientId, String clientSecret, String userId) {
        UserInfoResponse userInfoResp = oauth.client(clientId, clientSecret).userInfoRequest(token).send();
        assertEquals(Response.Status.OK.getStatusCode(), userInfoResp.getStatusCode());
        assertEquals(userId, userInfoResp.getUserInfo().getSub());
    }

    private void assertUserInfoError(String token, String clientId, String clientSecret, String error, String errorDesciption) {
        UserInfoResponse userInfoResp = oauth.client(clientId, clientSecret).userInfoRequest(token).send();
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), userInfoResp.getStatusCode());
        assertEquals(String.format("Bearer realm=\"%s\", error=\"%s\", error_description=\"%s\"", TEST, error, errorDesciption),
                userInfoResp.getHeader(HttpHeaders.WWW_AUTHENTICATE));
    }

    private void assertAccessTokenContext(String jti, AccessTokenContext.SessionType sessionType,
                                          AccessTokenContext.TokenType tokenType, String grantType) {
        AccessTokenContext ctx = testingClient.testing(TEST).getTokenContext(jti);
        assertEquals(sessionType, ctx.getSessionType());
        assertEquals(tokenType, ctx.getTokenType());
        assertEquals(grantType, ctx.getGrantType());
    }
}
