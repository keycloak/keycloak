package org.keycloak.test.framework.database;

public class DevFileDatabaseSupplier extends AbstractDatabaseSupplier {

    @Override
    TestDatabase getTestDatabase() {
        DatabaseConfig databaseConfig = new DatabaseConfig().vendor("dev-file");
        return new TestDatabase(databaseConfig);
    }

}
