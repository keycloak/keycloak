package org.keycloak.testframework.database;

import org.keycloak.testframework.annotations.InjectTestDatabase;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

public class OracleDatabaseSupplier extends AbstractDatabaseSupplier {

    @Override
    public String getAlias() {
        return OracleTestDatabase.NAME;
    }

    @Override
    TestDatabase getTestDatabase() {
        return new OracleTestDatabase();
    }

    @Override
    public KeycloakServerConfigBuilder intercept(KeycloakServerConfigBuilder serverConfig, InstanceContext<TestDatabase, InjectTestDatabase> instanceContext) {
        return super.intercept(serverConfig, instanceContext)
                .dependency("com.oracle.database.jdbc", "ojdbc17");
    }
}
