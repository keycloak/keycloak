package org.keycloak.services.client.query;

import java.util.Set;

import org.keycloak.representations.admin.v2.OIDCClientRepresentation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientQueryEvaluatorTest {

    @Test
    void matchBareValue() {
        var client = createClient("my-app", true);
        assertTrue(matches("clientId:my-app", client));
        assertFalse(matches("clientId:other", client));
    }

    @Test
    void matchQuotedValue() {
        var client = createClient("test", true);
        client.setDisplayName("My OAuth App");
        assertTrue(matches("displayName:\"My OAuth App\"", client));
        assertFalse(matches("displayName:\"Other Name\"", client));
    }

    @Test
    void matchBoolean() {
        var client = createClient("test", true);
        assertTrue(matches("enabled:true", client));
        assertFalse(matches("enabled:false", client));
    }

    @Test
    void matchProtocol() {
        var client = createClient("test", true);
        assertTrue(matches("protocol:openid-connect", client));
        assertFalse(matches("protocol:saml", client));
    }

    @Test
    void matchMultipleExpressions() {
        var client = createClient("my-app", true);
        assertTrue(matches("clientId:my-app enabled:true", client));
        assertFalse(matches("clientId:my-app enabled:false", client));
    }

    @Test
    void matchDotNotation() {
        var client = createClient("test", true);
        var auth = new OIDCClientRepresentation.Auth();
        auth.setMethod("client-secret");
        client.setAuth(auth);
        assertTrue(matches("auth.method:client-secret", client));
        assertFalse(matches("auth.method:client-jwt", client));
    }

    @Test
    void nullFieldNeverMatches() {
        var client = createClient("test", true);
        client.setDescription(null);
        assertFalse(matches("description:anything", client));
    }

    @Test
    void matchListSubset() {
        var client = createClient("test", true);
        client.setRoles(Set.of("admin", "user", "viewer"));
        assertTrue(matches("roles:[admin,user]", client));
        assertTrue(matches("roles:[admin]", client));
        assertFalse(matches("roles:[admin,superadmin]", client));
    }

    @Test
    void matchLoginFlows() {
        var client = createClient("test", true);
        client.setLoginFlows(Set.of(
                OIDCClientRepresentation.Flow.STANDARD,
                OIDCClientRepresentation.Flow.DIRECT_GRANT));
        assertTrue(matches("loginFlows:[STANDARD]", client));
        assertTrue(matches("loginFlows:[STANDARD,DIRECT_GRANT]", client));
        assertFalse(matches("loginFlows:[SERVICE_ACCOUNT]", client));
    }

    @Test
    void scalarMatchOnCollection() {
        var client = createClient("test", true);
        client.setRoles(Set.of("admin", "user"));
        assertTrue(matches("roles:admin", client));
        assertFalse(matches("roles:superadmin", client));
    }

    @Test
    void unknownFieldThrows() {
        var client = createClient("test", true);
        var queryCtx = QueryParseUtils.parse("unknownField:value");
        assertThrows(ClientQueryException.class, () ->
                ClientQueryEvaluator.matches(queryCtx, client));
    }

    @Test
    void mixedListEntriesThrows() {
        var client = createClient("test", true);
        client.setRoles(Set.of("admin", "user"));
        assertThrows(ClientQueryException.class, () -> matches("roles:[admin,key:value]", client));
    }

    @Test
    void emptyCollectionNeverMatchesList() {
        var client = createClient("test", true);
        client.setRoles(Set.of());
        assertFalse(matches("roles:[admin]", client));
    }

    @Test
    void emptyCollectionNeverMatchesScalar() {
        var client = createClient("test", true);
        client.setRoles(Set.of());
        assertFalse(matches("roles:admin", client));
    }

    @Test
    void emptyQuotedStringMatchesEmptyField() {
        var client = createClient("test", true);
        client.setDescription("");
        assertTrue(matches("description:\"\"", client));
    }

    @Test
    void emptyQuotedStringDoesNotMatchNonEmpty() {
        var client = createClient("test", true);
        client.setDescription("something");
        assertFalse(matches("description:\"\"", client));
    }

    @Test
    void duplicateFieldLastWins() {
        var client = createClient("test", true);
        client.setEnabled(true);
        // both expressions must match (implicit AND), so contradictory values = no match
        assertFalse(matches("enabled:true enabled:false", client));
    }

    @Test
    void caseSensitiveMatch() {
        var client = createClient("test", true);
        client.setDisplayName("MyApp");
        assertTrue(matches("displayName:MyApp", client));
        assertFalse(matches("displayName:myapp", client));
        assertFalse(matches("displayName:MYAPP", client));
    }

    @Test
    void enabledNullNeverMatches() {
        var client = createClient("test", true);
        client.setEnabled(null);
        assertFalse(matches("enabled:true", client));
        assertFalse(matches("enabled:false", client));
    }

    @Test
    void listWithUrlLikeEntries() {
        var client = createClient("test", true);
        client.setRedirectUris(Set.of("https://example.com/callback", "https://other.com"));
        assertTrue(matches("redirectUris:[https://example.com/callback]", client));
        assertFalse(matches("redirectUris:[https://missing.com]", client));
    }

    @Test
    void quotedValueWithSpecialChars() {
        var client = createClient("test", true);
        client.setDescription("value with: colons and [brackets]");
        assertTrue(matches("description:\"value with: colons and [brackets]\"", client));
    }

    private static boolean matches(String query, OIDCClientRepresentation client) {
        var queryCtx = QueryParseUtils.parse(query);
        return ClientQueryEvaluator.matches(queryCtx, client);
    }

    private static OIDCClientRepresentation createClient(String clientId, boolean enabled) {
        var client = new OIDCClientRepresentation(clientId);
        client.setEnabled(enabled);
        return client;
    }
}
