package org.keycloak.testframework.mail;

import java.util.List;

import org.keycloak.testframework.TestFrameworkExtension;
import org.keycloak.testframework.injection.Supplier;

import com.google.auto.service.AutoService;

@AutoService(TestFrameworkExtension.class)
public class GreenMailTestFrameworkExtension implements TestFrameworkExtension {

    @Override
    public List<Supplier<?, ?>> suppliers() {
        return List.of(new GreenMailSupplier());
    }

}
