package org.keycloak.testsuite.forms;

import java.util.List;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.UserRepresentation;

public final class VerifyProfileTest {

    private VerifyProfileTest() {
    }

    public static UserRepresentation getUserByUsername(RealmResource testRealm, String username) {
        List<UserRepresentation> users = testRealm.users().search(username);
        return users != null && !users.isEmpty() ? users.get(0) : null;
    }
}
