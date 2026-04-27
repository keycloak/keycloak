package org.keycloak.testframework.realm;

import java.util.List;
import java.util.Map;

import org.keycloak.representations.idm.RoleRepresentation;

public class RoleBuilder extends Builder<RoleRepresentation> {

    private RoleBuilder(RoleRepresentation rep) {
        super(rep);
    }

    public static RoleBuilder create() {
        RoleRepresentation rep = new RoleRepresentation();
        return new RoleBuilder(rep);
    }

    public static RoleBuilder update(RoleRepresentation rep) {
        return new RoleBuilder(rep);
    }

    public RoleBuilder id(String id) {
        rep.setId(id);
        return this;
    }

    public RoleBuilder name(String name) {
        rep.setName(name);
        return this;
    }

    public RoleBuilder description(String description) {
        rep.setDescription(description);
        return this;
    }

    public RoleBuilder attribute(String key, String value) {
        rep.singleAttribute(key, value);
        return this;
    }

    public RoleBuilder attributes(Map<String, List<String>> attributes) {
        rep.setAttributes(combine(rep.getAttributes(), attributes));
        return this;
    }

    public RoleBuilder composite(boolean enabled) {
        rep.setComposite(enabled);
        return this;
    }

    public RoleBuilder realmComposite(String... compositeRole) {
        rep.setComposites(createIfNull(rep.getComposites(), RoleRepresentation.Composites::new));
        rep.getComposites().setRealm(combine(rep.getComposites().getRealm(), compositeRole));
        return this;
    }

    public RoleBuilder clientComposite(String client, String... compositeRole) {
        rep.setComposites(createIfNull(rep.getComposites(), RoleRepresentation.Composites::new));
        rep.getComposites().setClient(combine(rep.getComposites().getClient(), client, compositeRole));
        return this;
    }

}
