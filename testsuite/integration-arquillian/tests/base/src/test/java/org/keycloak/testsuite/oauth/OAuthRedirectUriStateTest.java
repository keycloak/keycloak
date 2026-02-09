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

package org.keycloak.testsuite.oauth;

import java.net.MalformedURLException;
import java.net.URL;

import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.junit.Before;
import org.junit.Test;

public class OAuthRedirectUriStateTest extends AbstractTestRealmKeycloakTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Before
    public void clientConfiguration() {
        oauth.clientId("test-app");
        oauth.responseType(OIDCResponseType.CODE);
    }

    void assertStateReflected(String state) {
        AuthorizationEndpointResponse response = oauth.loginForm().state(state).doLogin("test-user@localhost", "password");
        Assert.assertNotNull(response.getCode());

        URL url;
        try {
            url = new URL(driver.getCurrentUrl());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        Assert.assertTrue(url.getQuery().contains("state=" + state));
    }

    @Test
    public void testSimpleStateParameter() {
        assertStateReflected("VeryLittleGravitasIndeed");
    }

    @Test
    public void testJsonStateParameter() {
        assertStateReflected("%7B%22csrf_token%22%3A%2B%22hlvZNIsWyqdkEhbjlQIia0ty2YY4TXat%22%2C%2B%22destination%22%3A%2B%22eyJhbGciOiJIUzI1NiJ9.Imh0dHA6Ly9sb2NhbGhvc3Q6NTAwMC9wcml2YXRlIg.T18WeIV29komDl8jav-3bSnUZDlMD8VOfIrd2ikP5zE%22%7D");
    }
}
