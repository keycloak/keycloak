package org.keycloak.testsuite.util;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>.
 */
public class RealmManager {

    private static RealmResource realm;

    private RealmManager() {
    }

    public static RealmManager realm(RealmResource realm) {
        RealmManager.realm = realm;
        return new RealmManager();
    }

    public void accessCodeLifeSpan(Integer accessCodeLifespan) {
        RealmRepresentation realmRepresentation = realm.toRepresentation();
        realmRepresentation.setAccessCodeLifespan(accessCodeLifespan);
        realm.update(realmRepresentation);
    }
}