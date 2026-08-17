package org.keycloak.testsuite.updaters;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.UserRepresentation;

public class UserAttributeUpdater implements Closeable {

    private final UserResource userResource;
    private final UserRepresentation original;
    private final UserRepresentation updated;

    public static UserAttributeUpdater forUserByUsername(Keycloak adminClient, String realm, String userName) {
        return forUserByUsername(adminClient.realm(realm), userName);
    }

    public static UserAttributeUpdater forUserByUsername(RealmResource realm, String userName) {
        List<UserRepresentation> users = realm.users().search(userName, true).stream()
                .filter(u -> userName.equalsIgnoreCase(u.getUsername()))
                .collect(Collectors.toList());
        if (users.size() != 1) {
            throw new IllegalStateException("Expected one user for " + userName + ", found " + users.size());
        }
        return new UserAttributeUpdater(realm.users().get(users.get(0).getId()));
    }

    public UserAttributeUpdater(UserResource resource) {
        this.userResource = resource;
        this.original = resource.toRepresentation();
        this.updated = resource.toRepresentation();
        if (this.updated.getAttributes() == null) {
            this.updated.setAttributes(new HashMap<>());
        }
    }

    public UserAttributeUpdater setEmailVerified(Boolean emailVerified) {
        updated.setEmailVerified(emailVerified);
        return this;
    }

    public UserAttributeUpdater setRequiredActions(UserModel.RequiredAction... requiredActions) {
        updated.setRequiredActions(Arrays.stream(requiredActions).map(Enum::name).collect(Collectors.toList()));
        return this;
    }

    public UserAttributeUpdater update() {
        userResource.update(updated);
        return this;
    }

    @Override
    public void close() throws IOException {
        userResource.update(original);
    }
}
