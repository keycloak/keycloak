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

package org.keycloak.tests.oidc.flows;

import java.io.IOException;
import java.util.List;

import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.HashUtils;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.common.BasicRealmWithUserConfig;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.util.TokenUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Abstract test for various values of response_type
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractOIDCResponseTypeTest {

    @InjectRealm(config = BasicRealmWithUserConfig.class)
    protected ManagedRealm managedRealm;

    @InjectClient(config = TestClientConfig.class, lifecycle = LifeCycle.METHOD)
    protected ManagedClient client;

    @InjectOAuthClient
    protected OAuthClient oauth;

    @InjectEvents
    protected Events events;

    @InjectPage
    protected LoginPage loginPage;

    @InjectWebDriver(lifecycle = LifeCycle.METHOD)
    protected ManagedWebDriver driver;

    @Test
    public void nonceAndSessionStateMatches() {
        EventRepresentation loginEvent = loginUser("abcdef123456");

        AuthorizationEndpointResponse authzResponse = oauth.parseLoginResponse();
        Assertions.assertNotNull(authzResponse.getSessionState());

        List<IDToken> idTokens = testAuthzResponseAndRetrieveIDTokens(authzResponse, loginEvent);

        for (IDToken idToken : idTokens) {
            Assertions.assertEquals("abcdef123456", idToken.getNonce());
            Assertions.assertEquals(authzResponse.getSessionState(), idToken.getSessionState());
        }
    }

    @Test
    public void authorizationRequestMissingResponseType() throws IOException {
        oauth.responseType(null);
        oauth.client(client.getClientId(), client.getSecret()).openLoginForm();

        // Always read error from the "query"
        AuthorizationEndpointResponse errorResponse = oauth.parseLoginResponse();
        Assertions.assertTrue(errorResponse.isRedirected());
        Assertions.assertEquals(errorResponse.getError(), OAuthErrorException.INVALID_REQUEST);

        EventAssertion.assertError(events.poll()).type(EventType.LOGIN_ERROR).error(Errors.INVALID_REQUEST).userId(null).sessionId(null);
    }

    @Test
    public void oidcFlow_RealmRS256_ClientRS384() throws Exception {
        oidcFlowRequest(Algorithm.RS256, Algorithm.RS384);
    }

    @Test
    public void oidcFlow_RealmES256_ClientES384() throws Exception {
        oidcFlowRequest(Algorithm.ES256, Algorithm.ES384);
    }

    @Test
    public void oidcFlow_RealmRS256_ClientPS256() throws Exception {
        oidcFlowRequest(Algorithm.RS256, Algorithm.PS256);
    }

    @Test
    public void oidcFlow_RealmPS256_ClientES256() throws Exception {
        oidcFlowRequest(Algorithm.PS256, Algorithm.ES256);
    }

    @Test
    public void oidcFlow_RealmEdDSA_ClientES256() throws Exception {
        oidcFlowRequest(Algorithm.EdDSA, Algorithm.ES256);
    }

    @Test
    public void oidcFlow_RealmPS256_ClientEdDSA() throws Exception {
        oidcFlowRequest(Algorithm.PS256, Algorithm.EdDSA);
    }

    private void oidcFlowRequest(String expectedAccessAlg, String expectedIdTokenAlg) throws Exception {
        try {
            setIdTokenSignatureAlgorithm(expectedIdTokenAlg);
            // Realm setting is used for access token signature algorithm
            managedRealm.updateWithCleanup(realm -> realm.defaultSignatureAlgorithm(expectedAccessAlg));
            client.updateWithCleanup(c -> c.attribute(OIDCConfigAttributes.ID_TOKEN_SIGNED_RESPONSE_ALG, expectedIdTokenAlg));
            oidcFlow(expectedAccessAlg, expectedIdTokenAlg);
        } finally {
            setIdTokenSignatureAlgorithm(Algorithm.RS256);
        }
    }

    private String idTokenSigAlgName = Algorithm.RS256;
    private void setIdTokenSignatureAlgorithm(String idTokenSigAlgName) {
        this.idTokenSigAlgName = idTokenSigAlgName;
    }
    protected String getIdTokenSignatureAlgorithm() {
        return this.idTokenSigAlgName;
    }

    /**
     *  Validate "at_hash" claim in IDToken.
     *  see KEYCLOAK-9635
     * @param accessTokenHash
     * @param accessToken
     */
    protected void assertValidAccessTokenHash(String accessTokenHash, String accessToken) {

        Assertions.assertNotNull(accessTokenHash);
        Assertions.assertNotNull(accessToken);
        assertEquals(accessTokenHash, HashUtils.accessTokenHash(getIdTokenSignatureAlgorithm(), accessToken));
    }

    protected void validateErrorStandardFlowNotAllowed() throws Exception {
        // Disable standard flow for client
        ClientRepresentation clientRep = client.admin().toRepresentation();
        clientRep.setStandardFlowEnabled(false);
        client.admin().update(clientRep);

        oauth.client(client.getClientId(), client.getSecret()).openLoginForm();

        AuthorizationEndpointResponse errorResponse = oauth.parseLoginResponse();
        Assertions.assertTrue(errorResponse.isRedirected());
        Assertions.assertEquals(errorResponse.getError(), OAuthErrorException.UNAUTHORIZED_CLIENT);
        Assertions.assertEquals(errorResponse.getErrorDescription(), "Client is not allowed to initiate browser login with given response_type. Standard flow is disabled for the client.");

        EventAssertion.assertError(events.poll()).type(EventType.LOGIN_ERROR).error(Errors.NOT_ALLOWED).userId(null).sessionId(null);

    }

    protected EventRepresentation loginUser(String nonce) {
        oauth.client(client.getClientId(), client.getSecret()).loginForm().nonce(nonce).state("somestate").open();

        loginPage.assertCurrent();
        loginPage.fillLogin(BasicRealmWithUserConfig.USERNAME, BasicRealmWithUserConfig.PASSWORD);
        loginPage.submit();
        Assertions.assertTrue(driver.driver().getCurrentUrl().startsWith(oauth.getRedirectUri()));
        Assertions.assertTrue(oauth.parseLoginResponse().getState().equals("somestate"));

        EventRepresentation eventRep = events.poll();
        EventAssertion.expectLoginSuccess(eventRep).details(Details.USERNAME, BasicRealmWithUserConfig.USERNAME);
        return eventRep;
    }

    protected abstract List<IDToken> testAuthzResponseAndRetrieveIDTokens(AuthorizationEndpointResponse authzResponse, EventRepresentation loginEvent);

    protected AccessTokenResponse sendTokenRequestAndGetResponse(EventRepresentation loginEvent) {
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);
        Assertions.assertEquals(200, response.getStatusCode());
        EventAssertion.expectCodeToTokenSuccess(events.poll())
                .sessionId(loginEvent.getSessionId())
                .userId(loginEvent.getUserId())
                .details(Details.CODE_ID, loginEvent.getDetails().get(Details.CODE_ID))
                .details(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_REFRESH)
                .details(Details.CLIENT_AUTH_METHOD, ClientIdAndSecretAuthenticator.PROVIDER_ID);
        return response;
    }

    private void oidcFlow(String expectedAccessAlg, String expectedIdTokenAlg) throws Exception {
        EventRepresentation loginEvent = loginUser("abcdef123456");

        AuthorizationEndpointResponse authzResponse = oauth.parseLoginResponse();
        Assertions.assertNotNull(authzResponse.getSessionState());

        JWSHeader header;
        String idToken = authzResponse.getIdToken();
        String accessToken = authzResponse.getAccessToken();
        if (idToken != null) {
            header = new JWSInput(idToken).getHeader();
            verifySignatureAlgorithm(header, expectedIdTokenAlg);
            assertEquals("JWT", header.getType());
            assertNull(header.getContentType());
        }
        if (accessToken != null) {
            header = new JWSInput(accessToken).getHeader();
            verifySignatureAlgorithm(header, expectedAccessAlg);
            assertEquals("JWT", header.getType());
            assertNull(header.getContentType());
        }

        List<IDToken> idTokens = testAuthzResponseAndRetrieveIDTokens(authzResponse, loginEvent);

        for (IDToken idt : idTokens) {
            Assertions.assertEquals("abcdef123456", idt.getNonce());
            Assertions.assertEquals(authzResponse.getSessionState(), idt.getSessionState());
        }
    }

    private void verifySignatureAlgorithm(JWSHeader header, String expectedAlgorithm) {
        assertEquals(expectedAlgorithm, header.getAlgorithm().name());
    }

    public static class TestClientConfig implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId("response-type-client").secret("password")
                    .redirectUris("*")
                    .implicitFlowEnabled(false);
        }
    }
}
