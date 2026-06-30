package org.keycloak.representations.admin.v2.validators;

import org.keycloak.representations.admin.v2.OIDCClientRepresentation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ClientPersistedFieldResolverTest {

    private final ClientPersistedFieldResolver resolver = new ClientPersistedFieldResolver();

    @Test
    void getProvidedValueReadsDeclaredFields() {
        var client = new OIDCClientRepresentation("test-client");
        client.setUuid("client-uuid");
        client.setProtocol(OIDCClientRepresentation.PROTOCOL);
        client.setCreatedTimestamp(1_700_000_000_000L);

        assertEquals("client-uuid", resolver.getProvidedValue(client, "uuid"));
        assertEquals(OIDCClientRepresentation.PROTOCOL, resolver.getProvidedValue(client, "protocol"));
        assertEquals("1700000000000", resolver.getProvidedValue(client, "createdTimestamp"));
    }

    @Test
    void getProvidedValueReturnsNullForUnsetField() {
        var client = new OIDCClientRepresentation("test-client");

        assertNull(resolver.getProvidedValue(client, "uuid"));
    }
}
