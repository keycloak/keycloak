package org.keycloak.testframework.injection;

import java.util.Map;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.testframework.server.KeycloakServer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ValueTypeAliasTest {

    @Test
    public void withAlias() {
        ValueTypeAlias valueTypeAlias = new ValueTypeAlias();
        valueTypeAlias.addAll(Map.of(KeycloakServer.class, "server"));
        Assertions.assertEquals("server", valueTypeAlias.getAlias(KeycloakServer.class));
    }

    @Test
    public void withoutAlias() {
        Assertions.assertEquals("Keycloak", new ValueTypeAlias().getAlias(Keycloak.class));
    }

}
