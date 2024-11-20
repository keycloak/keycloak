package org.keycloak.test.framework.database;

import org.keycloak.test.framework.annotations.InjectTestDatabase;
import org.keycloak.test.framework.injection.InstanceContext;
import org.keycloak.test.framework.server.KeycloakTestServerConfigBuilder;

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
    public void decorate(Object object, InstanceContext<TestDatabase, InjectTestDatabase> instanceContext) {
        super.decorate(object, instanceContext);
        if (object instanceof KeycloakTestServerConfigBuilder serverConfig) {
            serverConfig.dependency("com.oracle.database.jdbc", "ojdbc11");
        }
    }
}
