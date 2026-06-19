package org.keycloak.tests.scim.tck;

import java.util.List;

import jakarta.ws.rs.core.Response.Status;

import org.keycloak.scim.client.ScimClientException;
import org.keycloak.scim.protocol.response.ListResponse;
import org.keycloak.scim.resource.ResourceTypeRepresentation;
import org.keycloak.scim.resource.group.Group;
import org.keycloak.scim.resource.resourcetype.ResourceType;
import org.keycloak.scim.resource.resourcetype.ResourceType.SchemaExtension;
import org.keycloak.scim.resource.user.User;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakUrls;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.Test;

import static org.keycloak.scim.resource.Scim.ENTERPRISE_USER_SCHEMA;
import static org.keycloak.scim.resource.Scim.getCoreSchema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest(config = ScimServerConfig.class)
public class ResourceTypeTest extends AbstractScimTest {

    @InjectHttpClient
    HttpClient httpClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @Test
    public void testGet() {
        ListResponse<ResourceType> response = client.resourceTypes().getAll();

        assertNotNull(response);
        assertEquals(2, response.getResources().size());

        assertResourceType(response, User.class, "User Account", List.of(ENTERPRISE_USER_SCHEMA));
        assertResourceType(response, Group.class, "Group", List.of());
    }

    @Test
    public void testUnauthenticatedRequest() throws Exception {
        String url = keycloakUrls.getBaseUrl() + "/realms/" + realm.getName() + "/scim/v2/Users";
        HttpGet request = new HttpGet(url);
        request.setHeader("Accept", "application/scim+json");

        HttpResponse response = httpClient.execute(request);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatusLine().getStatusCode());
    }

    @Test
    public void testInvalidResourceType() {
        try {
            client.get("InvalidType");
            fail("Expected exception for invalid resource type");
        } catch (ScimClientException e) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), e.getError().getStatusInt());
        }
    }

    private static void assertResourceType(ListResponse<ResourceType> response, Class<? extends ResourceTypeRepresentation> resourceType,
            String expectedDescription, List<String> expectedSchemaExtensions) {
        ResourceType representation = response.getResources().stream().filter(r -> r.getName().equals(resourceType.getSimpleName())).findAny().orElse(null);

        assertNotNull(representation);
        assertEquals(resourceType.getSimpleName(), representation.getId());
        assertEquals(expectedDescription, representation.getDescription());
        assertEquals("/" + representation.getName() + "s", representation.getEndpoint());
        assertEquals(getCoreSchema(resourceType), representation.getSchema());

        if (!expectedSchemaExtensions.isEmpty()) {
            assertEquals(expectedSchemaExtensions.size(), representation.getSchemaExtensions().size());
            assertTrue(representation.getSchemaExtensions().stream().map(SchemaExtension::getSchema).toList().containsAll(expectedSchemaExtensions));
        }
    }
}
