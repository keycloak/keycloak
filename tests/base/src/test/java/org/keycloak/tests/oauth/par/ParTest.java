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
package org.keycloak.tests.oauth.par;


import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.tests.client.policies.AbstractClientPoliciesTest;
import org.keycloak.tests.common.TestRealmUserConfig;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.ParResponse;

import org.junit.jupiter.api.Test;

import static org.keycloak.models.ParConfig.DEFAULT_PAR_REQUEST_URI_LIFESPAN;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for Pushed Authorization Requests (PAR) security properties.
 */
@KeycloakIntegrationTest
public class ParTest extends AbstractClientPoliciesTest {

    @InjectUser(config = TestRealmUserConfig.class)
    protected ManagedUser user;

    @InjectTimeOffSet(enableForCaches = true)
    TimeOffSet timeOffSet;

    @InjectOAuthClient(lifecycle = LifeCycle.METHOD)
    OAuthClient oauth;

    @InjectPage
    LoginPage loginPage;

    // PAR object needs to be valid for the time of PAR lifespan together with the authenticationSession time as
    // (See https://github.com/keycloak/keycloak/issues/48072 for the details)
    @Test
    public void requestUriLifetimeDoesNotLimitAuthenticationSessionLength() {
        String origRedirectUri = oauth.getRedirectUri();

        // Pushed Authorization Request
        ParResponse pResp = oauth.doPushedAuthorizationRequest();
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();
        assertEquals(DEFAULT_PAR_REQUEST_URI_LIFESPAN, pResp.getExpiresIn()); // This time is still just requestUri lifespan (60 seconds). Attempt to use PAR after longer time tested elsewhere (in testFailureParExpired())

        // Authorization Request with request_uri of PAR
        // remove parameters as query strings of uri
        oauth.redirectUri(null);
        oauth.scope(null);
        oauth.responseType(null);
        String state = "testSuccessfulSinglePar";
        oauth.loginForm().requestUri(requestUri).state(state).open();
        loginPage.assertCurrent();

        // make sure interactive login waits longer than requestUriLifespan
        timeOffSet.set(DEFAULT_PAR_REQUEST_URI_LIFESPAN * 2);

        oauth.fillLoginForm("test-user@localhost", "password");
        AuthorizationEndpointResponse loginResponse = oauth.parseLoginResponse();
        assertEquals(state, loginResponse.getState());
        String code = loginResponse.getCode();

        // For this test it's enough to check that Code2Token succeeds
        oauth.redirectUri(origRedirectUri); // get tokens, it needed. https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.3
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);
        assertEquals(200, res.getStatusCode());
    }

}
