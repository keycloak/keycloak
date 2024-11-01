package org.keycloak.test.framework.realm;

import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

public class UserConfigBuilder {

    private final UserRepresentation representation;

    public UserConfigBuilder() {
        this.representation = new UserRepresentation();
        this.representation.setEnabled(true);
    }

    public UserConfigBuilder username(String username) {
        representation.setUsername(username);
        return this;
    }

    public UserConfigBuilder name(String firstName, String lastName) {
        representation.setFirstName(firstName);
        representation.setLastName(lastName);
        return this;
    }

    public UserConfigBuilder email(String email) {
        representation.setEmail(email);
        return this;
    }

    public UserConfigBuilder password(String password) {
        representation.setCredentials(Collections.combine(representation.getCredentials(), Representations.toCredential(CredentialRepresentation.PASSWORD, password)));
        return this;
    }

    public UserConfigBuilder roles(String... roles) {
        representation.setRealmRoles(Collections.combine(representation.getRealmRoles(), roles));
        return this;
    }

    public UserConfigBuilder groups(String... groups) {
        representation.setGroups(Collections.combine(representation.getGroups(), groups));
        return this;
    }

    public UserRepresentation build() {
        return representation;
    }

}
