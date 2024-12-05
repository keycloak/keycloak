package org.keycloak.test.framework.remote;

import org.keycloak.test.framework.TestFrameworkExtension;
import org.keycloak.test.framework.injection.Supplier;
import org.keycloak.test.framework.remote.timeoffset.TimeOffsetSupplier;

import java.util.List;

public class RemoteTestFrameworkExtension implements TestFrameworkExtension {
    @Override
    public List<Supplier<?, ?>> suppliers() {
        return List.of(
                new TimeOffsetSupplier(),
                new RemoteProvidersSupplier()
        );
    }

    @Override
    public List<Class<?>> alwaysEnabledValueTypes() {
        return List.of(RemoteProviders.class);
    }
}
