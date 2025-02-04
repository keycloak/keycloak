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

import java.util.List;

import org.apache.http.HttpStatus;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.TokenVerifier;
import org.keycloak.common.Profile;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.OAuthClient;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

/**
 * Test for various combinations of "audience" and "scope" parameters for internal-internal token-exchange
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@EnableFeature(value = Profile.Feature.TOKEN_EXCHANGE_STANDARD_V2, skipRestart = true)
@EnableFeature(value = Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ, skipRestart = true) // TODO: Remove as we may not need to use FGAP for token exchange
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClientTokenExchangeAudienceAndScopesTest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);
    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealm = loadJson(getClass().getResourceAsStream("/token-exchange/testrealm-token-exchange-v2.json"), RealmRepresentation.class);
        testRealms.add(testRealm);
    }

    @Test
    public void test01_scopeParamIncludedWithoutAudience() throws Exception {
        String accessToken = resourceOwnerLogin();
        oauth.scope("optional-scope2");
        AccessTokenResponse response = oauth.doTokenExchange(TEST, accessToken, (String) null, "requester-client", "secret", null);
        assertAudiencesAndScopes(response, List.of("target-client1", "target-client2"), List.of("default-scope1", "optional-scope2"));
    }

    @Test
    public void test02_scopeParamIncludedAudienceIncluded() throws Exception {
        String accessToken = resourceOwnerLogin();
        oauth.scope("optional-scope2");
        AccessTokenResponse response = oauth.doTokenExchange(TEST, accessToken, List.of("target-client1"), "requester-client", "secret", null);
        assertAudiencesAndScopes(response, List.of("target-client1"), List.of("default-scope1", "optional-scope2"));
    }


    @Test
    public void test03_scopeParamIncludedAudienceIncluded_unavailableAudience() throws Exception {
        String accessToken = resourceOwnerLogin();
        oauth.scope("optional-scope2");

        // The "target-client3" is valid client, but unavailable to the user. Request allowed, but "target-client3" audience will not be available
        AccessTokenResponse response = oauth.doTokenExchange(TEST, accessToken, List.of("target-client1", "target-client3"), "requester-client", "secret", null);
        assertAudiencesAndScopes(response, List.of("target-client1"), List.of("default-scope1", "optional-scope2"));
    }


    private String resourceOwnerLogin() throws Exception {
        oauth.realm(TEST);
        oauth.clientId("requester-client");
        oauth.scope(null);
        oauth.openid(false);
        AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "john", "password");
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(response.getAccessToken(), AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        assertAudiences(token, List.of("target-client1"));
        assertScopes(token, List.of("default-scope1"));
        return response.getAccessToken();
    }

    private void assertAudiences(AccessToken token, List<String> expectedAudiences) {
        MatcherAssert.assertThat("Incompatible audiences", List.of(token.getAudience()), containsInAnyOrder(expectedAudiences.toArray()));
        MatcherAssert.assertThat("Incompatible resource access", token.getResourceAccess().keySet(), containsInAnyOrder(expectedAudiences.toArray()));
    }

    private void assertScopes(AccessToken token, List<String> expectedScopes) {
        MatcherAssert.assertThat("Incompatible scopes", List.of(token.getScope().split(" ")), containsInAnyOrder(expectedScopes.toArray()));
    }

    private void assertAudiencesAndScopes(AccessTokenResponse tokenExchangeResponse, List<String> expectedAudiences, List<String> expectedScopes) throws Exception {
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(tokenExchangeResponse.getAccessToken(), AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        if (expectedAudiences == null) {
            Assert.assertNull("Expected token to not contain audience", token.getAudience());
        } else {
            assertAudiences(token, expectedAudiences);
        }
        assertScopes(token, expectedScopes);
    }

}
