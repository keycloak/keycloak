package org.keycloak.storage;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;

/**
 * Manage importing and updating of realms for the legacy store.
 *
 * @author Alexander Schwartz
 */
public interface ExportImportManager {
    void importRealm(RealmRepresentation rep, RealmModel newRealm, boolean skipUserDependent);

    void updateRealm(RealmRepresentation rep, RealmModel realm);
}
