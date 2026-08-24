package org.keycloak.tests.oauth.tokenexchange;

import java.util.Arrays;
import java.util.Map;

import org.keycloak.representations.AccessToken;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;

import static org.keycloak.OAuth2Constants.CLIENT_ID;
import static org.keycloak.protocol.oidc.OIDCLoginProtocol.ISSUER;
import static org.keycloak.representations.IDToken.ACT;
import static org.keycloak.representations.IDToken.MAY_ACT;
import static org.keycloak.representations.IDToken.PREFERRED_USERNAME;
import static org.keycloak.representations.JsonWebToken.SUBJECT;

final class DelegationAssertions {

    static void assertMayActNotPresent(AccessToken token) {
        Assertions.assertNull(token.getOtherClaims().get(MAY_ACT), "may_act claim should not be present");
    }

    static void assertMayActPresent(AccessToken token, String expectedActorId) {
        assertClaimPresent(token, MAY_ACT, expectedActorId);
    }

    static void assertMayActPresent(AccessToken token, String expectedActorId, String expectedClientId) {
        Map<String, Object> mayAct = assertClaimPresent(token, MAY_ACT, expectedActorId);
        Assertions.assertEquals(expectedClientId, mayAct.get(CLIENT_ID), "may_act.client_id should contain the client ID");
    }

    static void assertMayActPresent(AccessToken token, String expectedActorId, String expectedIss, String expectedUsername) {
        Map<String, Object> mayAct = assertClaimPresent(token, MAY_ACT, expectedActorId);
        assertOptionalClaim(mayAct, ISSUER, expectedIss, "may_act.iss");
        assertOptionalClaim(mayAct, PREFERRED_USERNAME, expectedUsername, "may_act.preferred_username");
    }

    static void assertActPresent(AccessToken token, String expectedActorId) {
        assertClaimPresent(token, ACT, expectedActorId);
    }

    static void assertActPresent(AccessToken token, String expectedActorId, String expectedClientId) {
        Map<String, Object> act = assertClaimPresent(token, ACT, expectedActorId);
        Assertions.assertEquals(expectedClientId, act.get(CLIENT_ID), "act.client_id should contain the client ID");
    }

    static void assertActPresent(AccessToken token, String expectedActorId, String expectedIss, String expectedUsername) {
        Map<String, Object> act = assertClaimPresent(token, ACT, expectedActorId);
        assertOptionalClaim(act, ISSUER, expectedIss, "act.iss");
        assertOptionalClaim(act, PREFERRED_USERNAME, expectedUsername, "act.preferred_username");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> assertClaimPresent(AccessToken token, String claimName, String expectedActorId) {
        Map<String, Object> claim = (Map<String, Object>) token.getOtherClaims().get(claimName);
        Assertions.assertNotNull(claim, claimName + " claim should be present");
        Assertions.assertEquals(expectedActorId, claim.get(SUBJECT), claimName + ".sub should contain the actor user ID");
        return claim;
    }

    private static void assertOptionalClaim(Map<String, Object> claims, String claimName, String expected, String label) {
        if (expected != null) {
            Assertions.assertEquals(expected, claims.get(claimName), label + " is not correct");
        } else {
            Assertions.assertNull(claims.get(claimName), label + " is not null");
        }
    }

    static void assertScopeContains(String scopeString, String expectedScope) {
        Assertions.assertNotNull(scopeString, "Scope string should not be null");
        MatcherAssert.assertThat(Arrays.asList(scopeString.split(" ")), Matchers.hasItem(expectedScope));
    }

    static void assertScopeNotContains(String scopeString, String expectedScope) {
        Assertions.assertNotNull(scopeString, "Scope string should not be null");
        MatcherAssert.assertThat(Arrays.asList(scopeString.split(" ")), Matchers.not(Matchers.hasItem(expectedScope)));
    }
}
