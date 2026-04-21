package org.keycloak.services.client.query;

import java.util.Set;

import org.keycloak.representations.admin.v2.OIDCClientRepresentation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldResolverTest {

    @Test
    void resolveClientId() {
        var client = new OIDCClientRepresentation("my-app");
        assertEquals("my-app", FieldResolver.resolve("clientId", client));
    }

    @Test
    void resolveEnabled() {
        var client = new OIDCClientRepresentation("test");
        client.setEnabled(true);
        assertEquals(true, FieldResolver.resolve("enabled", client));
    }

    @Test
    void resolveProtocol() {
        var client = new OIDCClientRepresentation("test");
        assertEquals("openid-connect", FieldResolver.resolve("protocol", client));
    }

    @Test
    void resolveRedirectUris() {
        var client = new OIDCClientRepresentation("test");
        client.setRedirectUris(Set.of("https://example.com"));
        var result = FieldResolver.resolve("redirectUris", client);
        assertTrue(result instanceof Set);
    }

    @Test
    void resolveAuthMethod() {
        var client = new OIDCClientRepresentation("test");
        var auth = new OIDCClientRepresentation.Auth();
        auth.setMethod("client-secret");
        client.setAuth(auth);
        assertEquals("client-secret", FieldResolver.resolve("auth.method", client));
    }

    @Test
    void resolveAuthMethodNullAuth() {
        var client = new OIDCClientRepresentation("test");
        assertNull(FieldResolver.resolve("auth.method", client));
    }

    @Test
    void resolveServiceAccountRoles() {
        var client = new OIDCClientRepresentation("test");
        client.setServiceAccountRoles(Set.of("uma_protection"));
        var result = FieldResolver.resolve("serviceAccountRoles", client);
        assertTrue(result instanceof Set);
        assertEquals(Set.of("uma_protection"), result);
    }

    @Test
    void unknownFieldThrows() {
        var client = new OIDCClientRepresentation("test");
        assertThrows(ClientQueryException.class, () -> FieldResolver.resolve("unknownField", client));
    }
}
