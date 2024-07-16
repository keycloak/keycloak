package org.keycloak.test.framework.database;

public class PostgresDatabaseSupplier extends AbstractDatabaseSupplier {

    @Override
    TestDatabase getTestDatabase() {
        DatabaseConfig databaseConfig = new DatabaseConfig()
                .vendor("postgres")
                .username("keycloak")
                .password("keycloak")
                .containerImage("the-postgres-container:the-version");
        return new TestDatabase(databaseConfig);
    }

}
