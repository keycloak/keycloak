package org.keycloak.test.framework.database;

import org.keycloak.test.framework.annotations.InjectTestDatabase;
import org.keycloak.test.framework.injection.InstanceContext;
import org.keycloak.test.framework.server.KeycloakServerConfigBuilder;

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
                .dependency("com.oracle.database.jdbc", "ojdbc11");
    }
}
