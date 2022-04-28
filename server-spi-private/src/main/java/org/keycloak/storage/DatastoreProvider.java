package org.keycloak.storage;

import org.keycloak.models.ClientProvider;
import org.keycloak.models.ClientScopeProvider;
import org.keycloak.models.GroupProvider;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleProvider;
import org.keycloak.models.UserProvider;
import org.keycloak.provider.Provider;


public interface DatastoreProvider extends Provider {

    ClientScopeProvider clientScopes();

    ClientProvider clients();

    GroupProvider groups();

    RealmProvider realms();

    RoleProvider roles();
    
    UserProvider users();

    ExportImportManager getExportImportManager();

    MigrationManager getMigrationManager();

}
