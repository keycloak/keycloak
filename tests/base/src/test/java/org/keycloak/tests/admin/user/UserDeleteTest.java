package org.keycloak.tests.admin.user;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import static org.junit.Assert.assertEquals;

@KeycloakIntegrationTest
public class UserDeleteTest extends AbstractUserTest {

    @Test
    public void delete() {
        String userId = createUser();
        deleteUser(userId);
    }

    @Test
    public void deleteNonExistent() {
        try (Response response = managedRealm.admin().users().delete("does-not-exist")) {
            assertEquals(404, response.getStatus());
        }
        Assertions.assertNull(adminEvents.poll());
    }
}
