package org.keycloak.tests.utils;

import java.util.LinkedList;

import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;

public abstract class LegacyRealmConfig implements RealmConfig {

    @Override
    public RealmConfigBuilder configure(RealmConfigBuilder realm) {
        RealmRepresentation realmRepresentation = realm.build();
        if (realmRepresentation.getUsers() == null) {
            realmRepresentation.setUsers(new LinkedList<>());
        }
        if (realmRepresentation.getClients() == null) {
            realmRepresentation.setClients(new LinkedList<>());

        }
        if (realmRepresentation.getGroups() == null) {
            realmRepresentation.setGroups(new LinkedList<>());
        }
        configureTestRealm(realmRepresentation);
        return RealmConfigBuilder.update(realmRepresentation);
    }

    public abstract void configureTestRealm(RealmRepresentation testRealm);

}
