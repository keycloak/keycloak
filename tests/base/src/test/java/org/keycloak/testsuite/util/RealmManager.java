package org.keycloak.testsuite.util;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;

public final class RealmManager {

    private RealmManager() {
    }

    public static RealmUpdater realm(RealmResource resource) {
        return new RealmUpdater(resource);
    }

    public static final class RealmUpdater {
        private final RealmResource realm;
        private final RealmRepresentation rep;

        private RealmUpdater(RealmResource realm) {
            this.realm = realm;
            this.rep = realm.toRepresentation();
        }

        public RealmUpdater passwordPolicy(String policy) {
            rep.setPasswordPolicy(policy);
            realm.update(rep);
            return this;
        }
    }
}
