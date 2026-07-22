package org.keycloak.tests.scim.tck;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.ws.rs.core.Response.Status;

import org.keycloak.scim.model.resourcetype.definition.ScimResourceTypeDefinitions;
import org.keycloak.scim.model.resourcetype.definition.ScimResourceTypeRepresentation;
import org.keycloak.scim.protocol.response.ListResponse;
import org.keycloak.scim.resource.resourcetype.ResourceType;
import org.keycloak.scim.resource.schema.Schema;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.util.JsonSerialization;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = ScimServerConfig.class)
public class ResourceTypeDefinitionTest extends AbstractScimTest {

    @InjectHttpClient
    HttpClient httpClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @Test
    public void testCreateGetUpdateDelete() throws IOException {
        ScimResourceTypeRepresentation created = createDefinition(newDevices());
        String id = created.getId();

        assertNotNull(id);
        assertEquals("Devices", created.getName());
        assertFalse(created.isBuiltIn());

        // list contains the custom type plus the read-only built-ins
        List<ScimResourceTypeRepresentation> all = listDefinitions();
        assertTrue(all.stream().anyMatch(d -> "Devices".equals(d.getName()) && !d.isBuiltIn()));
        assertTrue(all.stream().anyMatch(d -> "User".equals(d.getName()) && d.isBuiltIn()));
        assertTrue(all.stream().anyMatch(d -> "Group".equals(d.getName()) && d.isBuiltIn()));

        // read single
        ScimResourceTypeRepresentation fetched = getDefinition(id);
        assertEquals("Devices", fetched.getName());
        assertEquals("/Devices", fetched.getEndpoint());
        assertEquals(ScimResourceTypeDefinitions.DEFAULT_SCHEMA_URN_PREFIX + "Devices", fetched.getSchema());
        assertEquals(1, fetched.getAttributes().size());
        assertEquals("model", fetched.getAttributes().get(0).getName());

        // update
        fetched.setDescription("Managed devices");
        fetched.getAttributes().add(attribute("serialNumber", "string"));
        HttpResponse updateResponse = send(withJson(new HttpPut(url(id)), fetched));
        assertEquals(Status.OK.getStatusCode(), updateResponse.getStatusLine().getStatusCode());
        ScimResourceTypeRepresentation updated = readBody(updateResponse, ScimResourceTypeRepresentation.class);
        assertEquals("Managed devices", updated.getDescription());
        assertEquals(2, updated.getAttributes().size());

        // delete
        HttpResponse deleteResponse = send(auth(new HttpDelete(url(id))));
        assertEquals(Status.NO_CONTENT.getStatusCode(), deleteResponse.getStatusLine().getStatusCode());
        assertTrue(listDefinitions().stream().noneMatch(d -> "Devices".equals(d.getName()) && !d.isBuiltIn()));
    }

    @Test
    public void testDiscoveryReflectsCustomType() throws IOException {
        ScimResourceTypeRepresentation created = createDefinition(newDevices());
        realm.cleanup().add(r -> deleteQuietly(created.getId()));

        String schemaUrn = ScimResourceTypeDefinitions.DEFAULT_SCHEMA_URN_PREFIX + "Devices";

        // /ResourceTypes advertises the custom type
        ListResponse<ResourceType> resourceTypes = client.resourceTypes().getAll();
        ResourceType devices = resourceTypes.getResources().stream()
                .filter(r -> "Devices".equals(r.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(devices, "custom resource type should be listed in /ResourceTypes");
        assertEquals("/Devices", devices.getEndpoint());
        assertEquals(schemaUrn, devices.getSchema());

        // /Schemas exposes a schema derived from the attribute definitions
        Schema schema = client.schemas().get(schemaUrn);
        assertNotNull(schema, "custom schema should be exposed from /Schemas");
        assertEquals(schemaUrn, schema.getId());
        assertEquals("Devices", schema.getName());
        assertNotNull(schema.getAttributes());
        assertTrue(schema.getAttributes().stream().anyMatch(a -> "model".equals(a.getName())));
    }

    @Test
    public void testGetUnknownReturnsNotFound() throws IOException {
        HttpResponse response = send(auth(new HttpGet(url("does-not-exist"))));
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatusLine().getStatusCode());
        EntityUtils.consumeQuietly(response.getEntity());
    }

    @Test
    public void testDuplicateNameRejected() throws IOException {
        ScimResourceTypeRepresentation created = createDefinition(newDevices());
        realm.cleanup().add(r -> deleteQuietly(created.getId()));

        HttpResponse response = send(withJson(new HttpPost(url()), newDevices()));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatusLine().getStatusCode());
        EntityUtils.consumeQuietly(response.getEntity());
    }

