/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
import java.util.Arrays;
import java.util.Collection;

import jakarta.ws.rs.core.Response;

import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.crypto.AesCbcHmacShaContentEncryptionProvider;
import org.keycloak.crypto.AesGcmContentEncryptionProvider;
import org.keycloak.crypto.RsaCekManagementProvider;
import org.keycloak.events.Details;
import org.keycloak.jose.JOSEHeader;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.jose.jwe.JWEHeader;
import org.keycloak.jose.jwe.alg.JWEAlgorithmProvider;
import org.keycloak.jose.jwe.enc.JWEEncryptionProvider;
import org.keycloak.keys.Attributes;
import org.keycloak.keys.GeneratedEcdsaKeyProviderFactory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginTotpPage;
import org.keycloak.testframework.ui.page.OAuthGrantPage;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;

import org.junit.jupiter.api.Assertions;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractOIDCScopeTest {

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectEvents
    Events events;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    LoginTotpPage loginTotpPage;

    @InjectPage
    OAuthGrantPage grantPage;

    @InjectPage
    ErrorPage errorPage;


    protected Tokens sendTokenRequest(EventRepresentation loginEvent, String userId, String expectedScope, String clientId) {
        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.client(clientId, "password").doAccessTokenRequest(code);
        Assertions.assertEquals(200, response.getStatusCode());
        
        assertScopes(expectedScope, response.getScope());

        IDToken idToken = oauth.verifyIDToken(response.getIdToken());
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());

        // Test scope in the access token
        assertScopes(expectedScope, accessToken.getScope());

        EventRepresentation codeToTokenEvent = EventAssertion.expectCodeToTokenSuccess(events.poll())
                .sessionId(sessionId)
                .userId(userId)
                .clientId(clientId)
                .details(Details.CODE_ID, codeId)
                .details(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_REFRESH)
                .details(Details.CLIENT_AUTH_METHOD, ClientIdAndSecretAuthenticator.PROVIDER_ID).getEvent();

        // Test scope in the event
        assertScopes(expectedScope, codeToTokenEvent.getDetails().get(Details.SCOPE));

        return new Tokens(idToken, accessToken, response.getRefreshToken());
    }

    public static void assertScopes(String expectedScope, String receivedScope) {
        Collection<String> expectedScopes = Arrays.asList(expectedScope.split(" "));
        Collection<String> receivedScopes = Arrays.asList(receivedScope.split(" "));
        Assertions.assertTrue(expectedScopes.containsAll(receivedScopes) && receivedScopes.containsAll(expectedScopes),
                "Not matched. expectedScope: " + expectedScope + ", receivedScope: " + receivedScope);
    }

    /**
     * Helper function to authenticate with a username and password
     *
     * @param password The password to log in with
     * @param username The username to log in with
     */

    protected void authenticatePassword(String username, String password) {
        loginPage.assertCurrent();
        loginPage.fillLogin(username, password);
        loginPage.submit();
    }

    protected JWEEncryptionProvider getJweEncryptionProvider(String encAlgorithm) {
        JWEEncryptionProvider jweEncryptionProvider = null;
        switch (encAlgorithm) {
            case JWEConstants.A128GCM:
            case JWEConstants.A192GCM:
            case JWEConstants.A256GCM:
                jweEncryptionProvider = new AesGcmContentEncryptionProvider(null, encAlgorithm).jweEncryptionProvider();
                break;
            case JWEConstants.A128CBC_HS256:
            case JWEConstants.A192CBC_HS384:
            case JWEConstants.A256CBC_HS512:
                jweEncryptionProvider = new AesCbcHmacShaContentEncryptionProvider(null, encAlgorithm).jweEncryptionProvider();
                break;
            default:
                break;
        }
        return jweEncryptionProvider;
    }

    protected JOSEHeader getHeader(String base64Header) {
        try {
            byte[] decodedHeader = Base64Url.decode(base64Header);
            return JsonSerialization.readValue(decodedHeader, JWEHeader.class);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    protected JWEAlgorithmProvider getJweAlgorithmProvider(String algAlgorithm) {
        return new RsaCekManagementProvider(null, algAlgorithm).jweAlgorithmProvider();
    }

    protected void registerEcKeyProvider(String ecCurve, ManagedRealm realm) {
        ComponentRepresentation rep = new ComponentRepresentation();
        rep.setName("ecdsa-" + ecCurve);
        rep.setParentId(realm.admin().toRepresentation().getId());
        rep.setProviderId(GeneratedEcdsaKeyProviderFactory.ID);
        rep.setProviderType(KeyProvider.class.getName());
        rep.setConfig(new MultivaluedHashMap<>());
        rep.getConfig().putSingle(Attributes.PRIORITY_KEY, Long.toString(System.currentTimeMillis()));
        rep.getConfig().putSingle("active", "true");
        rep.getConfig().putSingle("enabled", "true");
        rep.getConfig().putSingle(GeneratedEcdsaKeyProviderFactory.ECDSA_ELLIPTIC_CURVE_KEY, ecCurve);

        try (Response response = realm.admin().components().add(rep)) {
            String id = ApiUtil.getCreatedId(response);
            realm.cleanup().add(r -> r.components().component(id).remove());
        }
    }

    static class Tokens {
        final IDToken idToken;
        final AccessToken accessToken;
        final String refreshToken;

        private Tokens(IDToken idToken, AccessToken accessToken, String refreshToken) {
            this.idToken = idToken;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }
}
