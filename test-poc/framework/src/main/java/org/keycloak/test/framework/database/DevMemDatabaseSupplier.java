package org.keycloak.test.framework.database;

public class DevMemDatabaseSupplier extends AbstractDatabaseSupplier {

    @Override
    TestDatabase getTestDatabase() {
        DatabaseConfig databaseConfig = new DatabaseConfig().vendor("dev-mem");
        return new TestDatabase(databaseConfig);
    }

}
