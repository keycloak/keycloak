package org.keycloak.scim.resource.response;

import java.util.List;

import org.keycloak.scim.protocol.response.ListResponse;
import org.keycloak.scim.resource.ResourceTypeRepresentation;
import org.keycloak.scim.resource.group.Group;
import org.keycloak.scim.resource.user.User;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ListResponseDeserializerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testDeserializeUserListResponse() throws Exception {
        String json = """
            {
              "schemas": ["urn:ietf:params:scim:api:messages:2.0:ListResponse"],
              "totalResults": 2,
              "startIndex": 1,
              "itemsPerPage": 2,
              "Resources": [
                {
                  "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
                  "id": "user1",
                  "userName": "bjensen@example.com",
                  "name": {
                    "givenName": "Barbara",
                    "familyName": "Jensen"
                  },
                  "active": true
                },
                {
                  "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
                  "id": "user2",
                  "userName": "jsmith@example.com",
                  "name": {
                    "givenName": "John",
                    "familyName": "Smith"
                  },
                  "active": true
                }
              ]
            }
            """;

        ListResponse<?> response = objectMapper.readValue(json, ListResponse.class);

        assertNotNull(response);
        assertEquals(2, response.getTotalResults());
        assertEquals(1, response.getStartIndex());
        assertEquals(2, response.getItemsPerPage());

        List<? extends ResourceTypeRepresentation> resources = response.getResources();
        assertNotNull(resources);
        assertEquals(2, resources.size());

        // Verify first resource is a User
        assertTrue(resources.get(0) instanceof User);
        User user1 = (User) resources.get(0);
        assertEquals("user1", user1.getId());
        assertEquals("bjensen@example.com", user1.getUserName());
        assertEquals("Barbara", user1.getName().getGivenName());
        assertEquals("Jensen", user1.getName().getFamilyName());
        assertTrue(user1.getActive());

        // Verify second resource is a User
        assertTrue(resources.get(1) instanceof User);
        User user2 = (User) resources.get(1);
        assertEquals("user2", user2.getId());
        assertEquals("jsmith@example.com", user2.getUserName());
    }

    @Test
    void testDeserializeGroupListResponse() throws Exception {
        String json = """
            {
              "schemas": ["urn:ietf:params:scim:api:messages:2.0:ListResponse"],
              "totalResults": 1,
              "Resources": [
                {
                  "schemas": ["urn:ietf:params:scim:schemas:core:2.0:Group"],
                  "id": "group1",
                  "displayName": "Engineering",
                  "members": [
                    {
                      "value": "user1",
                      "display": "Barbara Jensen"
                    }
                  ]
                }
              ]
            }
            """;

        ListResponse<?> response = objectMapper.readValue(json, ListResponse.class);

        assertNotNull(response);
        assertEquals(1, response.getTotalResults());

        List<? extends ResourceTypeRepresentation> resources = response.getResources();
        assertNotNull(resources);
        assertEquals(1, resources.size());

        // Verify resource is a Group
        assertTrue(resources.get(0) instanceof Group);
        Group group = (Group) resources.get(0);
        assertEquals("group1", group.getId());
        assertEquals("Engineering", group.getDisplayName());
        assertNotNull(group.getMembers());
        assertEquals(1, group.getMembers().size());
        assertEquals("user1", group.getMembers().get(0).getValue());
        assertEquals("Barbara Jensen", group.getMembers().get(0).getDisplay());
    }

    @Test
    void testDeserializeMixedResourceTypes() throws Exception {
        // This test demonstrates that resources can be of different types in the same response
        // (though this is not common in SCIM, the deserializer supports it)
        String json = """
            {
              "schemas": ["urn:ietf:params:scim:api:messages:2.0:ListResponse"],
              "totalResults": 2,
              "Resources": [
                {
                  "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
                  "id": "user1",
                  "userName": "bjensen@example.com"
                },
                {
                  "schemas": ["urn:ietf:params:scim:schemas:core:2.0:Group"],
                  "id": "group1",
                  "displayName": "Engineering"
                }
              ]
            }
            """;

        ListResponse<?> response = objectMapper.readValue(json, ListResponse.class);

        assertNotNull(response);
        assertEquals(2, response.getTotalResults());

        List<? extends ResourceTypeRepresentation> resources = response.getResources();
        assertNotNull(resources);
        assertEquals(2, resources.size());

        // First resource is User
        assertTrue(resources.get(0) instanceof User);
        User user = (User) resources.get(0);
        assertEquals("user1", user.getId());

        // Second resource is Group
        assertTrue(resources.get(1) instanceof Group);
        Group group = (Group) resources.get(1);
        assertEquals("group1", group.getId());
    }

    @Test
    void testDeserializeUserWithEnterpriseExtension() throws Exception {
        String json = """
            {
              "schemas": ["urn:ietf:params:scim:api:messages:2.0:ListResponse"],
              "totalResults": 1,
              "Resources": [
                {
                  "schemas": [
                    "urn:ietf:params:scim:schemas:core:2.0:User",
                    "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"
                  ],
                  "id": "user1",
                  "userName": "bjensen@example.com",
                  "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User": {
                    "employeeNumber": "12345",
                    "department": "Engineering"
                  }
                }
              ]
            }
            """;

        ListResponse<?> response = objectMapper.readValue(json, ListResponse.class);

        assertNotNull(response);
        List<? extends ResourceTypeRepresentation> resources = response.getResources();
        assertNotNull(resources);
        assertEquals(1, resources.size());

        // Verify it's a User with enterprise extension
        assertTrue(resources.get(0) instanceof User);
        User user = (User) resources.get(0);
        assertEquals("user1", user.getId());
        assertNotNull(user.getEnterpriseUser());
        assertEquals("12345", user.getEnterpriseUser().getEmployeeNumber());
        assertEquals("Engineering", user.getEnterpriseUser().getDepartment());
    }

    @Test
    void testDeserializeEmptyListResponse() throws Exception {
        String json = """
            {
              "schemas": ["urn:ietf:params:scim:api:messages:2.0:ListResponse"],
              "totalResults": 0,
              "Resources": []
            }
            """;

        ListResponse<?> response = objectMapper.readValue(json, ListResponse.class);

        assertNotNull(response);
        assertEquals(0, response.getTotalResults());
        assertNotNull(response.getResources());
        assertTrue(response.getResources().isEmpty());
    }
}
