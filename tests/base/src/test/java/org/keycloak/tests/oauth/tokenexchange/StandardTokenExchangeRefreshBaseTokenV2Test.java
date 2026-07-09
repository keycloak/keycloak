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

package org.keycloak.tests.oauth.tokenexchange;

import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.encode.AccessTokenContext;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.TokenUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


@KeycloakIntegrationTest
public class StandardTokenExchangeRefreshBaseTokenV2Test extends AbstractBaseTokenExchangeTest {

    @Test
    public void testScopeParamIncludedAudienceIncludedRefreshToken() {
        requesterClient.getAttributes().put(OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_REFRESH_ENABLED, OIDCAdvancedConfigWrapper.TokenExchangeRefreshTokenEnabled.SAME_SESSION.name());
        realm.admin().clients().get(requesterClient.getId()).update(requesterClient);

        String accessToken = resourceOwnerLogin("mike", "password", "subject-client", "secret").getAccessToken();
        oauth.scope("optional-scope2");
        AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret", List.of("target-client1"), OAuth2Constants.REFRESH_TOKEN_TYPE);
        assertAudiencesAndScopes(response, mike, List.of("target-client1"), List.of("default-scope1", "optional-scope2"), OAuth2Constants.REFRESH_TOKEN_TYPE, "subject-client");
        assertNotNull(response.getRefreshToken());

        oauth.client("requester-client", "secret");
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        AccessToken exchangedToken = assertAudiencesAndScopes(response, List.of("requester-client", "target-client1"), List.of("default-scope1", "optional-scope2"));
        EventRepresentation event = events.poll();
        EventAssertion.assertSuccess(event)
                .type(EventType.REFRESH_TOKEN)
                .details(Details.TOKEN_ID, exchangedToken.getId())
                .details(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_REFRESH)
                .sessionId(exchangedToken.getSessionId());

        oauth.client("requester-client", "secret");
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        exchangedToken = assertAudiencesAndScopes(response, List.of("requester-client", "target-client1"), List.of("default-scope1", "optional-scope2"));
        event = events.poll();
        EventAssertion.assertSuccess(event)
                .type(EventType.REFRESH_TOKEN)
                .details(Details.TOKEN_ID, exchangedToken.getId())
                .details(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_REFRESH)
                .sessionId(exchangedToken.getSessionId());

        requesterClient.getAttributes().put(OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_REFRESH_ENABLED, OIDCAdvancedConfigWrapper.TokenExchangeRefreshTokenEnabled.NO.name());
        realm.admin().clients().get(requesterClient.getId()).update(requesterClient);
    }

    @Test
    public void testExchangeNoRefreshToken() {
        String accessToken = resourceOwnerLogin("john", "password","subject-client", "secret").getAccessToken();

        AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret", null, null);
        assertEquals(OAuth2Constants.ACCESS_TOKEN_TYPE, response.getIssuedTokenType());
        String exchangedTokenString = response.getAccessToken();
        String refreshTokenString = response.getRefreshToken();
        assertNotNull(exchangedTokenString);
        assertNull(refreshTokenString);

        requesterClient.getAttributes().put(OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_REFRESH_ENABLED, OIDCAdvancedConfigWrapper.TokenExchangeRefreshTokenEnabled.SAME_SESSION.name());
        requesterClient.getAttributes().put(OIDCConfigAttributes.USE_REFRESH_TOKEN, Boolean.FALSE.toString());
        realm.admin().clients().get(requesterClient.getId()).update(requesterClient);

        realm.admin().clients().get(requesterClient.getId()).update(requesterClient);
        response = tokenExchange(accessToken, "requester-client", "secret", null,
                OAuth2Constants.REFRESH_TOKEN_TYPE);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        assertEquals("requested_token_type unsupported", response.getErrorDescription());

        requesterClient.getAttributes().put(OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_REFRESH_ENABLED, OIDCAdvancedConfigWrapper.TokenExchangeRefreshTokenEnabled.NO.name());
        requesterClient.getAttributes().put(OIDCConfigAttributes.USE_REFRESH_TOKEN, Boolean.TRUE.toString());
        realm.admin().clients().get(requesterClient.getId()).update(requesterClient);
    }

    @Test
    public void testOfflineAccessNotAllowed() {
        requesterClient.getAttributes().put(OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_REFRESH_ENABLED, OIDCAdvancedConfigWrapper.TokenExchangeRefreshTokenEnabled.SAME_SESSION.name());
        realm.admin().clients().get(requesterClient.getId()).update(requesterClient);

        String accessToken = resourceOwnerLogin("mike", "password", "subject-client", "secret").getAccessToken();
        String sessionId = getSessionIdFromToken(accessToken);
        assertEquals(getClientSessionsCountInUserSession(sessionId), Integer.valueOf(1));

        oauth.scope("offline_access");
        AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret", List.of("target-client1"), OAuth2Constants.REFRESH_TOKEN_TYPE);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        assertEquals("Scope offline_access not allowed for token exchange", response.getErrorDescription());

        // Check that client session was not created
        assertEquals(getClientSessionsCountInUserSession(sessionId), Integer.valueOf(1));

        requesterClient.getAttributes().put(OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_REFRESH_ENABLED, OIDCAdvancedConfigWrapper.TokenExchangeRefreshTokenEnabled.NO.name());
        realm.admin().clients().get(requesterClient.getId()).update(requesterClient);
    }

