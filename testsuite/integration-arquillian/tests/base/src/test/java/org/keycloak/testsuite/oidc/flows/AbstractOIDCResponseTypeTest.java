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

package org.keycloak.testsuite.oidc.flows;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuthErrorException;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.HashUtils;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.TokenSignatureUtil;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Abstract test for various values of response_type
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractOIDCResponseTypeTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }


    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = AbstractAdminTest.loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realm);
    }


    @Test
    public void nonceAndSessionStateMatches() {
        EventRepresentation loginEvent = loginUser("abcdef123456");

        OAuthClient.AuthorizationEndpointResponse authzResponse = new OAuthClient.AuthorizationEndpointResponse(oauth, isFragment());
        Assert.assertNotNull(authzResponse.getSessionState());

        List<IDToken> idTokens = testAuthzResponseAndRetrieveIDTokens(authzResponse, loginEvent);

        for (IDToken idToken : idTokens) {
            Assert.assertEquals("abcdef123456", idToken.getNonce());
            Assert.assertEquals(authzResponse.getSessionState(), idToken.getSessionState());
        }
    }


    @Test
    public void initialSessionStateUsedInRedirect() {
        EventRepresentation loginEvent = loginUserWithRedirect("abcdef123456", OAuthClient.APP_ROOT + "/auth?session_state=foo");

        OAuthClient.AuthorizationEndpointResponse authzResponse = new OAuthClient.AuthorizationEndpointResponse(oauth, isFragment());
        Assert.assertNotNull(authzResponse.getSessionState());

        List<IDToken> idTokens = testAuthzResponseAndRetrieveIDTokens(authzResponse, loginEvent);

        for (IDToken idToken : idTokens) {
            Assert.assertEquals(authzResponse.getSessionState(), idToken.getSessionState());
        }
    }


    @Test
    public void authorizationRequestMissingResponseType() throws IOException {
        oauth.responseType(null);
        UriBuilder b = UriBuilder.fromUri(oauth.getLoginFormUrl());
        driver.navigate().to(b.build().toURL());

        // Always read error from the "query"
        OAuthClient.AuthorizationEndpointResponse errorResponse = new OAuthClient.AuthorizationEndpointResponse(oauth, false);
        org.junit.Assert.assertTrue(errorResponse.isRedirected());
        org.junit.Assert.assertEquals(errorResponse.getError(), OAuthErrorException.INVALID_REQUEST);

        events.expectLogin().error(Errors.INVALID_REQUEST).user((String) null).session((String) null).clearDetails().assertEvent();
    }

    protected void validateNonceNotUsedSuccessExpected() {
    	loginUser(null);
    }

    protected void validateNonceNotUsedErrorExpected() {
        oauth.nonce(null);
        driver.navigate().to(oauth.getLoginFormUrl());

        assertFalse(loginPage.isCurrent());
        assertTrue(appPage.isCurrent());

        // Assert error response was sent because not logged in
        OAuthClient.AuthorizationEndpointResponse resp = new OAuthClient.AuthorizationEndpointResponse(oauth);
        Assert.assertNull(resp.getCode());
        Assert.assertNull(resp.getIdToken());
        Assert.assertEquals(OAuthErrorException.INVALID_REQUEST, resp.getError());
        Assert.assertEquals("Missing parameter: nonce", resp.getErrorDescription());
    }


    protected void validateErrorImplicitFlowNotAllowed() throws Exception {
        // Disable implicit flow for client
        clientManagerBuilder().implicitFlow(false);

        UriBuilder b = UriBuilder.fromUri(oauth.getLoginFormUrl());
        driver.navigate().to(b.build().toURL());

        OAuthClient.AuthorizationEndpointResponse errorResponse = new OAuthClient.AuthorizationEndpointResponse(oauth);
        Assert.assertTrue(errorResponse.isRedirected());
        Assert.assertEquals(errorResponse.getError(), OAuthErrorException.UNAUTHORIZED_CLIENT);
        Assert.assertEquals(errorResponse.getErrorDescription(), "Client is not allowed to initiate browser login with given response_type. Implicit flow is disabled for the client.");

        events.expectLogin().error(Errors.NOT_ALLOWED).user((String) null).session((String) null).clearDetails().assertEvent();

        // Revert
        clientManagerBuilder().implicitFlow(true);
    }


    protected void validateErrorStandardFlowNotAllowed() throws Exception {
        // Disable standard flow for client
        clientManagerBuilder().standardFlow(false);

        UriBuilder b = UriBuilder.fromUri(oauth.getLoginFormUrl());
        driver.navigate().to(b.build().toURL());

        OAuthClient.AuthorizationEndpointResponse errorResponse = new OAuthClient.AuthorizationEndpointResponse(oauth);
        Assert.assertTrue(errorResponse.isRedirected());
        Assert.assertEquals(errorResponse.getError(), OAuthErrorException.UNAUTHORIZED_CLIENT);
        Assert.assertEquals(errorResponse.getErrorDescription(), "Client is not allowed to initiate browser login with given response_type. Standard flow is disabled for the client.");

        events.expectLogin().error(Errors.NOT_ALLOWED).user((String) null).session((String) null).clearDetails().assertEvent();

        // Revert
        clientManagerBuilder().standardFlow(true);
    }



    protected EventRepresentation loginUser(String nonce) {
        if (nonce != null) {
            oauth.nonce(nonce);
        }

        driver.navigate().to(oauth.getLoginFormUrl());

        loginPage.assertCurrent();
        loginPage.login("test-user@localhost", "password");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        return events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
    }

    protected EventRepresentation loginUserWithRedirect(String nonce, String redirectUri) {
        if (nonce != null) {
            oauth.nonce(nonce);
        }

        if (redirectUri != null) {
            oauth.redirectUri(redirectUri);
        }

        driver.navigate().to(oauth.getLoginFormUrl());

        loginPage.assertCurrent();
        loginPage.login("test-user@localhost", "password");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        return events.expectLogin().detail(Details.REDIRECT_URI, redirectUri).detail(Details.USERNAME, "test-user@localhost").assertEvent();
    }

    protected abstract boolean isFragment();

    protected abstract List<IDToken> testAuthzResponseAndRetrieveIDTokens(OAuthClient.AuthorizationEndpointResponse authzResponse, EventRepresentation loginEvent);

    protected ClientManager.ClientManagerBuilder clientManagerBuilder() {
        return ClientManager.realm(adminClient.realm("test")).clientId("test-app");
    }

    private void oidcFlow(String expectedAccessAlg, String expectedIdTokenAlg) throws Exception {
        EventRepresentation loginEvent = loginUser("abcdef123456");

        OAuthClient.AuthorizationEndpointResponse authzResponse = new OAuthClient.AuthorizationEndpointResponse(oauth, isFragment());
        Assert.assertNotNull(authzResponse.getSessionState());

        JWSHeader header = null;
        String idToken = authzResponse.getIdToken();
        String accessToken = authzResponse.getAccessToken();
        if (idToken != null) {
            header = new JWSInput(idToken).getHeader();
            assertEquals(expectedIdTokenAlg, header.getAlgorithm().name());
            assertEquals("JWT", header.getType());
            assertNull(header.getContentType());
        }
        if (accessToken != null) {
            header = new JWSInput(accessToken).getHeader();
            assertEquals(expectedAccessAlg, header.getAlgorithm().name());
            assertEquals("JWT", header.getType());
            assertNull(header.getContentType());
        }

        List<IDToken> idTokens = testAuthzResponseAndRetrieveIDTokens(authzResponse, loginEvent);

        for (IDToken idt : idTokens) {
            Assert.assertEquals("abcdef123456", idt.getNonce());
            Assert.assertEquals(authzResponse.getSessionState(), idt.getSessionState());
        }
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

    private void oidcFlowRequest(String expectedAccessAlg, String expectedIdTokenAlg) throws Exception {
        try {
            setIdTokenSignatureAlgorithm(expectedIdTokenAlg);
            // Realm setting is used for access token signature algorithm
            TokenSignatureUtil.changeRealmTokenSignatureProvider(adminClient, expectedAccessAlg);
            TokenSignatureUtil.changeClientIdTokenSignatureProvider(ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app"), expectedIdTokenAlg);
            oidcFlow(expectedAccessAlg, expectedIdTokenAlg);
        } finally {
            setIdTokenSignatureAlgorithm(Algorithm.RS256);
            TokenSignatureUtil.changeRealmTokenSignatureProvider(adminClient, Algorithm.RS256);
            TokenSignatureUtil.changeClientIdTokenSignatureProvider(ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app"), Algorithm.RS256);
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

        Assert.assertNotNull(accessTokenHash);
        Assert.assertNotNull(accessToken);
        assertEquals(accessTokenHash, HashUtils.oidcHash(getIdTokenSignatureAlgorithm(), accessToken));
    }

    /**
     * Validate  "c_hash" claim in IDToken.
     * @param codeHash
     * @param code
     */
    protected void assertValidCodeHash(String codeHash, String code) {

        Assert.assertNotNull(codeHash);
        Assert.assertNotNull(code);
        Assert.assertEquals(codeHash, HashUtils.oidcHash(getIdTokenSignatureAlgorithm(), code));
    }
}
