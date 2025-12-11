package org.keycloak.tests.db;

import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.database.TestDatabase;
import org.keycloak.testframework.injection.Extensions;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.RoleConfigBuilder;

import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public abstract class AbstractDBSchemaTest {

    @InjectClient
    ManagedClient managedClient;

    protected static String dbType() {
        return Extensions.getInstance().findSupplierByType(TestDatabase.class).getAlias();
    }

    @Test
    public void testCaseSensitiveSchema() {
        RoleRepresentation role1 = RoleConfigBuilder.create()
                .name("role1")
                .description("role1-description")
                .singleAttribute("role1-attr-key", "role1-attr-val")
                .build();
        RolesResource roles = managedClient.admin().roles();
        roles.create(role1);
        roles.deleteRole(role1.getName());
    }
}
