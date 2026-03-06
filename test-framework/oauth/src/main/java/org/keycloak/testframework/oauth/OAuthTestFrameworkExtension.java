package org.keycloak.testframework.oauth;

import java.util.List;

import org.keycloak.testframework.TestFrameworkExtension;
import org.keycloak.testframework.injection.Supplier;

import com.google.auto.service.AutoService;

@AutoService(TestFrameworkExtension.class)
public class OAuthTestFrameworkExtension implements TestFrameworkExtension {

    @Override
    public List<Supplier<?, ?>> suppliers() {
        return List.of(new OAuthClientSupplier(), new TestAppSupplier(), new OAuthIdentityProviderSupplier(), new CimdProviderSupplier());
    }

}
