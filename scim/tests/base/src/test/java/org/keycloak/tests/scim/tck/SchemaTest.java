package org.keycloak.tests.scim.tck;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.scim.protocol.response.ListResponse;
import org.keycloak.scim.resource.Scim;
import org.keycloak.scim.resource.schema.Schema;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = ScimServerConfig.class)
public class SchemaTest extends AbstractScimTest {

    @Test
    public void testGetAllSchemas() {
        ListResponse<Schema> response = client.schemas().getAll();

        assertNotNull(response);
        assertNotNull(response.getResources());
        assertEquals(3, response.getTotalResults());
        assertEquals(3, response.getResources().size());

        // Verify all expected schemas are present
        List<String> schemaIds = response.getResources().stream()
                .map(Schema::getId)
                .toList();

        assertTrue(schemaIds.contains(Scim.USER_CORE_SCHEMA));
        assertTrue(schemaIds.contains(Scim.GROUP_CORE_SCHEMA));
        assertTrue(schemaIds.contains(Scim.ENTERPRISE_USER_SCHEMA));
    }

    @Test
    public void testGetUserCoreSchema() {
        Schema schema = client.schemas().get(Scim.USER_CORE_SCHEMA);

        assertNotNull(schema);
        assertEquals(Scim.USER_CORE_SCHEMA, schema.getId());
        assertEquals("User", schema.getName());
        assertEquals("User Account", schema.getDescription());
        assertNotNull(schema.getAttributes());
        assertFalse(schema.getAttributes().isEmpty());

        // Verify ALL expected attributes are present (extracted from UserCoreModelSchema)
        Set<String> attributeNames = schema.getAttributes().stream()
                .map(Schema.Attribute::getName)
                .collect(Collectors.toSet());

        assertEquals(14, attributeNames.size(), "User schema should have exactly 14 attributes");

        assertAttribute(findAttribute(schema, "userName"), "string", false, true, false, "readWrite", "server");
        assertAttribute(findAttribute(schema, "emails"), "complex", true, false, false, "readWrite", "global");
        assertAttribute(findAttribute(schema, "name"), "complex", false, false, true, "readWrite", "none");
        assertAttribute(findAttribute(schema, "displayName"), "string", false, false, true, "readWrite", "none");
        assertAttribute(findAttribute(schema, "title"), "string", false, false, true, "readWrite", "none");
        assertAttribute(findAttribute(schema, "externalId"), "string", false, false, true, "readWrite", "none");
        assertAttribute(findAttribute(schema, "userType"), "string", false, false, true, "readWrite", "none");
        assertAttribute(findAttribute(schema, "nickName"), "string", false, false, true, "readWrite", "none");
        assertAttribute(findAttribute(schema, "locale"), "string", false, false, true, "readWrite", "none");
        assertAttribute(findAttribute(schema, "timezone"), "string", false, false, true, "readWrite", "none");
        assertAttribute(findAttribute(schema, "preferredLanguage"), "string", false, false, true, "readWrite", "none");
        assertAttribute(findAttribute(schema, "profileUrl"), "string", false, false, true, "readWrite", "none");
        assertAttribute(findAttribute(schema, "active"), "boolean", false, false, true, "readWrite", "none");
        assertAttribute(findAttribute(schema, "groups"), "complex", true, false, true, "readWrite", "none");

        // Verify name sub-attributes
        Schema.Attribute name = findAttribute(schema, "name");
        assertNotNull(name.getSubAttributes(), "name should have sub-attributes");
        Set<String> nameSubAttrNames = name.getSubAttributes().stream()
                .map(Schema.Attribute::getName)
                .collect(Collectors.toSet());
        assertTrue(nameSubAttrNames.contains("givenName"));
        assertTrue(nameSubAttrNames.contains("familyName"));
        assertTrue(nameSubAttrNames.contains("middleName"));
        assertTrue(nameSubAttrNames.contains("honorificPrefix"));
        assertTrue(nameSubAttrNames.contains("honorificSuffix"));
        assertTrue(nameSubAttrNames.contains("formatted"));
        for (Schema.Attribute subAttr : name.getSubAttributes()) {
            assertSubAttribute(subAttr, "string", false, "readWrite");
        }
    }

    @Test
    public void testGetGroupCoreSchema() {
        Schema schema = client.schemas().get(Scim.GROUP_CORE_SCHEMA);

        assertNotNull(schema);
        assertEquals(Scim.GROUP_CORE_SCHEMA, schema.getId());
        assertEquals("Group", schema.getName());
        assertEquals("Group", schema.getDescription());
        assertNotNull(schema.getAttributes());

        Set<String> attributeNames = schema.getAttributes().stream()
                .map(Schema.Attribute::getName)
                .collect(Collectors.toSet());

        assertEquals(2, attributeNames.size(), "Group schema should have exactly 2 attributes");

        assertAttribute(findAttribute(schema, "displayName"), "string", false, false, false, "readWrite", "none");
        assertAttribute(findAttribute(schema, "externalId"), "string", false, false, true, "immutable", "none");
    }

