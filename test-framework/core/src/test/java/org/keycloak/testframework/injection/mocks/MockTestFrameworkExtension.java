package org.keycloak.testframework.injection.mocks;

import java.util.List;

import org.keycloak.testframework.TestFrameworkExtension;
import org.keycloak.testframework.injection.Supplier;

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
