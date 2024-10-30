package org.keycloak.test.framework.realm;

import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;

import java.util.Arrays;

public class RealmConfigBuilder {

    private final RealmRepresentation representation;

    public RealmConfigBuilder() {
        this.representation = new RealmRepresentation();
        this.representation.setEnabled(true);
    }

    public RealmConfigBuilder name(String name) {
        representation.setRealm(name);
        return this;
    }

    public RealmConfigBuilder roles(String... roleNames) {
        if (representation.getRoles() == null) {
            representation.setRoles(new RolesRepresentation());
        }
        representation.getRoles().setRealm(Collections.combine(representation.getRoles().getRealm(), Arrays.stream(roleNames).map(Representations::toRole)));
        return this;
    }

    public RealmConfigBuilder groups(String... groupsNames) {
        representation.setGroups(Collections.combine(representation.getGroups(), Arrays.stream(groupsNames).map(Representations::toGroup)));
        return this;
    }

    public RealmRepresentation build() {
        return representation;
    }

}
