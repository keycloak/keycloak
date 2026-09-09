package org.keycloak.services.resources.admin;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.keycloak.models.light.LightweightUserAdapter;
import org.keycloak.representations.idm.UserRepresentation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UserUpdateUtilsTest {

    @Test
    public void onlyRequiredActionsChanged_returnsTrueForRequiredActionsOnly() {
        UserRepresentation rep = new UserRepresentation();
        rep.setUsername("alice");
        rep.setFirstName("Alice");
        rep.setLastName("Example");
        rep.setEmail("alice@example.com");
        rep.setRequiredActions(List.of("UPDATE_PASSWORD"));

        LightweightUserAdapter user = new LightweightUserAdapter(null, "1");
        user.setUsername("alice");
        user.setFirstName("Alice");
        user.setLastName("Example");
        user.setEmail("alice@example.com");

        assertTrue(UserUpdateUtils.onlyRequiredActionsChanged(rep, user));
    }

    @Test
    public void onlyRequiredActionsChanged_returnsFalseWhenProfileFieldChanged() {
        UserRepresentation rep = new UserRepresentation();
        rep.setUsername("bob"); // changed username
        rep.setFirstName("Bob");
        rep.setLastName("Example");
        rep.setEmail("bob@example.com");
        rep.setRequiredActions(List.of("UPDATE_PASSWORD"));

        LightweightUserAdapter user = new LightweightUserAdapter(null, "2");
        user.setUsername("alice");
        user.setFirstName("Alice");
        user.setLastName("Example");
        user.setEmail("alice@example.com");

        assertFalse(UserUpdateUtils.onlyRequiredActionsChanged(rep, user));
    }

    @Test
    public void onlyRequiredActionsChanged_returnsFalseWhenAttributesDiffer() {
        UserRepresentation rep = new UserRepresentation();
        rep.setUsername("alice");
        rep.setFirstName("Alice");
        rep.setLastName("Example");
        rep.setEmail("alice@example.com");
        rep.setRequiredActions(List.of("UPDATE_PASSWORD"));
        rep.setAttributes(java.util.Map.of("phone", List.of("+1")));

        LightweightUserAdapter user = new LightweightUserAdapter(null, "3");
        user.setUsername("alice");
        user.setFirstName("Alice");
        user.setLastName("Example");
        user.setEmail("alice@example.com");

        assertFalse(UserUpdateUtils.onlyRequiredActionsChanged(rep, user));
    }
}
