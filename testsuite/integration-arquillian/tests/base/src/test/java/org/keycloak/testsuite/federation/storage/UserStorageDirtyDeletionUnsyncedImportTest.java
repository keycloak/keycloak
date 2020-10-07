package org.keycloak.testsuite.federation.storage;

import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.storage.UserStorageProvider.EditMode;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;

import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

/**
 *
 * @author hmlnarik
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public final class UserStorageDirtyDeletionUnsyncedImportTest extends AbstractUserStorageDirtyDeletionTest {

    @Override
    protected ComponentRepresentation getFederationProvider() {
        return getFederationProvider(EditMode.UNSYNCED, true);
    }

}
