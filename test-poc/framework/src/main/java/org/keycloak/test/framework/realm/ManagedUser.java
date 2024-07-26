package org.keycloak.test.framework.realm;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;

public class ManagedUser {

    private final UserRepresentation createdRepresentation;

    private final UserResource userResource;

    public ManagedUser(UserRepresentation createdRepresentation, UserResource userResource) {
        this.createdRepresentation = createdRepresentation;
        this.userResource = userResource;
    }

    public String getId() {
        return createdRepresentation.getId();
    }

    public String getUsername() {
        return createdRepresentation.getUsername();
    }

    public UserResource admin() {
        return userResource;
    }

}
