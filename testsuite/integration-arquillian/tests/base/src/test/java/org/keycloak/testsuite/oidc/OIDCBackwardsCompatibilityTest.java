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

package org.keycloak.testsuite.oidc;

import java.io.IOException;

import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertTrue;



/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OIDCBackwardsCompatibilityTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected OAuthGrantPage grantPage;

    @Page
    protected ErrorPage errorPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Before
    public void clientConfiguration() {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").directAccessGrant(true);
        /*
         * Configure the default client ID. Seems like OAuthClient is keeping the state of clientID
         * For example: If some test case configure oauth.clientId("sample-public-client"), other tests
         * will faile and the clientID will always be "sample-public-client
         * @see AccessTokenTest#testAuthorizationNegotiateHeaderIgnored()
         */
        oauth.clientId("test-app");
    }


    // KEYCLOAK-6286
    @Test
    public void testExcludeSessionStateParameter() {
        // Open login form and login successfully. Assert session_state is present
        AuthorizationEndpointResponse authzResponse = oauth.doLogin("test-user@localhost", "password");
        EventRepresentation loginEvent = events.expectLogin().assertEvent();
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(authzResponse.getSessionState());

        // Switch "exclude session_state" to on
        ClientResource client = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
        ClientRepresentation clientRep = client.toRepresentation();
        OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep);
        config.setExcludeSessionStateFromAuthResponse(true);
        client.update(clientRep);

        // Open login again and assert session_state not present
        oauth.openLoginForm();
        org.keycloak.testsuite.Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();

        authzResponse = oauth.parseLoginResponse();
        Assert.assertNull(authzResponse.getSessionState());

        // Revert
        config.setExcludeSessionStateFromAuthResponse(false);
        client.update(clientRep);
    }

    @Test
    public void testExcludeIssuerParameter() {
        // Open login form and login successfully. Assert iss parameter is present
        AuthorizationEndpointResponse authzResponse = oauth.doLogin("test-user@localhost", "password");
        events.expectLogin().assertEvent();
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertEquals(oauth.AUTH_SERVER_ROOT + "/realms/test", authzResponse.getIssuer());

        // Switch "exclude iss" to on
        ClientResource client = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
        ClientRepresentation clientRep = client.toRepresentation();
        OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep);
        config.setExcludeIssuerFromAuthResponse(true);
        client.update(clientRep);

        // Open login again and assert iss parameter is not present
        oauth.openLoginForm();
        org.keycloak.testsuite.Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();

        authzResponse = oauth.parseLoginResponse();
        Assert.assertNull(authzResponse.getIssuer());

        // Revert
        config.setExcludeIssuerFromAuthResponse(false);
        client.update(clientRep);
    }

    @Test
    public void testExcludeIssuerParameterOnError() throws IOException {
        // Open login form and login fails. Assert iss parameter is present
        oauth.responseType("tokenn");
        oauth.openLoginForm();

        AuthorizationEndpointResponse errorResponse = oauth.parseLoginResponse();
        assertTrue(errorResponse.isRedirected());
        Assert.assertEquals(errorResponse.getError(), OAuthErrorException.UNSUPPORTED_RESPONSE_TYPE);
        Assert.assertEquals(oauth.AUTH_SERVER_ROOT + "/realms/test", errorResponse.getIssuer());

        events.expectLogin().error(Errors.INVALID_REQUEST).user((String) null).session((String) null).clearDetails().detail(Details.RESPONSE_TYPE, "tokenn").assertEvent();

        // Switch "exclude iss" to on
        ClientResource client = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
        ClientRepresentation clientRep = client.toRepresentation();
        OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep);
        config.setExcludeIssuerFromAuthResponse(true);
        client.update(clientRep);

        // Open login again and assert iss parameter is not present
        oauth.openLoginForm();

        errorResponse = oauth.parseLoginResponse();
        assertTrue(errorResponse.isRedirected());
        Assert.assertEquals(errorResponse.getError(), OAuthErrorException.UNSUPPORTED_RESPONSE_TYPE);
        Assert.assertNull(errorResponse.getIssuer());

        events.expectLogin().error(Errors.INVALID_REQUEST).user((String) null).session((String) null).clearDetails().detail(Details.RESPONSE_TYPE, "tokenn").assertEvent();

    }
}
