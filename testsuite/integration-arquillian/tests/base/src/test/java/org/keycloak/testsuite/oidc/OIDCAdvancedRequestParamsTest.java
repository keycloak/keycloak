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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;

/**
 * Test for supporting advanced parameters of OIDC specs (max_age, nonce, prompt, ...)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OIDCAdvancedRequestParamsTest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);


    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
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

    @Test
    public void testMaxAge1() {
        // Open login form and login successfully
        oauth.doLogin("test-user@localhost", "password");
        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        IDToken idToken = retrieveIDToken(loginEvent);

        // Check that authTime is available and set to current time
        int authTime = idToken.getAuthTime();
        int currentTime = Time.currentTime();
        Assert.assertTrue(authTime <= currentTime && authTime + 3 >= currentTime);

        // Set time offset
        setTimeOffset(10);

        // Now open login form with maxAge=1
        oauth.maxAge("1");

        // Assert I need to login again through the login form
        oauth.doLogin("test-user@localhost", "password");
        loginEvent = events.expectLogin().assertEvent();

        idToken = retrieveIDToken(loginEvent);

        // Assert that authTime was updated
        int authTimeUpdated = idToken.getAuthTime();
        Assert.assertTrue(authTime + 10 <= authTimeUpdated);
    }

    @Test
    public void testMaxAge10000() {
        // Open login form and login successfully
        oauth.doLogin("test-user@localhost", "password");
        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        IDToken idToken = retrieveIDToken(loginEvent);

        // Check that authTime is available and set to current time
        int authTime = idToken.getAuthTime();
        int currentTime = Time.currentTime();
        Assert.assertTrue(authTime <= currentTime && authTime + 3 >= currentTime);

        // Set time offset
        setTimeOffset(10);

        // Now open login form with maxAge=10000
        oauth.maxAge("10000");

        // Assert that I will be automatically logged through cookie
        oauth.openLoginForm();
        loginEvent = events.expectLogin().assertEvent();

        idToken = retrieveIDToken(loginEvent);

        // Assert that authTime is still the same
        int authTimeUpdated = idToken.getAuthTime();
        Assert.assertEquals(authTime, authTimeUpdated);
    }

    private IDToken retrieveIDToken(EventRepresentation loginEvent) {
        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        Assert.assertEquals(200, response.getStatusCode());
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        events.expectCodeToToken(codeId, sessionId).assertEvent();
        return idToken;
    }

}
