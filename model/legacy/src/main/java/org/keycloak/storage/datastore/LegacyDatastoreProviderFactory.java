package org.keycloak.storage.datastore;

import org.keycloak.Config;
import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.storage.DatastoreProvider;
import org.keycloak.storage.DatastoreProviderFactory;

public class LegacyDatastoreProviderFactory implements DatastoreProviderFactory {

    private static final String PROVIDER_ID = "legacy";
    private long clientStorageProviderTimeout;
    private long roleStorageProviderTimeout;

    @Override
    public DatastoreProvider create(KeycloakSession session) {
        return new LegacyDatastoreProvider(this, session);
    }

    @Override
    public void init(Scope config) {
        clientStorageProviderTimeout = Config.scope("client").getLong("storageProviderTimeout", 3000L);
        roleStorageProviderTimeout = Config.scope("role").getLong("storageProviderTimeout", 3000L);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
    
    public long getClientStorageProviderTimeout() {
        return clientStorageProviderTimeout;
    }

    public long getRoleStorageProviderTimeout() {
        return roleStorageProviderTimeout;
    }

}
