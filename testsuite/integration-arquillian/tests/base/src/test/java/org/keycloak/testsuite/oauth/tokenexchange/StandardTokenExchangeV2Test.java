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

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.hamcrest.MatcherAssert;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.Profile;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.encode.AccessTokenContext;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.oidc.TokenMetadataRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.condition.ClientScopesConditionFactory;
import org.keycloak.services.clientpolicy.condition.GrantTypeConditionFactory;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.arquillian.annotation.UncaughtServerErrorExpected;
import org.keycloak.testsuite.broker.util.SimpleHttpDefault;
import org.keycloak.testsuite.client.policies.AbstractClientPoliciesTest;
import org.keycloak.testsuite.pages.ConsentPage;
import org.keycloak.testsuite.services.clientpolicy.executor.TestRaiseExceptionExecutorFactory;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.updaters.RoleScopeUpdater;
import org.keycloak.testsuite.updaters.UserAttributeUpdater;
import org.keycloak.testsuite.util.ClientPoliciesUtil;
import org.keycloak.testsuite.util.ServerURLs;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.UserInfoResponse;
import org.keycloak.testsuite.util.oauth.TokenExchangeRequest;
import org.keycloak.testsuite.utils.tls.TLSUtils;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientScopesConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createGrantTypeConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createTestRaiseExeptionExecutorConfig;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@EnableFeature(value = Profile.Feature.TOKEN_EXCHANGE_STANDARD_V2, skipRestart = true)
public class StandardTokenExchangeV2Test extends AbstractClientPoliciesTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

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

    protected String resourceOwnerLogin(String username, String password, String clientId, String secret) throws Exception {
        return resourceOwnerLogin(username, password, clientId, secret, null);
    }

    private String resourceOwnerLogin(String username, String password, String clientId, String secret, String scope) throws Exception {
        oauth.realm(TEST);
        oauth.client(clientId, secret);
        oauth.scope(scope);
        oauth.openid(false);
        AccessTokenResponse response = oauth.doPasswordGrantRequest(username, password);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(response.getAccessToken(), AccessToken.class);
        accessTokenVerifier.parse();
        return response.getAccessToken();
    }

    private String loginWithConsents(String username, String password, String clientId, String secret) throws Exception {
        oauth.client(clientId, secret).doLogin(username, password);
        consentPage.assertCurrent();
        consentPage.confirm();
        assertNotNull(oauth.parseLoginResponse().getCode());
        AccessTokenResponse response = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(response.getAccessToken(), AccessToken.class);
        accessTokenVerifier.parse();
        return response.getAccessToken();
    }

    protected AccessTokenResponse tokenExchange(String subjectToken, String clientId, String secret, List<String> audience, Map<String, String> additionalParams) {
        return oauth.tokenExchangeRequest(subjectToken).client(clientId, secret).audience(audience).additionalParams(additionalParams).send();
    }

    @Test
    @UncaughtServerErrorExpected
    public void testSubjectTokenType() throws Exception {
        oauth.realm(TEST);
        String accessToken = resourceOwnerLogin("john", "password", "subject-client", "secret");

        TokenExchangeRequest request = oauth.tokenExchangeRequest(accessToken, OAuth2Constants.ACCESS_TOKEN_TYPE);
        AccessTokenResponse response = request.send();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());

        request = oauth.tokenExchangeRequest(accessToken, OAuth2Constants.REFRESH_TOKEN_TYPE);
        response = request.send();
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());

        request = oauth.tokenExchangeRequest(accessToken, OAuth2Constants.ID_TOKEN_TYPE);
        response = request.send();
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());

        request = oauth.tokenExchangeRequest(accessToken, OAuth2Constants.SAML2_TOKEN_TYPE);
        response = request.send();
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());

        request = oauth.tokenExchangeRequest(accessToken, OAuth2Constants.JWT_TOKEN_TYPE);
        response = request.send();
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());

        request = oauth.tokenExchangeRequest(accessToken, "WRONG_TOKEN_TYPE");
        response = request.send();
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
    }

    @Test
    @UncaughtServerErrorExpected
    public void testRequestedTokenType() throws Exception {
        oauth.realm(TEST);
        String accessToken = resourceOwnerLogin("john", "password", "subject-client", "secret");

        AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret", null, Map.of(OAuth2Constants.REQUESTED_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
        assertNotNull(response.getAccessToken());
        assertEquals(TokenUtil.TOKEN_TYPE_BEARER, response.getTokenType());
        assertEquals(OAuth2Constants.ACCESS_TOKEN_TYPE, response.getIssuedTokenType());

        response = tokenExchange(accessToken, "requester-client", "secret", null, Map.of(OAuth2Constants.REQUESTED_TOKEN_TYPE, OAuth2Constants.REFRESH_TOKEN_TYPE));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        assertEquals("requested_token_type unsupported", response.getErrorDescription());

        try (ClientAttributeUpdater clientUpdater = ClientAttributeUpdater.forClient(adminClient, TEST, "requester-client")
                .setAttribute(OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_REFRESH_ENABLED, Boolean.TRUE.toString())
                .update()) {
            response = tokenExchange(accessToken, "requester-client", "secret", null, Map.of(OAuth2Constants.REQUESTED_TOKEN_TYPE, OAuth2Constants.REFRESH_TOKEN_TYPE));
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
            assertNotNull(response.getAccessToken());
            assertEquals(TokenUtil.TOKEN_TYPE_BEARER, response.getTokenType());
            assertEquals(OAuth2Constants.REFRESH_TOKEN_TYPE, response.getIssuedTokenType());
        }

        response = tokenExchange(accessToken, "requester-client", "secret", null, Map.of(OAuth2Constants.REQUESTED_TOKEN_TYPE, OAuth2Constants.ID_TOKEN_TYPE));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
        assertNotNull(response.getAccessToken());
        assertEquals(TokenUtil.TOKEN_TYPE_NA, response.getTokenType());
        assertEquals(OAuth2Constants.ID_TOKEN_TYPE, response.getIssuedTokenType());

        response = tokenExchange(accessToken, "requester-client", "secret", null, Map.of(OAuth2Constants.REQUESTED_TOKEN_TYPE, OAuth2Constants.JWT_TOKEN_TYPE));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        assertEquals("requested_token_type unsupported", response.getErrorDescription());

        response = tokenExchange(accessToken, "requester-client", "secret", null, Map.of(OAuth2Constants.REQUESTED_TOKEN_TYPE, OAuth2Constants.SAML2_TOKEN_TYPE));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        assertEquals("requested_token_type unsupported", response.getErrorDescription());

        response = tokenExchange(accessToken, "requester-client", "secret", null, Map.of(OAuth2Constants.REQUESTED_TOKEN_TYPE, "WRONG_TOKEN_TYPE"));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        assertEquals("requested_token_type unsupported", response.getErrorDescription());
    }

    @Test
    @UncaughtServerErrorExpected
    public void testExchange() throws Exception {
        oauth.realm(TEST);
        String accessToken = resourceOwnerLogin("john", "password", "subject-client", "secret");
        {
            AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret", null, null);
            assertEquals(OAuth2Constants.ACCESS_TOKEN_TYPE, response.getIssuedTokenType());
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
    public void testTransientSessionForRequester() throws Exception {
        final RealmResource realm = adminClient.realm(TEST);
        final UserRepresentation john = ApiUtil.findUserByUsername(realm, "john");

        oauth.realm(TEST);
        final String accessToken = resourceOwnerLogin("john", "password", "subject-client", "secret");

        oauth.scope(OAuth2Constants.SCOPE_OPENID); // add openid scope for the user-info request
        AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret", null, null);
        assertEquals(OAuth2Constants.ACCESS_TOKEN_TYPE, response.getIssuedTokenType());
        final String exchangedTokenString = response.getAccessToken();
        final AccessToken exchangedToken = TokenVerifier.create(exchangedTokenString, AccessToken.class).parse().getToken();
        assertEquals(getSessionIdFromToken(accessToken), exchangedToken.getSessionId());
        assertEquals("requester-client", exchangedToken.getIssuedFor());
        assertAccessTokenContext(exchangedToken.getId(), AccessTokenContext.SessionType.TRANSIENT,
                AccessTokenContext.TokenType.REGULAR, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE);

        // assert instrospection and user-info works
        assertIntrospectSuccess(exchangedTokenString, "requester-client", "secret", john.getId());
        assertUserInfoSuccess(exchangedTokenString, "requester-client", "secret", john.getId());

        // assert instrospection and user-info works in 10s
        setTimeOffset(10);
        assertIntrospectSuccess(exchangedTokenString, "requester-client", "secret", john.getId());
        assertUserInfoSuccess(exchangedTokenString, "requester-client", "secret", john.getId());

        // assert instrospection and user-info fails with session deleted
        realm.deleteSession(exchangedToken.getSessionId(), false);
        assertIntrospectError(exchangedTokenString, "requester-client", "secret");
        assertUserInfoError(exchangedTokenString, "requester-client", "secret", "invalid_token", "Session not found");
    }

    @Test
    public void testTransientSessionWithAdminApi() throws Exception {
        final RealmResource realm = adminClient.realm(TEST);

        // create a client-scope to the requester-client that adds realm-management view-role access
        final ClientResource client = ApiUtil.findClientByClientId(realm, Constants.REALM_MANAGEMENT_CLIENT_ID);
        createClientScopeForRole(realm, client, AdminRoles.VIEW_REALM, "realm-management-view-scope");

        // update the user and the requester-client to include the view-realm permission
        try (RoleScopeUpdater roleScopeUpdater = UserAttributeUpdater.forUserByUsername(realm, "john")
                .clientRoleScope(client.toRepresentation().getId())
                .add(ApiUtil.findClientRoleByName(client, AdminRoles.VIEW_REALM).toRepresentation())
                .update();
            ClientAttributeUpdater clientUpdater = ClientAttributeUpdater.forClient(adminClient, TEST, "requester-client")
                .addOptionalClientScope("realm-management-view-scope")
                .update()) {

            oauth.realm(TEST);
            final String accessToken = resourceOwnerLogin("john", "password", "subject-client", "secret");

            // token exchange with the realm-management-view optional scope
            oauth.scope("realm-management-view-scope");
            final AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret", List.of(Constants.REALM_MANAGEMENT_CLIENT_ID), null);
            assertAudiencesAndScopes(response, List.of(Constants.REALM_MANAGEMENT_CLIENT_ID), List.of("realm-management-view-scope"));
            final AccessToken exchangedToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class).parse().getToken();
            assertAccessTokenContext(exchangedToken.getId(), AccessTokenContext.SessionType.TRANSIENT,
                AccessTokenContext.TokenType.REGULAR, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE);

            try (Keycloak keycloak = Keycloak.getInstance(ServerURLs.getAuthServerContextRoot() + "/auth",
                    TEST, Constants.ADMIN_CLI_CLIENT_ID, response.getAccessToken(), TLSUtils.initializeTLS())) {
                assertEquals(TEST, keycloak.realm(TEST).toRepresentation().getRealm());
                setTimeOffset(10);
                assertEquals(TEST, keycloak.realm(TEST).toRepresentation().getRealm());
                realm.deleteSession(exchangedToken.getSessionId(), false);
                assertThrows(NotAuthorizedException.class, () -> keycloak.realm(TEST).toRepresentation().getRealm());
            }
        }
    }

    @Test
    public void testTransientSessionWithAccountApi() throws Exception {
        final RealmResource realm = adminClient.realm(TEST);

        // create a client-scope for the requester-client that adds account view-profile access
        createClientScopeForRole(realm, Constants.ACCOUNT_MANAGEMENT_CLIENT_ID, AccountRoles.VIEW_PROFILE, "account-view-profile-scope");

        // update the requester-client to include the view-profile permission
        try (ClientAttributeUpdater clientUpdater = ClientAttributeUpdater.forClient(adminClient, TEST, "requester-client")
                .addOptionalClientScope("account-view-profile-scope")
                .update()) {

            oauth.realm(TEST);
            final String accessToken = resourceOwnerLogin("john", "password", "subject-client", "secret");

            // token exchange with the view-profile optional scope
            oauth.scope("account-view-profile-scope");
            final AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret", List.of(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID), null);
            assertAudiencesAndScopes(response, List.of(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID), List.of("account-view-profile-scope"));
            final AccessToken exchangedToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class).parse().getToken();
            assertAccessTokenContext(exchangedToken.getId(), AccessTokenContext.SessionType.TRANSIENT,
                AccessTokenContext.TokenType.REGULAR, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE);

            final String accountUrl = ServerURLs.getAuthServerContextRoot() + "/auth/realms/test/account";
            assertEquals("john", SimpleHttpDefault.doGet(accountUrl, oauth.httpClient().get())
                    .auth(response.getAccessToken()).asJson(UserRepresentation.class).getUsername());
            setTimeOffset(10);
            assertEquals("john", SimpleHttpDefault.doGet(accountUrl, oauth.httpClient().get())
                    .auth(response.getAccessToken()).asJson(UserRepresentation.class).getUsername());
            realm.deleteSession(exchangedToken.getSessionId(), false);
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), SimpleHttpDefault.doGet(accountUrl, oauth.httpClient().get())
                    .auth(response.getAccessToken()).acceptJson().asResponse().getStatus());
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
    public void testExchangeForIdToken() throws Exception {
        oauth.realm(TEST);
        String accessToken = resourceOwnerLogin("john", "password","subject-client", "secret");

        // Exchange request with "scope=oidc" . ID Token should be issued in addition to access-token
        oauth.openid(true);
        oauth.scope(OAuth2Constants.SCOPE_OPENID);
        AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret", null, Map.of(OAuth2Constants.REQUESTED_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE));
        assertEquals(OAuth2Constants.ACCESS_TOKEN_TYPE, response.getIssuedTokenType());
        AccessToken exchangedToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class)
                .parse().getToken();
        assertEquals(TokenUtil.TOKEN_TYPE_BEARER, exchangedToken.getType());

        assertNotNull("ID Token is null, but was expected to be present", response.getIdToken());
        IDToken exchangedIdToken = TokenVerifier.create(response.getIdToken(), IDToken.class)
                .parse().getToken();
        assertEquals(TokenUtil.TOKEN_TYPE_ID, exchangedIdToken.getType());
        assertEquals(getSessionIdFromToken(accessToken), exchangedIdToken.getSessionId());
        assertEquals("requester-client", exchangedIdToken.getIssuedFor());

        // Exchange request without "scope=oidc" . Only access-token should be issued, but not ID Token
        oauth.openid(false);
        oauth.scope(null);
        response = tokenExchange(accessToken, "requester-client", "secret", null, Map.of(OAuth2Constants.REQUESTED_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE));
        assertEquals(OAuth2Constants.ACCESS_TOKEN_TYPE, response.getIssuedTokenType());
        assertNotNull(response.getAccessToken());
        assertNull("ID Token was present, but should not be present", response.getIdToken());

        // Exchange request requesting id-token. ID Token should be issued inside "access_token" parameter (as per token-exchange specification https://datatracker.ietf.org/doc/html/rfc8693#name-successful-response - parameter "access_token")
        response = tokenExchange(accessToken, "requester-client", "secret", null, Map.of(OAuth2Constants.REQUESTED_TOKEN_TYPE, OAuth2Constants.ID_TOKEN_TYPE));
        assertEquals(OAuth2Constants.ID_TOKEN_TYPE, response.getIssuedTokenType());
        assertEquals(TokenUtil.TOKEN_TYPE_NA, response.getTokenType());
        assertNotNull(response.getAccessToken());
        assertNull("ID Token was present, but should not be present", response.getIdToken());

        exchangedIdToken = TokenVerifier.create(response.getAccessToken(), IDToken.class)
                .parse().getToken();
        assertEquals(TokenUtil.TOKEN_TYPE_ID, exchangedIdToken.getType());
        assertEquals(getSessionIdFromToken(accessToken), exchangedIdToken.getSessionId());
        assertEquals("requester-client", exchangedIdToken.getIssuedFor());
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
        assertNull(token.getSessionId());
        response = tokenExchange(accessToken, "requester-client", "secret", null, null);
        assertAudiencesAndScopes(response, List.of("target-client1"), List.of("default-scope1"));
        assertEquals(OAuth2Constants.ACCESS_TOKEN_TYPE, response.getIssuedTokenType());
        String exchangedTokenString = response.getAccessToken();
        TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
        AccessToken exchangedToken = verifier.parse().getToken();
        assertNull(exchangedToken.getSessionId());
        assertEquals("requester-client", exchangedToken.getIssuedFor());

        try (ClientAttributeUpdater clientUpdater = ClientAttributeUpdater.forClient(adminClient, TEST, "requester-client")
                .setAttribute(OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_REFRESH_ENABLED, Boolean.TRUE.toString())
                .update()) {
            response = tokenExchange(accessToken, "requester-client", "secret", null,
                    Map.of(OAuth2Constants.REQUESTED_TOKEN_TYPE, OAuth2Constants.REFRESH_TOKEN_TYPE));
            assertAudiencesAndScopes(response, List.of("target-client1"), List.of("default-scope1"));
            assertEquals(OAuth2Constants.REFRESH_TOKEN_TYPE, response.getIssuedTokenType());
            assertNotNull(response.getAccessToken());
            assertNotNull(response.getRefreshToken());

            oauth.client("requester-client", "secret");
            response = oauth.doRefreshTokenRequest(response.getRefreshToken());
            assertAudiencesAndScopes(response, List.of("target-client1"), List.of("default-scope1"));

            oauth.client("requester-client", "secret");
            response = oauth.doRefreshTokenRequest(response.getRefreshToken());
            assertAudiencesAndScopes(response, List.of("target-client1"), List.of("default-scope1"));
        }
    }

    @Test
    @UncaughtServerErrorExpected
    public void testExchangeNoRefreshToken() throws Exception {


        String accessToken = resourceOwnerLogin("john", "password","subject-client", "secret");
        {
            AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret", null, null);
            assertEquals(OAuth2Constants.ACCESS_TOKEN_TYPE, response.getIssuedTokenType());
            String exchangedTokenString = response.getAccessToken();
            String refreshTokenString = response.getRefreshToken();
            assertNotNull(exchangedTokenString);
            assertNull(refreshTokenString);
        }

        try (ClientAttributeUpdater clienUpdater = ClientAttributeUpdater.forClient(adminClient, TEST, "requester-client")
                .setAttribute(OIDCConfigAttributes.USE_REFRESH_TOKEN, Boolean.FALSE.toString())
                .setAttribute(OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_REFRESH_ENABLED, Boolean.TRUE.toString())
                .update()) {
            AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret", null,
                    Map.of(OAuth2Constants.REQUESTED_TOKEN_TYPE, OAuth2Constants.REFRESH_TOKEN_TYPE));
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
            assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
            assertEquals("requested_token_type unsupported", response.getErrorDescription());
        }
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

        try (ClientAttributeUpdater clientUpdater = ClientAttributeUpdater.forClient(adminClient, TEST, "subject-client")
                .setConsentRequired(Boolean.TRUE)
                .update()) {
            AccessTokenResponse response = tokenExchange(accessToken, "subject-client", "secret", null, null);
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
            assertEquals(OAuthErrorException.INVALID_SCOPE, response.getError());
            assertEquals("Missing consents for Token Exchange in client subject-client", response.getErrorDescription());

            response = tokenExchange(accessToken, "subject-client", "secret", List.of("subject-client"), null);
            assertEquals(OAuthErrorException.INVALID_SCOPE, response.getError());
            assertEquals("Missing consents for Token Exchange in client subject-client", response.getErrorDescription());
        }
    }

    @Test
    public void testExchangeWithPublicClient() throws Exception {
        String accessToken = resourceOwnerLogin("john", "password","subject-client", "secret");
        AccessTokenResponse response = tokenExchange(accessToken, "requester-client-public", null,  null, null);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_CLIENT, response.getError());
        assertEquals("Public client is not allowed to exchange token", response.getErrorDescription());
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
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_CLIENT, response.getError());
        assertEquals("Audience not found", response.getErrorDescription());
        // The "target-client3" is valid client, but audience unavailable to the user. Request not allowed
        response = tokenExchange(accessToken, "requester-client", "secret",  List.of("target-client1", "target-client3"), null);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        assertEquals("Requested audience not available: target-client3", response.getErrorDescription());
    }

    @Test
    public void testScopeNotAllowed() throws Exception {
        String accessToken = resourceOwnerLogin("john", "password", "subject-client", "secret");

        //scope not allowed
        oauth.scope("optional-scope3");
        AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret",  List.of("target-client1", "target-client3"), null);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_SCOPE, response.getError());
        assertEquals("Invalid scopes: optional-scope3", response.getErrorDescription());

        //scope that doesn't exist
        oauth.scope("bad-scope");
        response = tokenExchange(accessToken, "requester-client", "secret",  List.of("target-client1", "target-client3"), null);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_SCOPE, response.getError());
        assertEquals("Invalid scopes: bad-scope", response.getErrorDescription());
    }

    @Test
    public void testScopeFilter() throws Exception {
        String accessToken = resourceOwnerLogin("john", "password", "subject-client", "secret");
        AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret",  List.of("target-client2"), null);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        assertEquals("Requested audience not available: target-client2", response.getErrorDescription());

        oauth.scope("optional-scope2");
        response = tokenExchange(accessToken, "requester-client", "secret",  List.of("target-client1"), null);
        assertAudiencesAndScopes(response, List.of("target-client1"), List.of("default-scope1"));

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
        try (ClientAttributeUpdater clientUpdater = ClientAttributeUpdater.forClient(adminClient, TEST, "requester-client")
                .setAttribute(OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_REFRESH_ENABLED, Boolean.TRUE.toString())
                .update()) {
            String accessToken = resourceOwnerLogin("mike", "password", "subject-client", "secret");
            oauth.scope("optional-scope2");
            AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret", List.of("target-client1"), Collections.singletonMap(OAuth2Constants.REQUESTED_TOKEN_TYPE, OAuth2Constants.REFRESH_TOKEN_TYPE));
            assertAudiencesAndScopes(response, List.of("target-client1"), List.of("default-scope1", "optional-scope2"));
            assertNotNull(response.getRefreshToken());

            oauth.client("requester-client", "secret");
            response = oauth.doRefreshTokenRequest(response.getRefreshToken());
            assertAudiencesAndScopes(response, List.of("target-client1"), List.of("default-scope1", "optional-scope2"));

            oauth.client("requester-client", "secret");
            response = oauth.doRefreshTokenRequest(response.getRefreshToken());
            assertAudiencesAndScopes(response, List.of("target-client1"), List.of("default-scope1", "optional-scope2"));
        }
    }

    @Test
    @UncaughtServerErrorExpected
    public void testExchangeWithDynamicScopesEnabled() throws Exception {
        testingClient.enableFeature(Profile.Feature.DYNAMIC_SCOPES);
        testExchange();
        testingClient.disableFeature(Profile.Feature.DYNAMIC_SCOPES);
    }
    
    @Test
    @UncaughtServerErrorExpected
    public void testExchangeDisabledOnClient() throws Exception {
        oauth.realm(TEST);
        String accessToken = resourceOwnerLogin("john", "password", "subject-client", "secret");
        {
            AccessTokenResponse response = tokenExchange(accessToken, "disabled-requester-client", "secret", null, null);
            org.junit.Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
            org.junit.Assert.assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
            org.junit.Assert.assertEquals("Standard token exchange is not enabled for the requested client", response.getErrorDescription());
        }
    }

    @Test
    public void testConsents() throws Exception {
        try (ClientAttributeUpdater clientUpdater = ClientAttributeUpdater.forClient(adminClient, TEST, "requester-client")
                .setConsentRequired(Boolean.TRUE)
                .update()) {
            // initial TE without any consent should fail
            String accessToken = resourceOwnerLogin("mike", "password", "subject-client", "secret");
            AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret",  null, null);
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
            assertEquals(OAuthErrorException.INVALID_SCOPE, response.getError());
            assertEquals("Missing consents for Token Exchange in client requester-client", response.getErrorDescription());

            // logout
            UserResource mike = ApiUtil.findUserByUsernameId(adminClient.realm(TEST), "mike");
            mike.logout();

            // perform a login and allow consent for default scopes, TE should work now
            accessToken = loginWithConsents("mike", "password", "requester-client", "secret");
            response = tokenExchange(accessToken, "requester-client", "secret",  null, null);
            assertAudiencesAndScopes(response,  List.of("target-client1"), List.of("default-scope1"));

            // request TE with optional-scope2 whose consent is missing, should fail
            oauth.scope("optional-scope2");
            response = tokenExchange(accessToken, "requester-client", "secret",  null, null);
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
            assertEquals(OAuthErrorException.INVALID_SCOPE, response.getError());
            assertEquals("Missing consents for Token Exchange in client requester-client", response.getErrorDescription());

            // logout
            mike.logout();

            // consent the additional scope, TE should work now
            accessToken = loginWithConsents("mike", "password", "requester-client", "secret");
            response = tokenExchange(accessToken, "requester-client", "secret",  null, null);
            assertAudiencesAndScopes(response,  List.of("target-client1"), List.of("default-scope1", "optional-scope2"));
        }
    }

    @Test
    public void testOfflineAccessNotAllowed() throws Exception {
        try (ClientAttributeUpdater clientUpdater = ClientAttributeUpdater.forClient(adminClient, TEST, "requester-client")
                .setAttribute(OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_REFRESH_ENABLED, Boolean.TRUE.toString())
                .update()) {
            String accessToken = resourceOwnerLogin("mike", "password", "subject-client", "secret");
            String sessionId = TokenVerifier.create(accessToken, AccessToken.class).parse().getToken().getSessionId();
            Assert.assertEquals(testingClient.testing(TEST).getClientSessionsCountInUserSession(TEST, sessionId), Integer.valueOf(1));

            oauth.scope("offline_access");
            AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret", List.of("target-client1"), Collections.singletonMap(OAuth2Constants.REQUESTED_TOKEN_TYPE, OAuth2Constants.REFRESH_TOKEN_TYPE));
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
            assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
            assertEquals("Scope offline_access not allowed for token exchange", response.getErrorDescription());

            // Check that client session was not created
            Assert.assertEquals(testingClient.testing(TEST).getClientSessionsCountInUserSession(TEST, sessionId), Integer.valueOf(1));
        }
    }

    // Issue 37116
    @Test
    public void testOfflineAccessLoginWithRegularTokenExchange() throws Exception {
        try (ClientAttributeUpdater clientUpdater1 = ClientAttributeUpdater.forClient(adminClient, TEST, "requester-client")
                .setAttribute(OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_REFRESH_ENABLED, Boolean.TRUE.toString())
                .update();
             ClientAttributeUpdater clientUpdater2 = ClientAttributeUpdater.forClient(adminClient, TEST, "subject-client")
                     .setOptionalClientScopes(List.of(OAuth2Constants.OFFLINE_ACCESS))
                     .update();
        ) {
            // Login with "scope=offline_access" . Will create offline user-session
            String accessToken = resourceOwnerLogin("mike", "password", "subject-client", "secret", OAuth2Constants.OFFLINE_ACCESS);
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(accessToken, AccessToken.class);
            AccessToken originalToken = verifier.parse().getToken();

            AccessTokenContext ctx = getTestingClient().testing().getTokenContext(originalToken.getId());
            assertEquals(ctx.getSessionType(), AccessTokenContext.SessionType.OFFLINE);

            // Token-exchange without "scope=offline_access". It is allowed and will create new "online" user session (as previous session was offline)
            oauth.scope(null);
            AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret", List.of("target-client1"), Collections.singletonMap(OAuth2Constants.REQUESTED_TOKEN_TYPE, OAuth2Constants.REFRESH_TOKEN_TYPE));
            verifier = TokenVerifier.create(response.getAccessToken(), AccessToken.class);
            AccessToken exchangedToken = verifier.parse().getToken();
            assertNotEquals(originalToken.getSessionId(), exchangedToken.getSessionId());

            ctx = getTestingClient().testing().getTokenContext(exchangedToken.getId());
            assertEquals(ctx.getSessionType(), AccessTokenContext.SessionType.ONLINE);

            // Refresh with the exchanged token - should be successful
            oauth.client("requester-client", "secret");
            response = oauth.doRefreshTokenRequest(response.getRefreshToken());
            assertAudiencesAndScopes(response, List.of("target-client1"), List.of("default-scope1"));
            assertEquals(getSessionIdFromToken(response.getAccessToken()), exchangedToken.getSessionId());
        }
    }

    @Test
    public void testOfflineAccessNotAllowedAfterOfflineAccessLogin() throws Exception {
        try (ClientAttributeUpdater clientUpdater1 = ClientAttributeUpdater.forClient(adminClient, TEST, "requester-client")
                .setAttribute(OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_REFRESH_ENABLED, Boolean.TRUE.toString())
                .update();
             ClientAttributeUpdater clientUpdater2 = ClientAttributeUpdater.forClient(adminClient, TEST, "subject-client")
                     .setOptionalClientScopes(List.of(OAuth2Constants.OFFLINE_ACCESS))
                     .update();
        ) {
            // Login with "scope=offline_access" . Will create offline user-session
            String accessToken = resourceOwnerLogin("mike", "password", "subject-client", "secret", OAuth2Constants.OFFLINE_ACCESS);
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(accessToken, AccessToken.class);
            AccessToken originalToken = verifier.parse().getToken();

            // Doublecheck count of sessions
            String subjectClientUuid = ApiUtil.findClientByClientId(adminClient.realm(TEST), "subject-client").toRepresentation().getId();
            String requesterClientUuid = ApiUtil.findClientByClientId(adminClient.realm(TEST), "requester-client").toRepresentation().getId();
            UserResource user = ApiUtil.findUserByUsernameId(adminClient.realm(TEST), "mike");
            Assert.assertEquals(user.getUserSessions().size(), 0);
            Assert.assertEquals(user.getOfflineSessions(subjectClientUuid).size(), 1);
            Assert.assertEquals(user.getOfflineSessions(requesterClientUuid).size(), 0);

            // Token exchange with scope=offline-access should not be allowed
            oauth.scope("offline_access");
            AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret", List.of("target-client1"), Collections.singletonMap(OAuth2Constants.REQUESTED_TOKEN_TYPE, OAuth2Constants.REFRESH_TOKEN_TYPE));
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());

            // Make sure not new user sessions persisted
            Assert.assertEquals(user.getUserSessions().size(), 0);
            Assert.assertEquals(user.getOfflineSessions(subjectClientUuid).size(), 1);
            Assert.assertEquals(user.getOfflineSessions(requesterClientUuid).size(), 0);
        }
    }

    @Test
    public void testClientPolicies() throws Exception {

        String json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Profilo")
                        .addExecutor(TestRaiseExceptionExecutorFactory.PROVIDER_ID,
                                createTestRaiseExeptionExecutorConfig(List.of(ClientPolicyEvent.TOKEN_EXCHANGE_REQUEST)))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policy with condition on client scope optional-scope2
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Client Scope Policy", Boolean.TRUE)
                        .addCondition(ClientScopesConditionFactory.PROVIDER_ID,
                                createClientScopesConditionConfig(ClientScopesConditionFactory.ANY, List.of("optional-scope2")))
                        .addCondition(GrantTypeConditionFactory.PROVIDER_ID,
                                createGrantTypeConditionConfig(List.of(OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        String accessToken = resourceOwnerLogin("john", "password", "subject-client", "secret");

        AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret", List.of("target-client1"), null);
        assertAudiencesAndScopes(response, List.of("target-client1"), List.of("default-scope1"));

        //block token exchange request if optional-scope2 is requested
        oauth.scope("optional-scope2");
        response  = tokenExchange(accessToken, "requester-client", "secret",  List.of("target-client2"), null);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(ClientPolicyEvent.TOKEN_EXCHANGE_REQUEST.toString(), response.getError());
        assertEquals("Exception thrown intentionally", response.getErrorDescription());
    }

    private void assertAudiences(AccessToken token, List<String> expectedAudiences) {
        MatcherAssert.assertThat("Incompatible audiences", token.getAudience() == null ? List.of() : List.of(token.getAudience()), containsInAnyOrder(expectedAudiences.toArray()));
        MatcherAssert.assertThat("Incompatible resource access", token.getResourceAccess().keySet(), containsInAnyOrder(expectedAudiences.toArray()));
    }

    private void assertScopes(AccessToken token, List<String> expectedScopes) {
        MatcherAssert.assertThat("Incompatible scopes", token.getScope().isEmpty() ? List.of() : List.of(token.getScope().split(" ")), containsInAnyOrder(expectedScopes.toArray()));
    }

    private void assertAudiencesAndScopes(AccessTokenResponse tokenExchangeResponse, List<String> expectedAudiences, List<String> expectedScopes) throws Exception {
        assertEquals(Response.Status.OK.getStatusCode(), tokenExchangeResponse.getStatusCode());
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(tokenExchangeResponse.getAccessToken(), AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        if (expectedAudiences == null) {
            assertNull("Expected token to not contain audience", token.getAudience());
        } else {
            assertAudiences(token, expectedAudiences);
        }
        assertScopes(token, expectedScopes);
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
        String tokenResponse = oauth.client(clientId, clientSecret).introspectionRequest(token).tokenTypeHint("access_token").send();
        TokenMetadataRepresentation rep = JsonSerialization.readValue(tokenResponse, TokenMetadataRepresentation.class);
        assertTrue(rep.isActive());
        assertEquals(userId, rep.getSubject());
    }

    private void assertIntrospectError(String token, String clientId, String clientSecret) throws IOException {
        String tokenResponse = oauth.client(clientId, clientSecret).introspectionRequest(token).tokenTypeHint("access_token").send();
        TokenMetadataRepresentation rep = JsonSerialization.readValue(tokenResponse, TokenMetadataRepresentation.class);
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
