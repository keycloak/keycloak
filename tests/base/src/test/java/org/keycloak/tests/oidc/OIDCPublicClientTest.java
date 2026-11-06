/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.tests.oidc;

import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.events.Details;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.tests.common.BasicRealmWithUserConfig;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.TokenUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@KeycloakIntegrationTest
public class OIDCPublicClientTest {

    @InjectRealm(config = BasicRealmWithUserConfig.class)
    ManagedRealm managedRealm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectEvents
    Events events;

    // KEYCLOAK-18258
    @Test
    public void accessTokenRequest() {
        // Update client to use custom client authenticator
        managedRealm.updateClientWithCleanup(oauth.getClientId(), c -> c.authenticatorType(JWTClientAuthenticator.PROVIDER_ID));

        // Switch client to public client now
        Assertions.assertEquals(JWTClientAuthenticator.PROVIDER_ID, oauth.clientResource().toRepresentation().getClientAuthenticatorType());
        managedRealm.updateClientWithCleanup(oauth.getClientId(), c -> c.publicClient());

        // It should be possible to authenticate
        oauth.doLogin(BasicRealmWithUserConfig.USERNAME, BasicRealmWithUserConfig.PASSWORD);

        EventRepresentation loginEvent = EventAssertion.expectLoginSuccess(events.poll()).getEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);

        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getAccessToken());
        EventAssertion.expectCodeToTokenSuccess(events.poll())
                .sessionId(sessionId)
                .details(Details.CODE_ID, codeId)
                .details(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_REFRESH)
                .details(Details.CLIENT_AUTH_METHOD, ClientIdAndSecretAuthenticator.PROVIDER_ID);
    }

}
