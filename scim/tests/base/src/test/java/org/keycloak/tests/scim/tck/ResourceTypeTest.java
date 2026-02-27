package org.keycloak.tests.scim.tck;

import java.util.List;

import org.keycloak.scim.protocol.response.ListResponse;
import org.keycloak.scim.resource.ResourceTypeRepresentation;
import org.keycloak.scim.resource.group.Group;
import org.keycloak.scim.resource.resourcetype.ResourceType;
import org.keycloak.scim.resource.resourcetype.ResourceType.SchemaExtension;
import org.keycloak.scim.resource.user.User;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.junit.jupiter.api.Test;

import static org.keycloak.scim.resource.Scim.ENTERPRISE_USER_SCHEMA;
import static org.keycloak.scim.resource.Scim.getCoreSchema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = ScimServerConfig.class)
public class ResourceTypeTest extends AbstractScimTest {

    @Test
    public void testGet() {
        ListResponse<ResourceType> response = client.resourceTypes().getAll();

        assertNotNull(response);
        assertEquals(2, response.getResources().size());

        assertResourceType(response, User.class, List.of(ENTERPRISE_USER_SCHEMA));
        assertResourceType(response, Group.class, List.of());
    }

    private static void assertResourceType(ListResponse<ResourceType> response, Class<? extends ResourceTypeRepresentation> resourceType, List<String> expectedSchemaExtensions) {
        ResourceType representation = response.getResources().stream().filter(r -> r.getName().equals(resourceType.getSimpleName())).findAny().orElse(null);

        assertNotNull(representation);
        assertEquals("/" + representation.getName() + "s", representation.getEndpoint());
        assertEquals(getCoreSchema(resourceType), representation.getSchema());

        if (!expectedSchemaExtensions.isEmpty()) {
            assertEquals(expectedSchemaExtensions.size(), representation.getSchemaExtensions().size());
            assertTrue(representation.getSchemaExtensions().stream().map(SchemaExtension::getSchema).toList().containsAll(expectedSchemaExtensions));
        }
    }
}
