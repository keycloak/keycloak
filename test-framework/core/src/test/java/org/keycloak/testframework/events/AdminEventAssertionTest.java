package org.keycloak.testframework.events;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AdminEventAssertionTest {

    @Test
    public void testSuccess() {
        AdminEventAssertion.assertSuccess(createEvent("CREATE"));
        Assertions.assertThrows(AssertionError.class, () -> AdminEventAssertion.assertSuccess(createEvent("CREATE_ERROR")));
        Assertions.assertThrows(AssertionError.class, () -> AdminEventAssertion.assertSuccess(createEvent("INVALID")));
    }

    @Test
    public void testError() {
        AdminEventAssertion.assertError(createEvent("CREATE_ERROR"));
        Assertions.assertThrows(AssertionError.class, () -> AdminEventAssertion.assertError(createEvent("CREATE")));
        Assertions.assertThrows(AssertionError.class, () -> AdminEventAssertion.assertError(createEvent("INVALID_ERROR")));
    }

    @Test
    public void assertRepresentation() throws IOException {
        UserRepresentation user = createUser("username", List.of("group-1", "group-2"));

        AdminEventRepresentation event = createEvent("CREATE");
        event.setRepresentation(JsonSerialization.writeValueAsString(user));

        AdminEventAssertion.assertSuccess(event).representation(user);

        Assertions.assertThrows(AssertionError.class, () ->
                AdminEventAssertion.assertSuccess(event).representation(createUser("username2", List.of("group-1", "group-2")))
        );
        Assertions.assertThrows(AssertionError.class, () ->
                AdminEventAssertion.assertSuccess(event).representation(createUser("username", List.of("group-1", "group-3")))
        );
    }

    private AdminEventRepresentation createEvent(String operation) {
        AdminEventRepresentation rep = new AdminEventRepresentation();
        rep.setId(UUID.randomUUID().toString());
        rep.setOperationType(operation);
        return rep;
    }

    private UserRepresentation createUser(String username, List<String> groups) {
        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(username);
        user.setGroups(groups);
        return user;
    }

}
