package org.keycloak.testframework.realm;

import java.util.List;
import java.util.Map;

import org.keycloak.representations.idm.RoleRepresentation;

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

    public RoleConfigBuilder singleAttribute(String key, String value) {
        rep.singleAttribute(key, value);
        return this;
    }

    public RoleConfigBuilder composite(boolean enabled) {
        rep.setComposite(enabled);
        return this;
    }

    public RoleConfigBuilder realmComposite(String compositeRole) {
        if (rep.getComposites() == null) {
            rep.setComposites(new RoleRepresentation.Composites());
        }

        rep.getComposites().setRealm(Collections.combine(rep.getComposites().getRealm(), compositeRole));
        return this;
    }

    public RoleConfigBuilder clientComposite(String client, String compositeRole) {
        if (rep.getComposites() == null) {
            rep.setComposites(new RoleRepresentation.Composites());
        }
        
        rep.getComposites().setClient(Collections.combine(rep.getComposites().getClient(), client, compositeRole));
        return this;
    }

    public RoleRepresentation build() {
        return rep;
    }
}
