package org.keycloak.testframework.database;

import org.keycloak.testframework.TestFrameworkExtension;
import org.keycloak.testframework.injection.Supplier;

import java.util.List;

public class OracleTestFrameworkExtension implements TestFrameworkExtension {

    @Override
    public List<Supplier<?, ?>> suppliers() {
        return List.of(new OracleDatabaseSupplier());
    }
}
