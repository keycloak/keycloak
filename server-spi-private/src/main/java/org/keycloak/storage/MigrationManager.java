package org.keycloak.storage;

import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;

/**
 * Handle the migration of the datastore and an imported realm representation.
 * Will eventually be handled by the store directly.
 *
 * @author Alexander Schwartz
 */
@Deprecated
public interface MigrationManager {

    void migrate();

    void migrate(RealmModel realm, RealmRepresentation rep, boolean skipUserDependent);
}
