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
        // UserCoreModelSchema has: userName, emails[0].value, name.*, externalId, nickName, locale, active
        // These should map to top-level attributes: userName, emails, name, externalId, nickName, locale, active
        Set<String> attributeNames = schema.getAttributes().stream()
                .map(Schema.Attribute::getName)
                .collect(Collectors.toSet());

        assertEquals(14, attributeNames.size(), "User schema should have exactly 7 attributes");
        assertTrue(attributeNames.contains("userName"));
        assertTrue(attributeNames.contains("emails"));
        assertTrue(attributeNames.contains("name"));
        assertTrue(attributeNames.contains("externalId"));
        assertTrue(attributeNames.contains("nickName"));
        assertTrue(attributeNames.contains("locale"));
        assertTrue(attributeNames.contains("active"));
        assertTrue(attributeNames.contains("profileUrl"));
        assertTrue(attributeNames.contains("preferredLanguage"));
        assertTrue(attributeNames.contains("displayName"));
        assertTrue(attributeNames.contains("timezone"));
        assertTrue(attributeNames.contains("groups"));
        assertTrue(attributeNames.contains("title"));
        assertTrue(attributeNames.contains("userType"));
    }

    @Test
    public void testGetGroupCoreSchema() {
        Schema schema = client.schemas().get(Scim.GROUP_CORE_SCHEMA);

        assertNotNull(schema);
        assertEquals(Scim.GROUP_CORE_SCHEMA, schema.getId());
        assertEquals("Group", schema.getName());
        assertEquals("Group", schema.getDescription());
        assertNotNull(schema.getAttributes());

        // Verify ALL expected attributes are present (extracted from GroupCoreModelSchema)
        // GroupCoreModelSchema currently only has: displayName
        // Note: members is not yet supported in GroupCoreModelSchema attribute mappers
        Set<String> attributeNames = schema.getAttributes().stream()
                .map(Schema.Attribute::getName)
                .collect(Collectors.toSet());

        assertEquals(1, attributeNames.size(), "Group schema should have exactly 1 attribute");
        assertTrue(attributeNames.contains("displayName"));
    }

    @Test
    public void testGetEnterpriseUserSchema() {
        Schema schema = client.schemas().get(Scim.ENTERPRISE_USER_SCHEMA);

        assertNotNull(schema);
        assertEquals(Scim.ENTERPRISE_USER_SCHEMA, schema.getId());
        assertEquals("EnterpriseUser", schema.getName());
        assertEquals("Enterprise User", schema.getDescription());
        assertNotNull(schema.getAttributes());

        // Verify ALL expected attributes are present (extracted from UserEnterpriseModelSchema)
        // UserEnterpriseModelSchema has: employeeNumber, costCenter, organization, division, department, manager.*
        // These should map to: employeeNumber, costCenter, organization, division, department, manager
        Set<String> attributeNames = schema.getAttributes().stream()
                .map(Schema.Attribute::getName)
                .collect(Collectors.toSet());

        assertEquals(6, attributeNames.size(), "Enterprise User schema should have exactly 6 attributes");
        assertTrue(attributeNames.contains("employeeNumber"));
        assertTrue(attributeNames.contains("costCenter"));
        assertTrue(attributeNames.contains("organization"));
        assertTrue(attributeNames.contains("division"));
        assertTrue(attributeNames.contains("department"));
        assertTrue(attributeNames.contains("manager"));
    }

    @Test
    public void testAttributeProperties() {
        Schema schema = client.schemas().get(Scim.USER_CORE_SCHEMA);

        // Test STRING type (userName)
        Schema.Attribute userNameAttr = findAttribute(schema, "userName");
        assertNotNull(userNameAttr, "userName attribute should exist");
        assertEquals("string", userNameAttr.getType());
        assertEquals(false, userNameAttr.getMultiValued());

        // Test BOOLEAN type (active)
        Schema.Attribute activeAttr = findAttribute(schema, "active");
        assertNotNull(activeAttr, "active attribute should exist");
        assertEquals("boolean", activeAttr.getType());
        assertEquals(false, activeAttr.getMultiValued());

        // Test COMPLEX multi-valued (emails)
        Schema.Attribute emailsAttr = findAttribute(schema, "emails");
        assertNotNull(emailsAttr, "emails attribute should exist");
        assertEquals("complex", emailsAttr.getType());
        assertEquals(true, emailsAttr.getMultiValued());

        // Test COMPLEX single-valued (name)
        Schema.Attribute nameAttr = findAttribute(schema, "name");
        assertNotNull(nameAttr, "name attribute should exist");
        assertEquals("complex", nameAttr.getType());
        assertEquals(false, nameAttr.getMultiValued());

        // Test STRING attributes (externalId, nickName, locale)
        Schema.Attribute externalIdAttr = findAttribute(schema, "externalId");
        assertNotNull(externalIdAttr, "externalId attribute should exist");
        assertEquals("string", externalIdAttr.getType());
        assertEquals(false, externalIdAttr.getMultiValued());

        Schema.Attribute nickNameAttr = findAttribute(schema, "nickName");
        assertNotNull(nickNameAttr, "nickName attribute should exist");
        assertEquals("string", nickNameAttr.getType());
        assertEquals(false, nickNameAttr.getMultiValued());

        Schema.Attribute localeAttr = findAttribute(schema, "locale");
        assertNotNull(localeAttr, "locale attribute should exist");
        assertEquals("string", localeAttr.getType());
        assertEquals(false, localeAttr.getMultiValued());
    }

    @Test
    public void testReferenceTypes() {
        // Test EnterpriseUser manager is a complex attribute
        Schema enterpriseUserSchema = client.schemas().get(Scim.ENTERPRISE_USER_SCHEMA);
        Schema.Attribute managerAttr = findAttribute(enterpriseUserSchema, "manager");
        assertNotNull(managerAttr, "manager attribute should exist");
        assertEquals("complex", managerAttr.getType());
        assertEquals(false, managerAttr.getMultiValued());

        // TODO: referenceTypes are not yet tracked in the model schema Attribute.
        // Once Attribute supports reference types, add assertions for:
        //   managerAttr.getReferenceTypes() containing "User"

        // Note: Group.members is not yet supported in GroupCoreModelSchema,
        // so reference type testing for members is omitted
    }

    @Test
    public void testEnterpriseUserAttributeTypes() {
        Schema schema = client.schemas().get(Scim.ENTERPRISE_USER_SCHEMA);

        // All enterprise user attributes (except manager) should be string type
        String[] stringAttributes = {"employeeNumber", "costCenter", "organization", "division", "department"};
        for (String attrName : stringAttributes) {
            Schema.Attribute attr = findAttribute(schema, attrName);
            assertNotNull(attr, attrName + " attribute should exist");
            assertEquals("string", attr.getType(), attrName + " should be string type");
            assertEquals(false, attr.getMultiValued(), attrName + " should not be multi-valued");
        }

        // Manager is complex type with User reference
        Schema.Attribute managerAttr = findAttribute(schema, "manager");
        assertNotNull(managerAttr, "manager attribute should exist");
        assertEquals("complex", managerAttr.getType());
        assertEquals(false, managerAttr.getMultiValued());
    }

    @Test
    public void testGroupAttributeTypes() {
        Schema schema = client.schemas().get(Scim.GROUP_CORE_SCHEMA);

        // displayName should be string
        Schema.Attribute displayNameAttr = findAttribute(schema, "displayName");
        assertNotNull(displayNameAttr, "displayName attribute should exist");
        assertEquals("string", displayNameAttr.getType());
        assertEquals(false, displayNameAttr.getMultiValued());

        // Note: members is not yet supported in GroupCoreModelSchema attribute mappers
    }

    @Test
    public void testPathExtractionLogic() {
        // This test verifies that the path extraction logic works correctly
        // Multiple SCIM paths should map to the same top-level attribute

        Schema userSchema = client.schemas().get(Scim.USER_CORE_SCHEMA);

        // UserCoreModelSchema has multiple paths for 'name':
        // - name.givenName, name.familyName, name.middleName, name.honorificPrefix, name.honorificSuffix
        // All should map to a single 'name' attribute
        Schema.Attribute nameAttr = findAttribute(userSchema, "name");
        assertNotNull(nameAttr, "name attribute should exist (from multiple name.* paths)");
        assertEquals("complex", nameAttr.getType());
        assertEquals(false, nameAttr.getMultiValued());

        // emails[0].value should map to 'emails' attribute
        Schema.Attribute emailsAttr = findAttribute(userSchema, "emails");
        assertNotNull(emailsAttr, "emails attribute should exist (from emails[0].value path)");
        assertEquals("complex", emailsAttr.getType());
        assertEquals(true, emailsAttr.getMultiValued());

        // EnterpriseUser has manager.value and manager.displayName
        // Both should map to a single 'manager' attribute
        Schema enterpriseSchema = client.schemas().get(Scim.ENTERPRISE_USER_SCHEMA);
        Schema.Attribute managerAttr = findAttribute(enterpriseSchema, "manager");
        assertNotNull(managerAttr, "manager attribute should exist (from manager.* paths)");
        assertEquals("complex", managerAttr.getType());
        assertEquals(false, managerAttr.getMultiValued());
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
}
