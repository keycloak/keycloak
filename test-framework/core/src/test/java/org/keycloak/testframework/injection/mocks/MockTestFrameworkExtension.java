package org.keycloak.testframework.injection.mocks;

import org.keycloak.testframework.TestFrameworkExtension;
import org.keycloak.testframework.injection.Supplier;

import java.util.List;

public class MockTestFrameworkExtension implements TestFrameworkExtension {

    @Override
    public List<Supplier<?, ?>> suppliers() {
        return List.of(
                new MockParentSupplier(),
                new MockParent2Supplier(),
                new MockChildSupplier()
        );
    }

}
