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

package org.keycloak.testsuite.oidc;

import java.util.List;

import javax.ws.rs.core.UriBuilder;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.events.Details;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.ActionURIUtils;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ScopeParameterTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected AccountUpdateProfilePage profilePage;

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
        oauth.maxAge(null);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = AbstractAdminTest.loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realm);
    }


    // If scope=openid is missing, IDToken won't be present
    @Test
    public void testMissingScopeOpenid() {
        String loginFormUrl = oauth.getLoginFormUrl();
        loginFormUrl = ActionURIUtils.removeQueryParamFromURI(loginFormUrl, OAuth2Constants.SCOPE);

        driver.navigate().to(loginFormUrl);
        oauth.fillLoginForm("test-user@localhost", "password");
        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String code = new OAuthClient.AuthorizationEndpointResponse(oauth).getCode();
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        // IDToken is not there
        Assert.assertEquals(200, response.getStatusCode());
        Assert.assertNull(response.getIdToken());
        Assert.assertNotNull(response.getRefreshToken());

        AccessToken token = oauth.verifyToken(response.getAccessToken());
        Assert.assertEquals(token.getSubject(), loginEvent.getUserId());

        // Refresh and assert idToken still not present
        response = oauth.doRefreshTokenRequest(response.getRefreshToken(), "password");
        Assert.assertEquals(200, response.getStatusCode());
        Assert.assertNull(response.getIdToken());

        token = oauth.verifyToken(response.getAccessToken());
        Assert.assertEquals(token.getSubject(), loginEvent.getUserId());
    }


    // If scope=openid is missing, IDToken won't be present
    @Test
    public void testMissingScopeOpenidInResourceOwnerPasswordCredentialRequest() throws Exception {
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("password", "test-user@localhost", "password");

        assertEquals(200, response.getStatusCode());

        // idToken not present
        Assert.assertNull(response.getIdToken());

        Assert.assertNotNull(response.getRefreshToken());
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        Assert.assertEquals(accessToken.getPreferredUsername(), "test-user@localhost");

    }
}
