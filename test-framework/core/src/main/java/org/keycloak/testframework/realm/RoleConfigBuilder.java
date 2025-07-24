package org.keycloak.testframework.realm;

import org.keycloak.representations.idm.RoleRepresentation;

import java.util.List;
import java.util.Map;

public class RoleConfigBuilder {

    private final RoleRepresentation rep;

    private RoleConfigBuilder(RoleRepresentation rep) {
        this.rep = rep;
    }

    public static RoleConfigBuilder create() {
        RoleRepresentation rep = new RoleRepresentation();
        return new RoleConfigBuilder(rep);
    }

    public static RoleConfigBuilder update(RoleRepresentation rep) {
        return new RoleConfigBuilder(rep);
    }

    public RoleConfigBuilder id(String id) {
        rep.setId(id);
        return this;
    }

    public RoleConfigBuilder name(String name) {
        rep.setName(name);
        return this;
    }

    public RoleConfigBuilder description(String description) {
        rep.setDescription(description);
        return this;
    }

    public RoleConfigBuilder attributes(Map<String, List<String>> attributes) {
        rep.setAttributes(Collections.combine(rep.getAttributes(), attributes));
        return this;
    }

    public RoleRepresentation build() {
        return rep;
    }
}
