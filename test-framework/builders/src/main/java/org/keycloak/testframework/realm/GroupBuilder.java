package org.keycloak.testframework.realm;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.keycloak.representations.idm.GroupRepresentation;

public class GroupBuilder extends Builder<GroupRepresentation> {

    private GroupBuilder(GroupRepresentation rep) {
        super(rep);
    }

    public static GroupBuilder create() {
        return new GroupBuilder(new GroupRepresentation());
    }

    public static GroupBuilder create(String name) {
        return create().name(name);
    }

    public static GroupBuilder update(GroupRepresentation rep) {
        return new GroupBuilder(rep);
    }

    public GroupBuilder name(String name) {
        rep.setName(name);
        return this;
    }

    public GroupBuilder path(String path) {
        rep.setPath(path);
        return this;
    }

    public GroupBuilder realmRoles(String... realmRoles) {
        rep.setRealmRoles(combine(rep.getRealmRoles(), realmRoles));
        return this;
    }

    public GroupBuilder clientRoles(String client, String... clientRoles) {
        rep.setClientRoles(combine(rep.getClientRoles(), client, clientRoles));
        return this;
    }

    public GroupBuilder attribute(String key, String... value) {
        rep.setAttributes(combine(rep.getAttributes(), key, value));
        return this;
    }

    public GroupBuilder setAttributes(Map<String, List<String>> attributes) {
        rep.setAttributes(attributes);
        return this;
    }

    public GroupBuilder subGroups(GroupRepresentation... subGroups) {
        rep.setSubGroups(combine(rep.getSubGroups(), subGroups));
        return this;
    }

    public GroupBuilder subGroups(GroupBuilder... subGroups) {
        rep.setSubGroups(combine(rep.getSubGroups(), subGroups));
        return this;
    }

    public GroupBuilder subGroups(String... subGroups) {
        return subGroups(Arrays.stream(subGroups).map(GroupBuilder::create).toArray(GroupBuilder[]::new));
    }

}
