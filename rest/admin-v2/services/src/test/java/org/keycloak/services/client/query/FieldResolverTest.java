package org.keycloak.services.client.query;

import java.util.Set;

import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.services.client.scim.ClientResourceTypeProvider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldResolverTest {

    @Test
    void resolveClientId() {
        var client = new OIDCClientRepresentation("my-app");
        assertEquals("my-app", ClientResourceTypeProvider.resolveField("clientId", client));
    }

    @Test
    void resolveEnabled() {
        var client = new OIDCClientRepresentation("test");
        client.setEnabled(true);
        assertEquals(true, ClientResourceTypeProvider.resolveField("enabled", client));
    }

    @Test
    void resolveProtocol() {
        var client = new OIDCClientRepresentation("test");
        assertEquals("openid-connect", ClientResourceTypeProvider.resolveField("protocol", client));
    }

    @Test
    void resolveRedirectUris() {
        var client = new OIDCClientRepresentation("test");
        client.setRedirectUris(Set.of("https://example.com"));
        var result = ClientResourceTypeProvider.resolveField("redirectUris", client);
        assertTrue(result instanceof Set);
    }

    @Test
    void resolveServiceAccountRoles() {
        var client = new OIDCClientRepresentation("test");
        client.setServiceAccountRoles(Set.of("uma_protection"));
        var result = ClientResourceTypeProvider.resolveField("serviceAccountRoles", client);
        assertTrue(result instanceof Set);
        assertEquals(Set.of("uma_protection"), result);
    }

}
