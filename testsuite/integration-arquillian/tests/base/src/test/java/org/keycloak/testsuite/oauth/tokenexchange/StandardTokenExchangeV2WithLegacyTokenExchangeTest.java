/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.testsuite.oauth.tokenexchange;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuthErrorException;
import org.keycloak.common.Profile;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.arquillian.annotation.UncaughtServerErrorExpected;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.Test;

import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

/**
 * Test with both V1 and V2 token-exchange enabled. Requests should be handled by V2
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@EnableFeature(value = Profile.Feature.TOKEN_EXCHANGE, skipRestart = true)
@EnableFeature(value = Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ, skipRestart = true)
@DisableFeature(value = Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ_V2, skipRestart = true)
public class StandardTokenExchangeV2WithLegacyTokenExchangeTest extends StandardTokenExchangeV2Test {

    @Test
    @UncaughtServerErrorExpected
    @Override
    public void testExchangeDisabledOnClient() throws Exception {
        // When client does not have TE enabled, request is handled by V1-provider, which returns different error
        oauth.realm(TEST);
        String accessToken = resourceOwnerLogin("john", "password", "subject-client", "secret").getAccessToken();
        {
            AccessTokenResponse response = tokenExchange(accessToken, "disabled-requester-client", "secret", null, null);
            org.junit.Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatusCode());
            org.junit.Assert.assertEquals(OAuthErrorException.ACCESS_DENIED, response.getError());
            org.junit.Assert.assertEquals("Client is not within the token audience", response.getErrorDescription());
        }
    }
}
