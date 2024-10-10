package test.org.keycloak.quarkus.deployment;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.storage.DatastoreProvider;
import org.keycloak.storage.DatastoreProviderFactory;

public class TestDatastoreFactory implements DatastoreProviderFactory {
    @Override
    public DatastoreProvider create(KeycloakSession session) {
        return new TestDatastore(null, session);
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "test";
    }
}