    @Test
    public void testGetEnterpriseUserSchema() {
        Schema schema = client.schemas().get(Scim.ENTERPRISE_USER_SCHEMA);

        assertNotNull(schema);
        assertEquals(Scim.ENTERPRISE_USER_SCHEMA, schema.getId());
        assertEquals("EnterpriseUser", schema.getName());
        assertEquals("Enterprise User", schema.getDescription());
        assertNotNull(schema.getAttributes());

        Set<String> attributeNames = schema.getAttributes().stream()
                .map(Schema.Attribute::getName)
                .collect(Collectors.toSet());

        assertEquals(6, attributeNames.size(), "Enterprise User schema should have exactly 6 attributes");

        // Simple string attributes
        assertAttribute(findAttribute(schema, "employeeNumber"), "string", false, false, true, "readWrite", "none");
        assertAttribute(findAttribute(schema, "costCenter"), "string", false, false, true, "readWrite", "none");
        assertAttribute(findAttribute(schema, "organization"), "string", false, false, true, "readWrite", "none");
        assertAttribute(findAttribute(schema, "division"), "string", false, false, true, "readWrite", "none");
        assertAttribute(findAttribute(schema, "department"), "string", false, false, true, "readWrite", "none");

        // Manager is a complex attribute with sub-attributes
        assertAttribute(findAttribute(schema, "manager"), "complex", false, false, false, "readWrite", "none");
        Schema.Attribute manager = findAttribute(schema, "manager");
        assertNotNull(manager.getSubAttributes(), "manager should have sub-attributes");
        Set<String> managerSubAttrNames = manager.getSubAttributes().stream()
                .map(Schema.Attribute::getName)
                .collect(Collectors.toSet());
        assertTrue(managerSubAttrNames.contains("value"));
        assertTrue(managerSubAttrNames.contains("displayName"));
        for (Schema.Attribute subAttr : manager.getSubAttributes()) {
            assertSubAttribute(subAttr, "string", false, "readWrite");
        }
    }

    @Test
    public void testNoDuplicateAttributes() {
        // Verify that attributes are not duplicated even when multiple SCIM paths
        // map to the same top-level attribute

        Schema userSchema = client.schemas().get(Scim.USER_CORE_SCHEMA);
        List<String> attributeNames = userSchema.getAttributes().stream()
                .map(Schema.Attribute::getName)
                .toList();

        // Check for duplicates
        Set<String> uniqueNames = Set.copyOf(attributeNames);
        assertEquals(uniqueNames.size(), attributeNames.size(),
                "Schema should not have duplicate attribute names");

        // Specifically verify 'name' appears only once (not once per name.* path)
        long nameCount = attributeNames.stream()
                .filter("name"::equals)
                .count();
        assertEquals(1, nameCount, "name attribute should appear exactly once");

        // Verify EnterpriseUser manager appears only once
        Schema enterpriseSchema = client.schemas().get(Scim.ENTERPRISE_USER_SCHEMA);
        List<String> enterpriseNames = enterpriseSchema.getAttributes().stream()
                .map(Schema.Attribute::getName)
                .toList();
        long managerCount = enterpriseNames.stream()
                .filter("manager"::equals)
                .count();
        assertEquals(1, managerCount, "manager attribute should appear exactly once");
    }

    // Helper method to find attribute by name
    private Schema.Attribute findAttribute(Schema schema, String name) {
        return schema.getAttributes().stream()
                .filter(attr -> name.equals(attr.getName()))
                .findFirst()
                .orElse(null);
    }

    private void assertAttribute(Schema.Attribute attribute, String type, boolean multiValued,
                                  boolean required, boolean caseExact, String mutability, String uniqueness) {
        assertNotNull(attribute, "attribute should not be null");
        assertEquals(type, attribute.getType(), attribute.getName() + " type");
        assertEquals(multiValued, attribute.getMultiValued(), attribute.getName() + " multiValued");
        assertEquals(required, attribute.getRequired(), attribute.getName() + " required");
        assertEquals(caseExact, attribute.getCaseExact(), attribute.getName() + " caseExact");
        assertEquals(mutability, attribute.getMutability(), attribute.getName() + " mutability");
        assertEquals(uniqueness, attribute.getUniqueness(), attribute.getName() + " uniqueness");
    }

    private void assertSubAttribute(Schema.Attribute subAttribute, String type, boolean multiValued, String mutability) {
        assertNotNull(subAttribute, "sub-attribute should not be null");
        assertEquals(type, subAttribute.getType(), subAttribute.getName() + " sub-attribute type");
        assertEquals(multiValued, subAttribute.getMultiValued(), subAttribute.getName() + " sub-attribute multiValued");
        assertEquals(mutability, subAttribute.getMutability(), subAttribute.getName() + " sub-attribute mutability");
    }
}
