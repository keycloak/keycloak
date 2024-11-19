package org.keycloak.test.framework.oauth.nimbus;

import org.keycloak.test.framework.TestFrameworkExtension;
import org.keycloak.test.framework.injection.Supplier;

import java.util.List;

public class NimbusTestFrameworkExtension implements TestFrameworkExtension {

    @Override
    public List<Supplier<?, ?>> suppliers() {
        return List.of(new OAuthClientSupplier());
    }

}
