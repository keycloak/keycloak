package org.keycloak.testframework.oauth;

import org.keycloak.testframework.TestFrameworkExtension;
import org.keycloak.testframework.injection.Supplier;

import java.util.List;

public class OAuthTestFrameworkExtension implements TestFrameworkExtension {

    @Override
    public List<Supplier<?, ?>> suppliers() {
        return List.of(new OAuthClientSupplier(), new TestAppSupplier(), new OAuthIdentityProviderSupplier());
    }

}
