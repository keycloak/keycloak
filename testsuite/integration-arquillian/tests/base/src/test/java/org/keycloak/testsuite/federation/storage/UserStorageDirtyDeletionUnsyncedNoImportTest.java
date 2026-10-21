package org.keycloak.testsuite.federation.storage;

import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.storage.UserStorageProvider.EditMode;

/**
 *
 * @author hmlnarik
 */
public final class UserStorageDirtyDeletionUnsyncedNoImportTest extends AbstractUserStorageDirtyDeletionTest {

    @Override
    protected ComponentRepresentation getFederationProvider() {
        return getFederationProvider(EditMode.UNSYNCED, false);
    }

}
