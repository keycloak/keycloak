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

import jakarta.ws.rs.core.Response;
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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

/**
 * Test for various combinations of "audience" and "scope" parameters for internal-internal token-exchange
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@EnableFeature(value = Profile.Feature.TOKEN_EXCHANGE_STANDARD_V2, skipRestart = true)
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
    public void testOptionalScopeParamRequestedWithoutAudience() throws Exception {
        String accessToken = resourceOwnerLogin("john", "password", List.of("target-client1"), List.of("default-scope1"));;
        oauth.scope("optional-scope2");
        AccessTokenResponse response = oauth.doTokenExchange(accessToken, (String) null, "requester-client", "secret", null);
        assertAudiencesAndScopes(response, List.of("target-client1", "target-client2"), List.of("default-scope1", "optional-scope2"));
    }

    @Test
    public void testAudienceRequested() throws Exception {
        String accessToken = resourceOwnerLogin("john", "password", List.of("target-client1"), List.of("default-scope1"));;
        AccessTokenResponse response = oauth.doTokenExchange(accessToken, List.of("target-client1"), "requester-client", "secret", null);
        assertAudiencesAndScopes(response, List.of("target-client1"), List.of("default-scope1"));
    }

    @Test
    public void testUnavailableAudienceRequested() throws Exception {
        String accessToken = resourceOwnerLogin("john", "password", List.of("target-client1"), List.of("default-scope1"));;
        // The "target-client3" is valid client, but unavailable to the user. Request allowed, but "target-client3" audience will not be available
        AccessTokenResponse response = oauth.doTokenExchange(accessToken, List.of("target-client1", "target-client3"), "requester-client", "secret", null);
        assertAudiencesAndScopes(response, List.of("target-client1"), List.of("default-scope1"));
    }

    @Test
    public void testScopeNotAllowed() throws Exception {
        String accessToken = resourceOwnerLogin("john", "password", List.of("target-client1"), List.of("default-scope1"));

        //scope not allowed
        oauth.scope("optional-scope3");
        AccessTokenResponse response = oauth.doTokenExchange(accessToken, List.of("target-client1", "target-client3"), "requester-client", "secret", null);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());

        //scope that doesn't exist
        oauth.scope("bad-scope");
        response = oauth.doTokenExchange(accessToken, List.of("target-client1", "target-client3"), "requester-client", "secret", null);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
    }

    @Test
    public void testScopeFilter() throws Exception {
        String accessToken = resourceOwnerLogin("john", "password", List.of("target-client1"), List.of("default-scope1"));
        AccessTokenResponse response = oauth.doTokenExchange(accessToken, List.of( "target-client2"), "requester-client", "secret", null);
        assertAudiencesAndScopes(response, List.of(), List.of());

        oauth.scope("optional-scope2");
        response = oauth.doTokenExchange(accessToken, List.of( "target-client2"), "requester-client", "secret", null);
        assertAudiencesAndScopes(response, List.of("target-client2"), List.of( "optional-scope2"));

        oauth.scope("optional-scope2");
        response = oauth.doTokenExchange(accessToken, List.of("target-client1", "target-client2"), "requester-client", "secret", null);
        assertAudiencesAndScopes(response, List.of("target-client1", "target-client2"), List.of("default-scope1", "optional-scope2"));

        //just check that the exchanged token contains the optional-scope2 mapped by the realm role
        accessToken = resourceOwnerLogin("mike", "password", List.of("target-client1"), List.of("default-scope1"));
        oauth.scope("optional-scope2");
        response = oauth.doTokenExchange(accessToken, List.of(), "requester-client", "secret", null);
        assertAudiencesAndScopes(response, List.of("target-client1"), List.of("default-scope1", "optional-scope2"));

        accessToken = resourceOwnerLogin("mike", "password", List.of("target-client1"), List.of("default-scope1"));
        oauth.scope("optional-scope2");
        response = oauth.doTokenExchange(accessToken, List.of("target-client1"), "requester-client", "secret", null);
        assertAudiencesAndScopes(response,  List.of("target-client1"), List.of("default-scope1", "optional-scope2"));
    }

    private String resourceOwnerLogin(String username, String password, List<String> audience, List<String> scope) throws Exception {
        oauth.realm(TEST);
        oauth.clientId("requester-client");
        oauth.scope(null);
        oauth.openid(false);
        AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", username, password);
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(response.getAccessToken(), AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        assertAudiences(token, audience);
        assertScopes(token, scope);
        return response.getAccessToken();
    }

    private void assertAudiences(AccessToken token, List<String> expectedAudiences) {
        MatcherAssert.assertThat("Incompatible audiences", token.getAudience() == null ? List.of() : List.of(token.getAudience()), containsInAnyOrder(expectedAudiences.toArray()));
        MatcherAssert.assertThat("Incompatible resource access", token.getResourceAccess().keySet(), containsInAnyOrder(expectedAudiences.toArray()));
    }

    private void assertScopes(AccessToken token, List<String> expectedScopes) {
        MatcherAssert.assertThat("Incompatible scopes", token.getScope().isEmpty() ? List.of() : List.of(token.getScope().split(" ")), containsInAnyOrder(expectedScopes.toArray()));
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