    @Test
    public void testOfflineAccessLoginWithRegularTokenExchange() {
        requesterClient.getAttributes().put(OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_REFRESH_ENABLED, OIDCAdvancedConfigWrapper.TokenExchangeRefreshTokenEnabled.SAME_SESSION.name());
        realm.admin().clients().get(requesterClient.getId()).update(requesterClient);

        String offlineAccessScopeId = realm.admin().clientScopes().findAll().stream()
                .filter(scope -> OAuth2Constants.OFFLINE_ACCESS.equals(scope.getName()))
                .findFirst()
                .orElseThrow()
                .getId();
        realm.admin().clients().get(subjectClient.getId()).addOptionalClientScope(offlineAccessScopeId);


        // Login with "scope=offline_access" . Will create offline user-session
        String accessToken = resourceOwnerLogin("mike", "password", "subject-client", "secret", OAuth2Constants.OFFLINE_ACCESS).getAccessToken();
        AccessToken originalToken = verifyAccessToken(accessToken);

        AccessTokenContext ctx = getAccessTokenContext(originalToken.getId());
        assertEquals(AccessTokenContext.SessionType.OFFLINE, ctx.getSessionType());

        // normal access token exchange is allowed for the offline session
        oauth.scope(null);
        AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret", List.of("target-client1"), null);
        AccessToken exchangedToken = assertAudiencesAndScopes(response, mike, List.of("target-client1"), List.of("default-scope1"));
        assertEquals(originalToken.getSessionId(), exchangedToken.getSessionId());

        // Refresh token-exchange without "scope=offline_access". Not allowed cos a new new "online" user session is needed (as previous one was offline)
        oauth.scope(null);
        response = tokenExchange(accessToken, "requester-client", "secret", List.of("target-client1"), OAuth2Constants.REFRESH_TOKEN_TYPE);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(Errors.INVALID_REQUEST, response.getError());
        assertEquals("Refresh token not valid as requested_token_type because creating a new session is needed", response.getErrorDescription());
        EventAssertion.assertError(events.poll())
                .type(EventType.TOKEN_EXCHANGE_ERROR)
                .clientId("requester-client")
                .error(Errors.INVALID_REQUEST)
                .userId(mike.getId())
                .sessionId(originalToken.getSessionId())
                .details(Details.REASON, "Refresh token not valid as requested_token_type because creating a new session is needed");

        requesterClient.getAttributes().put(OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_REFRESH_ENABLED, OIDCAdvancedConfigWrapper.TokenExchangeRefreshTokenEnabled.NO.name());
        realm.admin().clients().get(requesterClient.getId()).update(requesterClient);
        realm.admin().clients().get(subjectClient.getId()).removeOptionalClientScope(offlineAccessScopeId);
    }

    @Test
    public void testOfflineAccessNotAllowedAfterOfflineAccessLogin() {
        requesterClient.getAttributes().put(OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_REFRESH_ENABLED, OIDCAdvancedConfigWrapper.TokenExchangeRefreshTokenEnabled.SAME_SESSION.name());
        realm.admin().clients().get(requesterClient.getId()).update(requesterClient);

        // Login with "scope=offline_access" . Will create offline user-session
        String accessToken = resourceOwnerLogin("mike", "password", "subject-client", "secret", OAuth2Constants.OFFLINE_ACCESS).getAccessToken();

        // Doublecheck count of sessions
        String subjectClientUuid = subjectClient.getId();
        String requesterClientUuid = requesterClient.getId();
        UserResource user = realm.admin().users().get(mike.getId());
        assertEquals(0, user.getUserSessions().size());
        assertEquals(1, user.getOfflineSessions(subjectClientUuid).size());
        assertEquals(0, user.getOfflineSessions(requesterClientUuid).size());

        // Token exchange with scope=offline-access should not be allowed
        oauth.scope("offline_access");
        AccessTokenResponse response = tokenExchange(accessToken, "requester-client", "secret", List.of("target-client1"), OAuth2Constants.REFRESH_TOKEN_TYPE);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());

        // Make sure not new user sessions persisted
        assertEquals(0, user.getUserSessions().size());
        assertEquals(1, user.getOfflineSessions(subjectClientUuid).size());
        assertEquals(0, user.getOfflineSessions(requesterClientUuid).size());

        oauth.scope("openid");
        requesterClient.getAttributes().put(OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_REFRESH_ENABLED, OIDCAdvancedConfigWrapper.TokenExchangeRefreshTokenEnabled.NO.name());
        realm.admin().clients().get(requesterClient.getId()).update(requesterClient);
    }

}
