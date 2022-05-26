package org.keycloak.models.map.datastore;

import org.keycloak.models.ClientProvider;
import org.keycloak.models.ClientScopeProvider;
import org.keycloak.models.GroupProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleProvider;
import org.keycloak.models.UserProvider;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.storage.DatastoreProvider;
import org.keycloak.storage.ExportImportManager;
import org.keycloak.storage.MigrationManager;

public class MapDatastoreProvider implements DatastoreProvider {

    private final KeycloakSession session;

    public MapDatastoreProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void close() {
    }

    @Override
    public ClientScopeProvider clientScopes() {
        return session.getProvider(ClientScopeProvider.class);
    }

    @Override
    public ClientProvider clients() {
        return session.getProvider(ClientProvider.class);
    }

    @Override
    public GroupProvider groups() {
        return session.getProvider(GroupProvider.class);
    }

    @Override
    public RealmProvider realms() {
        return session.getProvider(RealmProvider.class);
    }

    @Override
    public RoleProvider roles() {
        return session.getProvider(RoleProvider.class);
    }

    @Override
    public UserProvider users() {
        return session.getProvider(UserProvider.class);
    }

    @Override
    public ExportImportManager getExportImportManager() {
        return new MapExportImportManager(session);
    }

    @Override
    public MigrationManager getMigrationManager() {
        return new MigrationManager() {
            @Override
            public void migrate() {
                // Do not migrate the datasources
            }

            @Override
            public void migrate(RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
                // Do not migrate the representation: assume it is the latest
            }
        };
    }

}
