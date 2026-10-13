package org.keycloak.testframework.infinispan;

import java.util.List;

import org.keycloak.testframework.TestFrameworkExtension;
import org.keycloak.testframework.injection.Supplier;

public class InfinispanExternalServerTestFrameworkExtension implements TestFrameworkExtension {

    @Override
    public List<Supplier<?, ?>> suppliers() {
        return List.of(new InfinispanExternalServerSupplier());
    }
}
