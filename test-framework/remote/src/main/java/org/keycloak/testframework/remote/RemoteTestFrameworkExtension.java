package org.keycloak.testframework.remote;

import org.keycloak.testframework.TestFrameworkExtension;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.runonserver.RunOnServerSupplier;
import org.keycloak.testframework.remote.timeoffset.TimeOffsetSupplier;

import java.util.List;

public class RemoteTestFrameworkExtension implements TestFrameworkExtension {
    @Override
    public List<Supplier<?, ?>> suppliers() {
        return List.of(
                new TimeOffsetSupplier(),
                new RunOnServerSupplier(),
                new RemoteProvidersSupplier()
        );
    }

    @Override
    public List<Class<?>> alwaysEnabledValueTypes() {
        return List.of(RemoteProviders.class);
    }
}
