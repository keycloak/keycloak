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

import java.util.Arrays;
import java.util.Collection;

import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.events.Details;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginTotpPage;
import org.keycloak.testframework.ui.page.OAuthGrantPage;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
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
