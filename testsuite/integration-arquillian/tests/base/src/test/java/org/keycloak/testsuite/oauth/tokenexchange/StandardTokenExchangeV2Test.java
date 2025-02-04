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

import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.TokenVerifier;
import org.keycloak.common.Profile;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.arquillian.annotation.UncaughtServerErrorExpected;
import org.keycloak.testsuite.util.OAuthClient;

import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@EnableFeature(value = Profile.Feature.TOKEN_EXCHANGE_STANDARD_V2, skipRestart = true)
@EnableFeature(value = Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ, skipRestart = true) // TODO: Replace with admin-fine-grained-authz V2
public class StandardTokenExchangeV2Test extends AbstractStandardTokenExchangeTest {

    @Override
    protected void setupRealm() {
        testingClient.server().run(TokenExchangeTestUtils::setupRealm);
        testingClient.server().run(StandardTokenExchangeV2Test::setupRealmForStandardTokenExchangeV2);
    }

    public static void setupRealmForStandardTokenExchangeV2(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(TEST);

        // Add "target" as an audience, so it is available in the tokens
        ClientScopeModel basicClientScope = KeycloakModelUtils.getClientScopeByName(realm, "basic");
        ProtocolMapperModel mapper = AudienceProtocolMapper.createClaimMapper("target-audience-mapper", "target", null, true, false, true);
        basicClientScope.addProtocolMapper(mapper);

        // Add role scope mapping to the "requester" client instead of "target" client
        RoleModel exampleRole = session.roles().getRealmRole(realm, "example");
        ClientModel client = realm.getClientByClientId("client-exchanger");
        client.addScopeMapping(exampleRole);
        client = realm.getClientByClientId("legal");
        client.addScopeMapping(exampleRole);

        client = realm.getClientByClientId("target");
        client.deleteScopeMapping(exampleRole);
    }

    @Override
    protected String getInitialAccessTokenForClientExchanger() throws Exception {
        oauth.clientId("client-exchanger");
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "user", "password");
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertNotNull(token.getSessionId());
        Assert.assertEquals(token.getPreferredUsername(), "user");
        return accessToken;
    }

    // Scope parameter is different with V2. TODO: Should write differently this test for V2
    @Test
    @UncaughtServerErrorExpected
    @Ignore
    public void testExchangeDifferentScopes() throws Exception {

    }

    // Scope parameter is different with V2. TODO: Should write differently this test for V2
    @Test
    @UncaughtServerErrorExpected
    @Ignore
    public void testExchangeDifferentScopesWithScopeParameter() throws Exception {

    }

}
