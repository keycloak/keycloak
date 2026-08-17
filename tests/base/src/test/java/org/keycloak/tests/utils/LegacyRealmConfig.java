package org.keycloak.tests.utils;

import java.util.LinkedList;
import java.util.List;

import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;

public abstract class LegacyRealmConfig implements RealmConfig {

    @Override
    public RealmBuilder configure(RealmBuilder realm) {
        RealmRepresentation realmRepresentation = realm.build();
        realmRepresentation.setRealm("test");
        realmRepresentation.setId("test");
        realmRepresentation.setEnabled(true);
        realmRepresentation.setSslRequired("external");
        realmRepresentation.setRegistrationAllowed(true);
        realmRepresentation.setResetPasswordAllowed(true);
        realmRepresentation.setEditUsernameAllowed(true);
        realmRepresentation.setLoginWithEmailAllowed(true);
        if (realmRepresentation.getUsers() == null) {
            realmRepresentation.setUsers(new LinkedList<>());
        }
        addDefaultUserIfMissing(realmRepresentation, "test-user@localhost", "Tom", "Brady");
        addDefaultUserIfMissing(realmRepresentation, "john-doh@localhost", "John", "Doh");

        if (realmRepresentation.getClients() == null) {
            realmRepresentation.setClients(new LinkedList<>());
        }
        if (realmRepresentation.getGroups() == null) {
            realmRepresentation.setGroups(new LinkedList<>());
        }
        configureTestRealm(realmRepresentation);
        return RealmBuilder.update(realmRepresentation);
    }

    private void addDefaultUserIfMissing(RealmRepresentation realm, String username, String firstName, String lastName) {
        boolean exists = realm.getUsers().stream()
                .anyMatch(u -> username.equals(u.getUsername()) || username.equals(u.getEmail()));
        if (exists) {
            return;
        }

        CredentialRepresentation password = new CredentialRepresentation();
        password.setType(CredentialRepresentation.PASSWORD);
        password.setValue("password");
        password.setTemporary(false);

        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(username);
        user.setEmail(username);
        user.setEmailVerified(false);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setCredentials(List.of(password));
        user.setRealmRoles(List.of("user", "offline_access"));
        realm.getUsers().add(user);
    }

    public abstract void configureTestRealm(RealmRepresentation testRealm);

}
