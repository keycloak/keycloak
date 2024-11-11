package org.keycloak.test.framework.database;

import org.keycloak.test.framework.TestFrameworkExtension;
import org.keycloak.test.framework.injection.Supplier;

import java.util.List;

public class OracleTestFrameworkExtension implements TestFrameworkExtension {

    @Override
    public List<Supplier<?, ?>> suppliers() {
        return List.of(new OracleDatabaseSupplier());
    }
}
