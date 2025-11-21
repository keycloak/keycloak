package org.keycloak.testframework.remote;

import java.util.List;

import org.keycloak.testframework.TestFrameworkExtension;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.remote.runonserver.RunOnServerSupplier;
import org.keycloak.testframework.remote.runonserver.TestClassServerSupplier;
import org.keycloak.testframework.remote.timeoffset.TimeOffsetSupplier;

public class RemoteTestFrameworkExtension implements TestFrameworkExtension {
    @Override
    public List<Supplier<?, ?>> suppliers() {
        return List.of(
                new TimeOffsetSupplier(),
                new RunOnServerSupplier(),
                new RemoteProvidersSupplier(),
                new TestClassServerSupplier()
        );
    }

    @Override
    public List<Class<?>> alwaysEnabledValueTypes() {
        return List.of(RemoteProviders.class);
    }
}
