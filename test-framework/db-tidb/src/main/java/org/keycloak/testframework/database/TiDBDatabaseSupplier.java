package org.keycloak.testframework.database;

import org.keycloak.common.Profile;
import org.keycloak.testframework.annotations.InjectTestDatabase;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

public class TiDBDatabaseSupplier extends AbstractDatabaseSupplier {

    @Override
    public String getAlias() {
        return "tidb";
    }

    @Override
    public KeycloakServerConfigBuilder intercept(KeycloakServerConfigBuilder serverConfig, InstanceContext<TestDatabase, InjectTestDatabase> instanceContext) {
        KeycloakServerConfigBuilder builder = super.intercept(serverConfig, instanceContext);
        builder.features(Profile.Feature.DB_TIDB);
        return builder;
    }

    @Override
    TestDatabase getTestDatabase() {
        return new TiDBTestDatabase();
    }

}
