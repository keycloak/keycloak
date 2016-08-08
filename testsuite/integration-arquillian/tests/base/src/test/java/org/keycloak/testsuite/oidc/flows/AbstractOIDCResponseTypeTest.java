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

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuthErrorException;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.jose.jws.Algorithm;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.TestRealmKeycloakTest;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Abstract test for various values of response_type
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractOIDCResponseTypeTest extends TestRealmKeycloakTest {

    // Harcoded for now
    Algorithm jwsAlgorithm = Algorithm.RS256;

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
    public void nonceMatches() {
        EventRepresentation loginEvent = loginUser("abcdef123456");
        List<IDToken> idTokens = retrieveIDTokens(loginEvent);

        for (IDToken idToken : idTokens) {
            Assert.assertEquals("abcdef123456", idToken.getNonce());
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
        Assert.assertEquals(errorResponse.getError(), OAuthErrorException.UNSUPPORTED_RESPONSE_TYPE);
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
        Assert.assertEquals(errorResponse.getError(), OAuthErrorException.UNSUPPORTED_RESPONSE_TYPE);
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

    protected abstract List<IDToken> retrieveIDTokens(EventRepresentation loginEvent);

    protected ClientManager.ClientManagerBuilder clientManagerBuilder() {
        return ClientManager.realm(adminClient.realm("test")).clientId("test-app");
    }
}
