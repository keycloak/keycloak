package org.keycloak.testsuite.forms;

import java.util.List;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.UserRepresentation;

public final class VerifyProfileTest {

    private VerifyProfileTest() {
    }

    public static UserRepresentation getUserByUsername(RealmResource testRealm, String username) {
        List<UserRepresentation> users = testRealm.users().search(username);
        if (users != null && !users.isEmpty()) {
            return users.get(0);
        }
        return null;
    }
}
