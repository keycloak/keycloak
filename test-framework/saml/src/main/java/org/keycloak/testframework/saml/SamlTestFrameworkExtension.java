package org.keycloak.testframework.saml;

import java.util.List;

import org.keycloak.testframework.TestFrameworkExtension;
import org.keycloak.testframework.injection.Supplier;

public class SamlTestFrameworkExtension implements TestFrameworkExtension {

    @Override
    public List<Supplier<?, ?>> suppliers() {
        return List.of(
            new SamlClientSupplier(),
            new TestSamlAppSupplier()
        );
    }
}
