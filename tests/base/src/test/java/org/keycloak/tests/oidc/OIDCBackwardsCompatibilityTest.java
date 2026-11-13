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

import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.tests.common.BasicRealmWithUserConfig;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@KeycloakIntegrationTest
public class OIDCBackwardsCompatibilityTest extends AbstractOIDCScopeTest {

    @InjectRealm(config = BasicRealmWithUserConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    // KEYCLOAK-6286
    @Test
    public void testExcludeSessionStateParameter() {
        // Open login form and login successfully. Assert session_state is present
        AuthorizationEndpointResponse authzResponse = oauth.doLogin(BasicRealmWithUserConfig.USERNAME, BasicRealmWithUserConfig.PASSWORD);
        EventAssertion.expectLoginSuccess(events.poll());
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        Assertions.assertNotNull(authzResponse.getSessionState());

        // Switch "exclude session_state" to on
        ClientResource client = oauth.clientResource();
        ClientRepresentation clientRep = client.toRepresentation();
        OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep);
        config.setExcludeSessionStateFromAuthResponse(true);
        client.update(clientRep);

        // Open login again and assert session_state not present
        oauth.openLoginForm();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        EventAssertion.expectLoginSuccess(events.poll()).details(Details.USERNAME, BasicRealmWithUserConfig.USERNAME);

        authzResponse = oauth.parseLoginResponse();
        Assertions.assertNull(authzResponse.getSessionState());
    }

    @Test
    public void testExcludeIssuerParameter() {
        // Open login form and login successfully. Assert iss parameter is present
        AuthorizationEndpointResponse authzResponse = oauth.doLogin(BasicRealmWithUserConfig.USERNAME, BasicRealmWithUserConfig.PASSWORD);
        EventAssertion.expectLoginSuccess(events.poll());
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        Assertions.assertEquals(managedRealm.getBaseUrl(), authzResponse.getIssuer());

        // Switch "exclude iss" to on
        ClientResource client = oauth.clientResource();
        ClientRepresentation clientRep = client.toRepresentation();
        OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep);
        config.setExcludeIssuerFromAuthResponse(true);
        client.update(clientRep);

        // Open login again and assert iss parameter is not present
        oauth.openLoginForm();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        EventAssertion.expectLoginSuccess(events.poll()).details(Details.USERNAME, BasicRealmWithUserConfig.USERNAME);

        authzResponse = oauth.parseLoginResponse();
        Assertions.assertNull(authzResponse.getIssuer());
    }

    @Test
    public void testExcludeIssuerParameterOnError() {
        // Open login form and login fails. Assert iss parameter is present
        oauth.responseType("tokenn");
        oauth.openLoginForm();

        AuthorizationEndpointResponse errorResponse = oauth.parseLoginResponse();
        Assertions.assertTrue(errorResponse.isRedirected());
        Assertions.assertEquals(OAuthErrorException.UNSUPPORTED_RESPONSE_TYPE, errorResponse.getError());
        Assertions.assertEquals(managedRealm.getBaseUrl(), errorResponse.getIssuer());

        EventAssertion.assertError(events.poll()).type(EventType.LOGIN_ERROR).error(Errors.INVALID_REQUEST).userId(null).sessionId(null).details(Details.RESPONSE_TYPE, "tokenn");

        // Switch "exclude iss" to on
        ClientResource client = oauth.clientResource();
        ClientRepresentation clientRep = client.toRepresentation();
        OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep);
        config.setExcludeIssuerFromAuthResponse(true);
        client.update(clientRep);

        // Open login again and assert iss parameter is not present
        oauth.openLoginForm();

        errorResponse = oauth.parseLoginResponse();
        Assertions.assertTrue(errorResponse.isRedirected());
        Assertions.assertEquals(OAuthErrorException.UNSUPPORTED_RESPONSE_TYPE, errorResponse.getError());
        Assertions.assertNull(errorResponse.getIssuer());

        EventAssertion.assertError(events.poll()).type(EventType.LOGIN_ERROR).error(Errors.INVALID_REQUEST).userId(null).sessionId(null).details(Details.RESPONSE_TYPE, "tokenn");
    }
}
