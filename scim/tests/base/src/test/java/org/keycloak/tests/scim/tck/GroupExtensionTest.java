/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.keycloak.tests.scim.tck;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.scim.client.ResourceFilter;
import org.keycloak.scim.model.group.GroupAttributeMappings;
import org.keycloak.scim.model.group.GroupExtensionModelSchema;
import org.keycloak.scim.protocol.request.PatchRequest;
import org.keycloak.scim.protocol.response.ListResponse;
import org.keycloak.scim.resource.Scim;
import org.keycloak.scim.resource.group.Group;
import org.keycloak.scim.resource.schema.Schema;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class GroupExtensionTest extends AbstractScimTest {

    private static final String CUSTOM_SCHEMA = "urn:example:params:scim:schemas:extension:custom:1.0:Group";

    private static final String MAPPINGS = """
            [
              {"name": "entryUUID", "scimAttribute": "%1$s:entryUUID", "readOnly": true},
              {"name": "costCenter", "scimAttribute": "%1$s:costCenter"},
              {"name": "tags", "scimAttribute": "%1$s:tags", "multivalued": true}
            ]
            """.formatted(CUSTOM_SCHEMA);

    @Test
    public void testSchemaNotListedWithoutMappings() {
        List<String> schemaIds = client.schemas().getAll().getResources().stream().map(Schema::getId).toList();
        assertFalse(schemaIds.contains(CUSTOM_SCHEMA));
        assertNull(client.schemas().get(CUSTOM_SCHEMA));
    }

    @Test
    public void testSchemaListedWithMappings() {
        setMappings(MAPPINGS);

        List<String> schemaIds = client.schemas().getAll().getResources().stream().map(Schema::getId).toList();
        assertTrue(schemaIds.contains(CUSTOM_SCHEMA));
        assertTrue(schemaIds.contains(Scim.GROUP_CORE_SCHEMA));

        Schema schema = client.schemas().get(CUSTOM_SCHEMA);
        assertNotNull(schema);
        assertEquals(3, schema.getAttributes().size());

        Schema.Attribute entryUUID = findAttribute(schema, "entryUUID");
        assertEquals("string", entryUUID.getType());
        assertEquals("readOnly", entryUUID.getMutability());
        assertFalse(entryUUID.getMultiValued());

        Schema.Attribute costCenter = findAttribute(schema, "costCenter");
        assertEquals("readWrite", costCenter.getMutability());
        assertFalse(costCenter.getMultiValued());

        Schema.Attribute tags = findAttribute(schema, "tags");
        assertEquals("readWrite", tags.getMutability());
        assertTrue(tags.getMultiValued());

        // the core group schema is unchanged
        Schema core = client.schemas().get(Scim.GROUP_CORE_SCHEMA);
        assertTrue(core.getAttributes().stream().noneMatch(a -> a.getName().equals("entryUUID")));
    }

    @Test
    public void testGetGroupWithExtensionAttributes() {
        setMappings(MAPPINGS);

        String id = createGroupWithAdminApi(Map.of(
                "entryUUID", List.of("5f1a2b3c"),
                "costCenter", List.of("CC-42"),
                "tags", List.of("a", "b"),
                "unmapped", List.of("hidden")));

        Group group = client.groups().get(id);

        assertTrue(group.getSchemas().contains(Scim.GROUP_CORE_SCHEMA));
        assertTrue(group.getSchemas().contains(CUSTOM_SCHEMA));
        Map<?, ?> extension = getExtension(group);
        assertEquals("5f1a2b3c", extension.get("entryUUID"));
        assertEquals("CC-42", extension.get("costCenter"));
        assertEquals(List.of("a", "b"), extension.get("tags"));
        assertFalse(extension.containsKey("unmapped"));

        // only the requested extension attribute is returned
        group = client.groups().get(id, List.of(CUSTOM_SCHEMA + ":costCenter"));
        extension = getExtension(group);
        assertEquals(Map.of("costCenter", "CC-42"), extension);
        // the core schema is always included, even if no core attribute is returned
        assertTrue(group.getSchemas().contains(Scim.GROUP_CORE_SCHEMA));

        // the extension schema is excluded
        group = client.groups().get(id, null, List.of(CUSTOM_SCHEMA));
        assertNull(group.getExtensions() == null ? null : group.getExtensions().get(CUSTOM_SCHEMA));
        assertFalse(group.getSchemas().contains(CUSTOM_SCHEMA));
    }

    @Test
    public void testGetGroupWithoutExtensionValues() {
        setMappings(MAPPINGS);

        String id = createGroupWithAdminApi(Map.of());
        Group group = client.groups().get(id);

        assertFalse(group.getSchemas().contains(CUSTOM_SCHEMA));
        assertTrue(group.getExtensions() == null || !group.getExtensions().containsKey(CUSTOM_SCHEMA));
    }

    @Test
    public void testCreateAndUpdate() {
        setMappings(MAPPINGS);

        Group group = new Group();
        group.addSchema(Scim.GROUP_CORE_SCHEMA);
        group.addSchema(CUSTOM_SCHEMA);
        group.setDisplayName(KeycloakModelUtils.generateId());
        setExtension(group, Map.of("costCenter", "CC-1", "tags", List.of("x", "y"), "entryUUID", "client-value"));

        group = client.groups().create(group);
        String id = group.getId();

        GroupRepresentation rep = realm.admin().groups().group(id).toRepresentation();
        assertEquals(List.of("CC-1"), rep.getAttributes().get("costCenter"));
        assertEquals(2, rep.getAttributes().get("tags").size());
        assertTrue(rep.getAttributes().get("tags").containsAll(List.of("x", "y")));
        // read-only attributes are ignored
        assertNull(rep.getAttributes().get("entryUUID"));

        group = client.groups().get(id);
        group.setExtensions(new HashMap<>());
        setExtension(group, Map.of("costCenter", "CC-2", "tags", List.of("z")));
        client.groups().update(group);

        rep = realm.admin().groups().group(id).toRepresentation();
        assertEquals(List.of("CC-2"), rep.getAttributes().get("costCenter"));
        assertEquals(List.of("z"), rep.getAttributes().get("tags"));
    }

    @Test
    public void testPatch() {
        setMappings(MAPPINGS);

        String id = createGroupWithAdminApi(Map.of(
                "entryUUID", List.of("5f1a2b3c"),
                "costCenter", List.of("CC-1"),
                "tags", List.of("a")));

        client.groups().patch(id, PatchRequest.create()
                .replace(CUSTOM_SCHEMA + ":costCenter", "CC-2")
                .add(CUSTOM_SCHEMA + ":tags", "b")
                .replace(CUSTOM_SCHEMA + ":entryUUID", "changed")
                .build());

        Map<?, ?> extension = getExtension(client.groups().get(id));
        assertEquals("CC-2", extension.get("costCenter"));
        assertEquals(2, ((List<?>) extension.get("tags")).size());
        assertTrue(((List<?>) extension.get("tags")).containsAll(List.of("a", "b")));
        // read-only attributes are ignored
        assertEquals("5f1a2b3c", extension.get("entryUUID"));

        client.groups().patch(id, PatchRequest.create()
                .remove(CUSTOM_SCHEMA + ":costCenter")
                .remove(CUSTOM_SCHEMA + ":entryUUID")
                .build());

        extension = getExtension(client.groups().get(id));
        assertFalse(extension.containsKey("costCenter"));
        assertEquals("5f1a2b3c", extension.get("entryUUID"));
    }

    @Test
    public void testPatchExtensionObject() {
        setMappings(MAPPINGS);

        String id = createGroupWithAdminApi(Map.of("costCenter", List.of("CC-1")));

        // the path is the extension schema and the value holds its attributes
        client.groups().patch(id, PatchRequest.create()
                .replace(CUSTOM_SCHEMA, "{\"costCenter\": \"CC-2\"}")
                .build());
        assertEquals("CC-2", getExtension(client.groups().get(id)).get("costCenter"));

        // no path, and the value holds the extension schema
        client.groups().patch(id, PatchRequest.create()
                .replace("{\"" + CUSTOM_SCHEMA + "\": {\"costCenter\": \"CC-3\"}}")
                .build());
        assertEquals("CC-3", getExtension(client.groups().get(id)).get("costCenter"));

        // add to a multivalued attribute through the extension schema
        client.groups().patch(id, PatchRequest.create()
                .add(CUSTOM_SCHEMA, "{\"tags\": [\"a\", \"b\"]}")
                .build());
        List<?> tags = (List<?>) getExtension(client.groups().get(id)).get("tags");
        assertEquals(2, tags.size());
        assertTrue(tags.containsAll(List.of("a", "b")));
    }

    @Test
    public void testSelectByExtensionSchema() {
        setMappings("""
                [
                  {"name": "costCenter", "scimAttribute": "%s:costCenter"},
                  {"name": "team", "scimAttribute": "%s:team"}
                ]
                """.formatted(CUSTOM_SCHEMA, GroupExtensionModelSchema.KEYCLOAK_GROUP_SCHEMA));

        String id = createGroupWithAdminApi(Map.of("costCenter", List.of("CC-1"), "team", List.of("core-iam")));

        // excluding one extension schema does not exclude the other one served by the same model schema
        Group group = client.groups().get(id, null, List.of(GroupExtensionModelSchema.KEYCLOAK_GROUP_SCHEMA));
        assertEquals("CC-1", getExtension(group).get("costCenter"));
        assertFalse(group.getExtensions().containsKey(GroupExtensionModelSchema.KEYCLOAK_GROUP_SCHEMA));
        assertFalse(group.getSchemas().contains(GroupExtensionModelSchema.KEYCLOAK_GROUP_SCHEMA));

        group = client.groups().get(id, null, List.of(CUSTOM_SCHEMA));
        assertFalse(group.getExtensions().containsKey(CUSTOM_SCHEMA));
        assertEquals(Map.of("team", "core-iam"), group.getExtensions().get(GroupExtensionModelSchema.KEYCLOAK_GROUP_SCHEMA));

        // requesting one extension schema only returns the attributes from that extension
        group = client.groups().get(id, List.of(GroupExtensionModelSchema.KEYCLOAK_GROUP_SCHEMA));
        assertEquals(Map.of("team", "core-iam"), group.getExtensions().get(GroupExtensionModelSchema.KEYCLOAK_GROUP_SCHEMA));
        assertFalse(group.getExtensions().containsKey(CUSTOM_SCHEMA));
        assertTrue(group.getSchemas().contains(Scim.GROUP_CORE_SCHEMA));
    }

    @Test
    public void testFilter() {
        setMappings(MAPPINGS);

        String uuid = KeycloakModelUtils.generateId();
        String id = createGroupWithAdminApi(Map.of("entryUUID", List.of(uuid), "tags", List.of("red", "blue")));
        createGroupWithAdminApi(Map.of("entryUUID", List.of(KeycloakModelUtils.generateId()), "tags", List.of("red")));

        ListResponse<Group> response = client.groups().getAll(ResourceFilter.filter().eq(CUSTOM_SCHEMA + ":entryUUID", uuid).build());
        assertEquals(1, response.getTotalResults());
        assertEquals(id, response.getResources().get(0).getId());

        response = client.groups().getAll(ResourceFilter.filter().eq(CUSTOM_SCHEMA + ":tags", "blue").build());
        assertEquals(1, response.getTotalResults());
        assertEquals(id, response.getResources().get(0).getId());

        response = client.groups().getAll(ResourceFilter.filter().eq(CUSTOM_SCHEMA + ":tags", "red").build());
        assertEquals(2, response.getTotalResults());

        response = client.groups().getAll(ResourceFilter.filter().eq(CUSTOM_SCHEMA + ":entryUUID", "non-existent").build());
        assertEquals(0, response.getTotalResults());

        // unmapped attributes cannot be used to filter
        createGroupWithAdminApi(Map.of("unmapped", List.of("value")));
        response = client.groups().getAll(ResourceFilter.filter().eq(CUSTOM_SCHEMA + ":unmapped", "value").build());
        assertEquals(0, response.getTotalResults());
    }

    @Test
    public void testInvalidMappingsRejected() {
        String duplicate = """
                [
                  {"name": "a", "scimAttribute": "%1$s:entryUUID"},
                  {"name": "b", "scimAttribute": "%1$s:entryUUID"}
                ]
                """.formatted(CUSTOM_SCHEMA);

        for (String invalid : List.of(duplicate,
                "[{\"name\": \"externalId\", \"scimAttribute\": \"" + CUSTOM_SCHEMA + ":externalId\"}]",
                "[{\"name\": \"entryUUID\", \"scimAttribute\": \"entryUUID\"}]",
                "[{\"name\": \"entryUUID\", \"scimAttribute\": \"" + Scim.GROUP_CORE_SCHEMA + ":entryUUID\"}]",
                "[{\"name\": \"entryUUID\", \"scimAttribute\": \"urn:bad schema:Group:entryUUID\"}]",
                "not json")) {
            RealmRepresentation rep = realm.admin().toRepresentation();
            rep.getAttributes().put(GroupAttributeMappings.REALM_ATTRIBUTE, invalid);
            assertThrows(BadRequestException.class, () -> realm.admin().update(rep), invalid);
        }

        assertNull(realm.admin().toRepresentation().getAttributes().get(GroupAttributeMappings.REALM_ATTRIBUTE));
    }

    private void setMappings(String mappings) {
        realm.updateWithCleanup(r -> r.attribute(GroupAttributeMappings.REALM_ATTRIBUTE, mappings));
    }

    private String createGroupWithAdminApi(Map<String, List<String>> attributes) {
        GroupRepresentation rep = new GroupRepresentation();
        rep.setName(KeycloakModelUtils.generateId());
        rep.setAttributes(attributes);

        try (Response response = realm.admin().groups().add(rep)) {
            String id = ApiUtil.getCreatedId(response);
            realm.cleanup().add(r -> r.groups().group(id).remove());
            return id;
        }
    }

    private static Map<?, ?> getExtension(Group group) {
        assertNotNull(group.getExtensions());
        Object value = group.getExtensions().get(CUSTOM_SCHEMA);
        assertInstanceOf(Map.class, value);
        return (Map<?, ?>) value;
    }

    private static void setExtension(Group group, Map<String, Object> values) {
        if (group.getExtensions() == null) {
            group.setExtensions(new HashMap<>());
        }
        group.getExtensions().put(CUSTOM_SCHEMA, new HashMap<>(values));
    }

    private static Schema.Attribute findAttribute(Schema schema, String name) {
        return schema.getAttributes().stream()
                .filter(a -> a.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Attribute " + name + " not found in schema " + schema.getId()));
    }
}
