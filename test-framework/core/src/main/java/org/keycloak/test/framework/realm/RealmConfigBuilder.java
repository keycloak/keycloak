package org.keycloak.test.framework.realm;

import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;

import java.util.Arrays;

public class RealmConfigBuilder {

    private final RealmRepresentation rep;

    private RealmConfigBuilder(RealmRepresentation rep) {
        this.rep = rep;
    }

    public static RealmConfigBuilder create() {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setEnabled(true);
        return new RealmConfigBuilder(rep);
    }

    public static RealmConfigBuilder update(RealmRepresentation rep) {
        return new RealmConfigBuilder(rep);
    }

    public RealmConfigBuilder name(String name) {
        rep.setRealm(name);
        return this;
    }

    public RealmConfigBuilder registrationEmailAsUsername(boolean registrationEmailAsUsername) {
        rep.setRegistrationEmailAsUsername(registrationEmailAsUsername);
        return this;
    }

    public RealmConfigBuilder roles(String... roleNames) {
        if (rep.getRoles() == null) {
            rep.setRoles(new RolesRepresentation());
        }
        rep.getRoles().setRealm(Collections.combine(rep.getRoles().getRealm(), Arrays.stream(roleNames).map(Representations::toRole)));
        return this;
    }

    public RealmConfigBuilder groups(String... groupsNames) {
        rep.setGroups(Collections.combine(rep.getGroups(), Arrays.stream(groupsNames).map(Representations::toGroup)));
        return this;
    }

    public RealmRepresentation build() {
        return rep;
    }

}
