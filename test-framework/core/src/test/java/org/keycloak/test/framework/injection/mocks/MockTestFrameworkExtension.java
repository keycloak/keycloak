package org.keycloak.test.framework.injection.mocks;

import org.keycloak.test.framework.TestFrameworkExtension;
import org.keycloak.test.framework.injection.Supplier;

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
