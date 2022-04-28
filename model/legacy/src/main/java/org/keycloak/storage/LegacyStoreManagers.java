package org.keycloak.storage;

import org.keycloak.models.ClientProvider;
import org.keycloak.models.ClientScopeProvider;
import org.keycloak.models.GroupProvider;
import org.keycloak.models.RoleProvider;
import org.keycloak.models.UserProvider;
import org.keycloak.storage.federated.UserFederatedStorageProvider;

public interface LegacyStoreManagers {
    
    ClientProvider clientStorageManager();

    ClientScopeProvider clientScopeStorageManager();

    RoleProvider roleStorageManager();

    GroupProvider groupStorageManager();

    UserProvider userStorageManager();

    UserProvider userLocalStorage();

    UserFederatedStorageProvider userFederatedStorage();
}
