package org.keycloak.testframework.database;

import java.util.List;

import org.keycloak.testframework.TestFrameworkExtension;
import org.keycloak.testframework.injection.Supplier;

public class PostgresTestFrameworkExtension implements TestFrameworkExtension {

    @Override
    public List<Supplier<?, ?>> suppliers() {
        return List.of(new PostgresDatabaseSupplier());
    }
}