    @Test
    public void testReservedNameRejected() throws IOException {
        ScimResourceTypeRepresentation definition = new ScimResourceTypeRepresentation();
        definition.setName("Users");
        definition.setAttributes(List.of(attribute("model", "string")));

        HttpResponse response = send(withJson(new HttpPost(url()), definition));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatusLine().getStatusCode());
        EntityUtils.consumeQuietly(response.getEntity());
    }

    @Test
    public void testReservedAttributeNameRejected() throws IOException {
        ScimResourceTypeRepresentation definition = new ScimResourceTypeRepresentation();
        definition.setName("Devices");
        definition.setAttributes(List.of(attribute("id", "string")));

        HttpResponse response = send(withJson(new HttpPost(url()), definition));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatusLine().getStatusCode());
        EntityUtils.consumeQuietly(response.getEntity());
    }

    @Test
    public void testUnauthenticatedRequestRejected() throws IOException {
        HttpGet request = new HttpGet(url());
        HttpResponse response = httpClient.execute(request);
        int status = response.getStatusLine().getStatusCode();
        EntityUtils.consumeQuietly(response.getEntity());
        assertTrue(status == Status.UNAUTHORIZED.getStatusCode() || status == Status.FORBIDDEN.getStatusCode(),
                "expected 401/403 but got " + status);
    }

    private ScimResourceTypeRepresentation newDevices() {
        ScimResourceTypeRepresentation definition = new ScimResourceTypeRepresentation();
        definition.setName("Devices");
        definition.setDescription("Devices");
        definition.setAttributes(List.of(attribute("model", "string")));
        return definition;
    }

    private static Schema.Attribute attribute(String name, String type) {
        Schema.Attribute attribute = new Schema.Attribute();
        attribute.setName(name);
        attribute.setType(type);
        return attribute;
    }

    private ScimResourceTypeRepresentation createDefinition(ScimResourceTypeRepresentation definition) throws IOException {
        HttpResponse response = send(withJson(new HttpPost(url()), definition));
        assertEquals(Status.CREATED.getStatusCode(), response.getStatusLine().getStatusCode());
        return readBody(response, ScimResourceTypeRepresentation.class);
    }

    private ScimResourceTypeRepresentation getDefinition(String id) throws IOException {
        HttpResponse response = send(auth(new HttpGet(url(id))));
        assertEquals(Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
        return readBody(response, ScimResourceTypeRepresentation.class);
    }

    private List<ScimResourceTypeRepresentation> listDefinitions() throws IOException {
        HttpResponse response = send(auth(new HttpGet(url())));
        assertEquals(Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
        return List.of(readBody(response, ScimResourceTypeRepresentation[].class));
    }

    private void deleteQuietly(String id) {
        try {
            EntityUtils.consumeQuietly(send(auth(new HttpDelete(url(id)))).getEntity());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private HttpResponse send(HttpUriRequest request) throws IOException {
        return httpClient.execute(request);
    }

    private <T extends HttpUriRequest> T auth(T request) {
        request.setHeader("Authorization", "Bearer " + adminClient.tokenManager().getAccessTokenString());
        return request;
    }

    private HttpUriRequest withJson(HttpUriRequest request, Object body) throws IOException {
        auth(request);
        request.setHeader("Content-Type", "application/json");
        ((org.apache.http.client.methods.HttpEntityEnclosingRequestBase) request)
                .setEntity(new StringEntity(JsonSerialization.writeValueAsString(body), StandardCharsets.UTF_8));
        return request;
    }

    private <T> T readBody(HttpResponse response, Class<T> type) throws IOException {
        String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        return JsonSerialization.readValue(body, type);
    }

    private String url() {
        return keycloakUrls.getBaseUrl() + "/admin/realms/" + realm.getName() + "/scim/resource-types";
    }

    private String url(String id) {
        return url() + "/" + id;
    }
}
