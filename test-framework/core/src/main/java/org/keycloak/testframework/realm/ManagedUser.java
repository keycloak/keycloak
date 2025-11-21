package org.keycloak.testframework.realm;

import java.util.Optional;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
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

    public String getPassword() {
        return getPassword(createdRepresentation);
    }

    public UserResource admin() {
        return userResource;
    }

    public static String getPassword(UserRepresentation userRepresentation) {
        Optional<CredentialRepresentation> password = userRepresentation.getCredentials().stream().filter(c -> c.getType().equals(CredentialRepresentation.PASSWORD)).findFirst();
        return password.map(CredentialRepresentation::getValue).orElse(null);
    }

}
