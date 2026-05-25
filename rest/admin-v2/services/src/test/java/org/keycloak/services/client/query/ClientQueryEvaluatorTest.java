package org.keycloak.services.client.query;

import java.util.Set;

import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.admin.v2.SAMLClientRepresentation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientQueryEvaluatorTest {

    @Test
    void matchStringEquality() {
        var client = createClient("my-app", true);
        assertTrue(matches("clientId eq \"my-app\"", client));
        assertFalse(matches("clientId eq \"other\"", client));
    }

    @Test
    void matchQuotedStringValue() {
        var client = createClient("test", true);
        client.setDisplayName("My OAuth App");
        assertTrue(matches("displayName eq \"My OAuth App\"", client));
        assertFalse(matches("displayName eq \"Other Name\"", client));
    }

    @Test
    void matchBoolean() {
        var client = createClient("test", true);
        assertTrue(matches("enabled eq true", client));
        assertFalse(matches("enabled eq false", client));
    }

    @Test
    void matchAndExpression() {
        var client = createClient("my-app", true);
        assertTrue(matches("clientId eq \"my-app\" and enabled eq true", client));
        assertFalse(matches("clientId eq \"my-app\" and enabled eq false", client));
    }

    @Test
    void matchOrExpression() {
        var client = createClient("my-app", true);
        assertTrue(matches("clientId eq \"my-app\" or clientId eq \"other\"", client));
        assertTrue(matches("clientId eq \"other\" or clientId eq \"my-app\"", client));
        assertFalse(matches("clientId eq \"other\" or clientId eq \"another\"", client));
    }

    @Test
    void matchNotExpression() {
        var client = createClient("test", true);
        assertTrue(matches("not enabled eq false", client));
        assertFalse(matches("not enabled eq true", client));
    }

    @Test
    void matchPresenceOperator() {
        var client = createClient("test", true);
        client.setDescription("has a description");
        assertTrue(matches("description pr", client));

        var client2 = createClient("test2", true);
        client2.setDescription(null);
        assertFalse(matches("description pr", client2));
    }

    @Test
    void matchParenthesizedGrouping() {
        var client = createClient("my-app", true);
        assertTrue(matches("(clientId eq \"my-app\" or clientId eq \"other\") and enabled eq true", client));
        assertFalse(matches("(clientId eq \"other\" or clientId eq \"another\") and enabled eq true", client));
    }

    @Test
    void matchDotNotation() {
        var client = createClient("test", true);
        var auth = new OIDCClientRepresentation.Auth();
        auth.setMethod("client-secret");
        client.setAuth(auth);
        assertTrue(matches("auth.method eq \"client-secret\"", client));
        assertFalse(matches("auth.method eq \"client-jwt\"", client));
    }

    @Test
    void nullFieldNeverMatches() {
        var client = createClient("test", true);
        client.setDescription(null);
        assertFalse(matches("description eq \"anything\"", client));
    }

    @Test
    void matchCollectionMembership() {
        var client = createClient("test", true);
        client.setRoles(Set.of("admin", "user", "viewer"));
        assertTrue(matches("roles eq \"admin\"", client));
        assertFalse(matches("roles eq \"superadmin\"", client));
    }

    @Test
    void matchCollectionWithAnd() {
        var client = createClient("test", true);
        client.setRoles(Set.of("admin", "user", "viewer"));
        assertTrue(matches("roles eq \"admin\" and roles eq \"user\"", client));
        assertFalse(matches("roles eq \"admin\" and roles eq \"superadmin\"", client));
    }

    @Test
    void matchLoginFlows() {
        var client = createClient("test", true);
        client.setLoginFlows(Set.of(
                OIDCClientRepresentation.Flow.STANDARD,
                OIDCClientRepresentation.Flow.DIRECT_GRANT));
        assertTrue(matches("loginFlows eq \"STANDARD\"", client));
        assertTrue(matches("loginFlows eq \"STANDARD\" and loginFlows eq \"DIRECT_GRANT\"", client));
        assertFalse(matches("loginFlows eq \"SERVICE_ACCOUNT\"", client));
    }

    @Test
    void emptyCollectionNeverMatches() {
        var client = createClient("test", true);
        client.setRoles(Set.of());
        assertFalse(matches("roles eq \"admin\"", client));
    }

    @Test
    void emptyStringMatchesEmptyField() {
        var client = createClient("test", true);
        client.setDescription("");
        assertTrue(matches("description eq \"\"", client));
    }

    @Test
    void caseSensitiveMatch() {
        var client = createClient("test", true);
        client.setDisplayName("MyApp");
        assertTrue(matches("displayName eq \"MyApp\"", client));
        assertFalse(matches("displayName eq \"myapp\"", client));
        assertFalse(matches("displayName eq \"MYAPP\"", client));
    }

    @Test
    void jsonEscapedString() {
        var client = createClient("test", true);
        client.setDescription("line1\nline2");
        assertTrue(matches("description eq \"line1\\nline2\"", client));
    }

    @Test
    void matchNullValue() {
        var client = createClient("test", true);
        client.setDescription(null);
        assertTrue(matches("not description pr", client));
    }

    @Test
    void matchNotEquals() {
        var client = createClient("my-app", true);
        assertTrue(matches("clientId ne \"other\"", client));
        assertFalse(matches("clientId ne \"my-app\"", client));
    }

    @Test
    void matchContains() {
        var client = createClient("test", true);
        client.setDescription("hello world");
        assertTrue(matches("description co \"world\"", client));
        assertTrue(matches("description co \"hello\"", client));
        assertFalse(matches("description co \"missing\"", client));
    }

    @Test
    void matchStartsWith() {
        var client = createClient("test", true);
        client.setDescription("hello world");
        assertTrue(matches("description sw \"hello\"", client));
        assertFalse(matches("description sw \"world\"", client));
    }

    @Test
    void matchEndsWith() {
        var client = createClient("test", true);
        client.setDescription("hello world");
        assertTrue(matches("description ew \"world\"", client));
        assertFalse(matches("description ew \"hello\"", client));
    }

    @Test
    void containsOnCollection() {
        var client = createClient("test", true);
        client.setRoles(Set.of("admin-role", "user-role"));
        assertTrue(matches("roles co \"admin\"", client));
        assertFalse(matches("roles co \"super\"", client));
    }

    @Test
    void oidcFieldAgainstSamlClientReturnsFalse() {
        var client = new SAMLClientRepresentation();
        client.setClientId("saml-client");
        client.setEnabled(true);
        var filterCtx = QueryParseUtils.parse("auth.method eq \"client-secret\"");
        assertFalse(ClientQueryEvaluator.matches(filterCtx, client));
    }

    @Test
    void baseFieldMatchesSamlClient() {
        var client = new SAMLClientRepresentation();
        client.setClientId("saml-client");
        client.setEnabled(true);
        var filterCtx = QueryParseUtils.parse("enabled eq true");
        assertTrue(ClientQueryEvaluator.matches(filterCtx, client));
    }

    @Test
    void andBindsTighterThanOr() {
        var client = createClient("test", true);
        client.setDescription("hello");
        // false or (true and true) == true
        assertTrue(matches("enabled eq false or description eq \"hello\" and enabled eq true", client));
        // false or (false and true) == false
        assertFalse(matches("description eq \"nope\" or enabled eq false and description eq \"hello\"", client));
    }

    @Test
    void eqNullMatchesNullField() {
        var client = createClient("test", true);
        client.setDescription(null);
        assertTrue(matches("description eq null", client));

        client.setDescription("something");
        assertFalse(matches("description eq null", client));
    }

    @Test
    void neAndNotEqConsistentOnNullField() {
        var client = createClient("test", true);
        client.setDescription(null);
        assertTrue(matches("not description eq \"something\"", client));
        assertTrue(matches("description ne \"something\"", client));
    }

    @Test
    void notWithParenthesizedComparison() {
        var client = createClient("test", true);
        client.setDescription("hello");
        assertTrue(matches("not (description eq \"world\")", client));
        assertFalse(matches("not (description eq \"hello\")", client));
    }

    private static boolean matches(String query, OIDCClientRepresentation client) {
        var filterCtx = QueryParseUtils.parse(query);
        return ClientQueryEvaluator.matches(filterCtx, client);
    }

    private static OIDCClientRepresentation createClient(String clientId, boolean enabled) {
        var client = new OIDCClientRepresentation(clientId);
        client.setEnabled(enabled);
        return client;
    }
}
