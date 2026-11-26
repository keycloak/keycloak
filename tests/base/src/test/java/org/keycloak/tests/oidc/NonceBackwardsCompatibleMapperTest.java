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

package org.keycloak.tests.oidc;

import java.io.IOException;

import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.NonceBackwardsCompatibleMapper;
import org.keycloak.protocol.oidc.utils.OIDCResponseMode;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AuthorizationResponseToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ProtocolMapperBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.common.BasicRealmWithUserConfig;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.util.TokenUtil;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author rmartinc
 */
@KeycloakIntegrationTest
public class NonceBackwardsCompatibleMapperTest {

    @InjectRealm(config = NonceBackwardsCompatibleMapperTestRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectEvents
    Events events;

    @Test
    public void testNonceWithoutMapper() throws IOException {
        testNonce(false, false);
    }

    @Test
    public void testNonceWithMapper() throws IOException {
        createNonceMapper();
        testNonce(true, false);
    }

    @Test
    public void testOfflineSessionNonceWithMapper() throws IOException {
        createNonceMapper();
        testNonce(true, true);
    }

    @Test
    public void testImplicitFlowWithoutMapper() throws Exception {
        managedRealm.updateClientWithCleanup(oauth.getClientId(), c -> c.implicitFlowEnabled(true));
        testNonceImplicit(false);
    }

    @Test
    public void testImplicitFlowWithMapper() throws Exception {
        createNonceMapper();
        managedRealm.updateClientWithCleanup(oauth.getClientId(), c -> c.implicitFlowEnabled(true));
        testNonceImplicit(true);
    }

    private void createNonceMapper() {
        ProtocolMapperRepresentation nonceMapper = ProtocolMapperBuilder.create()
                .name("nonce")
                .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                .protocolMapper(NonceBackwardsCompatibleMapper.PROVIDER_ID)
                .build();
        ApiUtil.getCreatedId(oauth.clientResource().getProtocolMappers().createMapper(nonceMapper));
    }

    private void checkNonce(String expectedNonce, String nonce, boolean expected) {
        if (expected) {
            Assertions.assertEquals(expectedNonce, nonce);
        } else {
            Assertions.assertNull(nonce);
        }
    }

    private void testIntrospection(String accessToken, String expectedNonce, boolean expected) throws IOException {
        JsonNode nonce = oauth.doIntrospectionAccessTokenRequest(accessToken).asJsonNode().get(OIDCLoginProtocol.NONCE_PARAM);
        checkNonce(expectedNonce, nonce != null? nonce.asText() : null, expected);
    }

    private void testNonceImplicit(boolean mapper) throws IOException {
        String nonce = KeycloakModelUtils.generateId();
        oauth.responseMode(OIDCResponseMode.JWT.value());
        oauth.responseType(OIDCResponseType.TOKEN + " " + OIDCResponseType.ID_TOKEN);
        AuthorizationEndpointResponse response = oauth.loginForm().nonce(nonce)
                .doLogin(BasicRealmWithUserConfig.USERNAME, BasicRealmWithUserConfig.PASSWORD);

        Assertions.assertTrue(response.isRedirected());
        AuthorizationResponseToken responseToken = oauth.verifyAuthorizationResponseToken(response.getResponse());

        String accessTokenString = (String) responseToken.getOtherClaims().get("access_token");
        AccessToken token = oauth.verifyToken(accessTokenString);
        checkNonce(nonce, token.getNonce(), mapper);
        String idTokenString = (String) responseToken.getOtherClaims().get("id_token");
        IDToken idToken = oauth.verifyToken(idTokenString, IDToken.class);
        checkNonce(nonce, idToken.getNonce(), true);

        testIntrospection(accessTokenString, nonce, mapper);
        testIntrospection(idTokenString, nonce, true);
    }

    private void testNonce(boolean mapper, boolean offlineSession) throws IOException {
        String nonce = KeycloakModelUtils.generateId();
        if (offlineSession) {
            oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        }
        oauth.loginForm().nonce(nonce).doLogin(BasicRealmWithUserConfig.USERNAME, BasicRealmWithUserConfig.PASSWORD);
        EventRepresentation loginEvent = EventAssertion.expectLoginSuccess(events.poll()).getEvent();

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);

        AccessToken token = oauth.verifyToken(response.getAccessToken());
        checkNonce(nonce, token.getNonce(), mapper);
        IDToken idToken = oauth.verifyToken(response.getIdToken(), IDToken.class);
        checkNonce(nonce, idToken.getNonce(), true);
        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        checkNonce(nonce, refreshToken.getNonce(), mapper);

        EventRepresentation tokenEvent = EventAssertion.expectCodeToTokenSuccess(events.poll())
                .sessionId(loginEvent.getSessionId())
                .details(Details.CODE_ID, loginEvent.getDetails().get(Details.CODE_ID))
                .details(Details.REFRESH_TOKEN_TYPE, offlineSession? TokenUtil.TOKEN_TYPE_OFFLINE : TokenUtil.TOKEN_TYPE_REFRESH)
                .details(Details.CLIENT_AUTH_METHOD, ClientIdAndSecretAuthenticator.PROVIDER_ID).getEvent();

        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        EventAssertion.expectRefreshTokenSuccess(events.poll())
                .sessionId(loginEvent.getSessionId())
                .details(Details.REFRESH_TOKEN_ID, tokenEvent.getDetails().get(Details.REFRESH_TOKEN_ID))
                .details(Details.REFRESH_TOKEN_TYPE, offlineSession? TokenUtil.TOKEN_TYPE_OFFLINE : TokenUtil.TOKEN_TYPE_REFRESH)
                .details(Details.CLIENT_AUTH_METHOD, ClientIdAndSecretAuthenticator.PROVIDER_ID);

        token = oauth.verifyToken(response.getAccessToken());
        checkNonce(nonce, token.getNonce(), mapper);
        idToken = oauth.verifyToken(response.getIdToken(), IDToken.class);
        checkNonce(nonce, idToken.getNonce(), mapper);
        refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        checkNonce(nonce, refreshToken.getNonce(), mapper);

        testIntrospection(response.getAccessToken(), nonce, mapper);
    }

    public static class NonceBackwardsCompatibleMapperTestRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.users(UserBuilder.create(BasicRealmWithUserConfig.USERNAME)
                    .password(BasicRealmWithUserConfig.PASSWORD)
                    .email("basic@localhost")
                    .name("First", "Last")
                    .realmRoles(OAuth2Constants.OFFLINE_ACCESS))
                    .enabledEventTypes(EventType.LOGIN.name(), EventType.CODE_TO_TOKEN.name(), EventType.REFRESH_TOKEN.name());
        }
    }
}
