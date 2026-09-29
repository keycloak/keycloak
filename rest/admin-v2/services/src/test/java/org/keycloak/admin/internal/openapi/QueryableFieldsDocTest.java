package org.keycloak.admin.internal.openapi;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.admin.v2.SAMLClientRepresentation;
import org.keycloak.services.client.query.QueryParseUtils;
import org.keycloak.services.client.scim.BaseClientModelSchema;
import org.keycloak.services.client.scim.OIDCClientModelSchema;
import org.keycloak.services.client.scim.SAMLClientModelSchema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryableFieldsDocTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String COMMON_GROUP = "common";
    private static final Map<String, BaseClientModelSchema<?>> PROTOCOL_SCHEMAS = Map.of(
            OIDCClientRepresentation.PROTOCOL, OIDCClientModelSchema.INSTANCE,
            SAMLClientRepresentation.PROTOCOL, SAMLClientModelSchema.INSTANCE);

    private static JsonNode queryableFields;
    private static JsonNode schemas;
    private static Map<String, List<String>> documented;

    @BeforeAll
    static void loadDocModel() throws Exception {
        Path docFile = Path.of("target/admin-v2-doc.json");
        assertTrue(Files.exists(docFile), "Documentation model must be generated before running this test");

        JsonNode doc = MAPPER.readTree(docFile.toFile());
        queryableFields = doc.path("queryableFields");
        schemas = doc.path("schemas");
        documented = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> group : queryableFields.properties()) {
            List<String> names = new ArrayList<>();
            group.getValue().forEach(field -> names.add(field.path("name").asText()));
            documented.put(group.getKey(), names);
        }
    }

    @Test
    void documentedFieldsAreExactlyTheSearchableFields() {
        List<String> all = documented.values().stream().flatMap(List::stream).toList();

        assertEquals(all.size(), new HashSet<>(all).size(),
                "each field must be documented in exactly one group: " + documented);
        assertEquals(BaseClientModelSchema.QUERYABLE_FIELDS, new HashSet<>(all),
                "documented fields must match the fields accepted by the query validator");
        for (String field : all) {
            assertDoesNotThrow(() -> QueryParseUtils.validateField(field, null),
                    "documented field must be accepted by the query validator: " + field);
        }
    }

    @Test
    void fieldsAreGroupedByProtocolSupport() {
        Set<String> expectedGroups = new HashSet<>(PROTOCOL_SCHEMAS.keySet());
        expectedGroups.add(COMMON_GROUP);
        assertEquals(expectedGroups, documented.keySet());

        for (String field : documented.get(COMMON_GROUP)) {
            PROTOCOL_SCHEMAS.forEach((protocol, schema) -> assertNotNull(schema.getAttributeByPath(field),
                    "common field must be resolvable by the " + protocol + " schema: " + field));
        }
        PROTOCOL_SCHEMAS.forEach((protocol, schema) -> {
            for (String field : documented.get(protocol)) {
                assertNotNull(schema.getAttributeByPath(field),
                        "field must be resolvable by the " + protocol + " schema: " + field);
                PROTOCOL_SCHEMAS.forEach((other, otherSchema) -> {
                    if (!other.equals(protocol)) {
                        assertNull(otherSchema.getAttributeByPath(field),
                                field + " is documented as " + protocol + "-specific but is also resolvable by " + other);
                    }
                });
            }
        });
    }

    @Test
    void documentedFieldsHaveTypeAndDescription() {
        queryableFields.forEach(group -> group.forEach(field -> {
            String name = field.path("name").asText();
            assertTrue(field.hasNonNull("type"), "documented field must have a type: " + name);
            assertTrue(field.hasNonNull("description"), "documented field must have a description: " + name);
        }));
    }

    @Test
    void commonFieldsFollowRepresentationPropertyOrder() {
        List<String> propertyOrder = new ArrayList<>();
        schemas.path(BaseClientRepresentation.class.getSimpleName()).path("properties")
                .forEach(property -> propertyOrder.add(property.path("name").asText()));
        List<String> expected = propertyOrder.stream().filter(BaseClientModelSchema.QUERYABLE_FIELDS::contains).toList();

        assertFalse(expected.isEmpty(), "representation schema must declare the queryable properties");
        assertEquals(expected, documented.get(COMMON_GROUP));
    }
}
