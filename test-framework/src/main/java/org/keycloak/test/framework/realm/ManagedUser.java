package org.keycloak.test.framework.realm;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.Optional;

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

    public String getPassword() {
        Optional<CredentialRepresentation> password = createdRepresentation.getCredentials().stream().filter(c -> c.getType().equals(CredentialRepresentation.PASSWORD)).findFirst();
        return password.map(CredentialRepresentation::getValue).orElse(null);
    }

    public UserResource admin() {
        return userResource;
    }

}
