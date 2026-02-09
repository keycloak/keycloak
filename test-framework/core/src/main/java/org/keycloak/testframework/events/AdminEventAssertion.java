package org.keycloak.testframework.events;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.common.util.reflections.Reflections;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.AuthDetailsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;

public class AdminEventAssertion {

    private final AdminEventRepresentation event;
    private final boolean expectSuccess;

    protected AdminEventAssertion(AdminEventRepresentation event, boolean expectSuccess) {
        Assertions.assertNotNull(event, "Event was null");
        Assertions.assertNotNull(event.getId(), "Event id was null");
        this.event = event;
        this.expectSuccess = expectSuccess;
    }

    public static AdminEventAssertion assertSuccess(AdminEventRepresentation event) {
        Assertions.assertFalse(event.getOperationType().endsWith("_ERROR"), "Expected successful event");
        return new AdminEventAssertion(event, true)
                .assertEventId()
                .assertValidOperationType();
    }

    public static AdminEventAssertion assertError(AdminEventRepresentation event) {
        Assertions.assertTrue(event.getOperationType().endsWith("_ERROR"), "Expected error event");
        return new AdminEventAssertion(event, false)
                .assertEventId()
                .assertValidOperationType();
    }

    public static AdminEventAssertion assertEvent(AdminEventRepresentation event, OperationType operationType, String resourcePath, Object representation, ResourceType resourceType) {
        return assertSuccess(event)
                .operationType(operationType)
                .resourcePath(resourcePath)
                .representation(representation)
                .resourceType(resourceType);
    }

    public static AdminEventAssertion assertEvent(AdminEventRepresentation event, OperationType operationType, String resourcePath, ResourceType resourceType) {
        return assertSuccess(event)
                .operationType(operationType)
                .resourcePath(resourcePath)
                .resourceType(resourceType);
    }

    public AdminEventAssertion operationType(OperationType operationType) {
        Assertions.assertEquals(operationType.name(), getOperationType());
        return this;
    }

    public AdminEventAssertion auth(String expectedRealmId, String expectedClientId, String expectedUserId) {
        AuthDetailsRepresentation authDetails = event.getAuthDetails();
        Assertions.assertEquals(expectedRealmId, authDetails.getRealmId());
        Assertions.assertEquals(expectedClientId, authDetails.getClientId());
        Assertions.assertEquals(expectedUserId, authDetails.getUserId());
        return this;
    }

    public AdminEventAssertion resourceType(ResourceType expectedResourceType) {
        Assertions.assertEquals(expectedResourceType.name(), event.getResourceType());
        return this;
    }

    public AdminEventAssertion resourcePath(String... expectedResourcePath) {
        Assertions.assertEquals(String.join("/", expectedResourcePath), event.getResourcePath());
        return this;
    }

    public AdminEventAssertion representation(Object expectedRep) {
        String actualRepresentation = event.getRepresentation();
        if (expectedRep == null) {
            Assertions.assertNull(event.getRepresentation());
        } else {
            try {
                if (expectedRep instanceof List) {
                    // List of roles. All must be available in actual representation
                    List<RoleRepresentation> expectedRoles = (List<RoleRepresentation>) expectedRep;
                    List<RoleRepresentation> actualRoles = JsonSerialization.readValue(new ByteArrayInputStream(actualRepresentation.getBytes()), new TypeReference<>() {});

                    Map<String, String> expectedRolesMap = new HashMap<>();
                    for (RoleRepresentation role : expectedRoles) {
                        expectedRolesMap.put(role.getId(), role.getName());
                    }

                    Map<String, String> actualRolesMap = new HashMap<>();
                    for (RoleRepresentation role : actualRoles) {
                        actualRolesMap.put(role.getId(), role.getName());
                    }
                    Assertions.assertEquals(expectedRolesMap, actualRolesMap);

                } else if (expectedRep instanceof Map<?, ?> expectedRepMap) {
                    Map<?, ?> actualRepMap = JsonSerialization.readValue(actualRepresentation, Map.class);
                    for (Map.Entry<?, ?> entry : expectedRepMap.entrySet()) {
                        Object expectedValue = entry.getValue();
                        if (expectedValue != null) {
                            Object actualValue = actualRepMap.get(entry.getKey());
                            Assertions.assertEquals(expectedValue, actualValue, "Map item with key '" + entry.getKey() + "' not equal.");
                        }
                    }
                } else {
                    Object actualRep = JsonSerialization.readValue(actualRepresentation, expectedRep.getClass());

                    // Reflection-based comparing for other types - compare the non-null fields of "expected" representation with the "actual" representation from the event
                    for (Method method : Reflections.getAllDeclaredMethods(expectedRep.getClass())) {
                        if (method.getParameterCount() == 0 && (method.getName().startsWith("get") || method.getName().startsWith("is"))) {
                            Object expectedValue = Reflections.invokeMethod(method, expectedRep);
                            if (expectedValue != null) {
                                Object actualValue = Reflections.invokeMethod(method, actualRep);
                                Assertions.assertEquals(expectedValue, actualValue, "Property method '" + method.getName() + "' of representation not equal.");
                            }
                        }
                    }
                }
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
        return this;
    }

    private AdminEventAssertion assertEventId() {
        MatcherAssert.assertThat(event.getId(), EventMatchers.isUUID());
        return this;
    }

    private AdminEventAssertion assertValidOperationType() {
        String actualOperationType = getOperationType();
        try {
            OperationType.valueOf(actualOperationType);
        } catch (IllegalArgumentException e) {
            Assertions.fail("Unknown operation type: " + actualOperationType);
        }
        return this;
    }

    private String getOperationType() {
        return expectSuccess ? event.getOperationType() : event.getOperationType().substring(0, "_ERROR".length());
    }

}
