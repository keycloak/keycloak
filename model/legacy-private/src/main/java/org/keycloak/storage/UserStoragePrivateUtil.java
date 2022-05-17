package org.keycloak.storage;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserProvider;
import org.keycloak.storage.datastore.LegacyDatastoreProvider;

/**
 * @author Alexander Schwartz
 */
public class UserStoragePrivateUtil {
    public static UserProvider userLocalStorage(KeycloakSession session) {
        return ((LegacyDatastoreProvider) session.getProvider(DatastoreProvider.class)).userLocalStorage();
    }
}
