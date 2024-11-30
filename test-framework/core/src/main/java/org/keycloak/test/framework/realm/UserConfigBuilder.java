package org.keycloak.test.framework.realm;

import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

public class UserConfigBuilder {

    private final UserRepresentation rep;

    private UserConfigBuilder(UserRepresentation rep) {
        this.rep = rep;
    }

    public static UserConfigBuilder create() {
        UserRepresentation rep = new UserRepresentation();
        rep.setEnabled(true);
        return new UserConfigBuilder(rep);
    }

    public static UserConfigBuilder update(UserRepresentation rep) {
        return new UserConfigBuilder(rep);
    }

    public UserConfigBuilder username(String username) {
        rep.setUsername(username);
        return this;
    }

    public UserConfigBuilder name(String firstName, String lastName) {
        rep.setFirstName(firstName);
        rep.setLastName(lastName);
        return this;
    }

    public UserConfigBuilder email(String email) {
        rep.setEmail(email);
        return this;
    }

    public UserConfigBuilder emailVerified() {
        rep.setEmailVerified(true);
        return this;
    }

    public UserConfigBuilder password(String password) {
        rep.setCredentials(Collections.combine(rep.getCredentials(), Representations.toCredential(CredentialRepresentation.PASSWORD, password)));
        return this;
    }

    public UserConfigBuilder roles(String... roles) {
        rep.setRealmRoles(Collections.combine(rep.getRealmRoles(), roles));
        return this;
    }

    public UserConfigBuilder groups(String... groups) {
        rep.setGroups(Collections.combine(rep.getGroups(), groups));
        return this;
    }

    public UserRepresentation build() {
        return rep;
    }

}
