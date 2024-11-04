package org.keycloak.federation.scim.core.service;

import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class KeycloakDao {

    private final KeycloakSession keycloakSession;

    public KeycloakDao(KeycloakSession keycloakSession) {
        this.keycloakSession = keycloakSession;
    }

    private KeycloakSession getKeycloakSession() {
        return keycloakSession;
    }

    private RealmModel getRealm() {
        return getKeycloakSession().getContext().getRealm();
    }

    public boolean groupExists(KeycloakId groupId) {
        GroupModel group = getKeycloakSession().groups().getGroupById(getRealm(), groupId.asString());
        return group != null;
    }

    public boolean userExists(KeycloakId userId) {
        UserModel user = getUserById(userId);
        return user != null;
    }

    public UserModel getUserById(KeycloakId userId) {
        return getKeycloakSession().users().getUserById(getRealm(), userId.asString());
    }

    public GroupModel getGroupById(KeycloakId groupId) {
        return getKeycloakSession().groups().getGroupById(getRealm(), groupId.asString());
    }

    public Stream<GroupModel> getGroupsStream() {
        return getKeycloakSession().groups().getGroupsStream(getRealm());
    }

    public GroupModel createGroup(String displayName) {
        return getKeycloakSession().groups().createGroup(getRealm(), displayName);
    }

    public Set<KeycloakId> getGroupMembers(GroupModel groupModel) {
        return getKeycloakSession().users().getGroupMembersStream(getRealm(), groupModel).map(UserModel::getId)
                .map(KeycloakId::new).collect(Collectors.toSet());
    }

    public Stream<UserModel> getUsersStream() {
        return getKeycloakSession().users().searchForUserStream(getRealm(), Collections.emptyMap());
    }

    public UserModel getUserByUsername(String username) {
        return getKeycloakSession().users().getUserByUsername(getRealm(), username);
    }

    public UserModel getUserByEmail(String email) {
        return getKeycloakSession().users().getUserByEmail(getRealm(), email);
    }

    public UserModel addUser(String username) {
        return getKeycloakSession().users().addUser(getRealm(), username);
    }

}
