package org.keycloak.testframework.realm;

import org.keycloak.representations.idm.GroupRepresentation;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class GroupConfigBuilder {
    private final GroupRepresentation rep;

    private GroupConfigBuilder(GroupRepresentation rep) {
        this.rep = rep;
    }

    public static GroupConfigBuilder create() {
        GroupRepresentation rep = new GroupRepresentation();
        return new GroupConfigBuilder(rep);
    }

    public static GroupConfigBuilder update(GroupRepresentation rep) {
        return new GroupConfigBuilder(rep);
    }

    public GroupConfigBuilder name(String name) {
        rep.setName(name);
        return this;
    }

    public GroupConfigBuilder path(String path) {
        rep.setPath(path);
        return this;
    }

    public GroupConfigBuilder realmRoles(String... realmRoles) {
        if (rep.getRealmRoles() == null) {
            rep.setRealmRoles(new LinkedList<>());
        }
        rep.getRealmRoles().addAll(List.of(realmRoles));
        return this;
    }

    public GroupConfigBuilder clientRole(String client, String... clientRoles) {
        if (rep.getClientRoles() == null) {
            rep.setClientRoles(new HashMap<>());
        }
        rep.getClientRoles().put(client, List.of(clientRoles));
        return this;
    }

    public GroupConfigBuilder attribute(String name, String... values) {
        rep.setAttributes(Collections.combine(rep.getAttributes(), name, values));
        return this;
    }

    public GroupConfigBuilder setAttributes(Map<String, List<String>> attributes) {
        rep.setAttributes(attributes);
        return this;
    }
//
//    public GroupConfigBuilder singleAttribute(String name, String value) {
//        rep.singleAttribute(name, value);
//        return this;
//    }

    public GroupConfigBuilder subGroups(GroupRepresentation... subGroups) {
        if (rep.getSubGroups() == null) {
            rep.setSubGroups(new LinkedList<>());
        }
        rep.getSubGroups().addAll(List.of(subGroups));
        return this;
    }

    public GroupRepresentation build() {
        return rep;
    }
}
