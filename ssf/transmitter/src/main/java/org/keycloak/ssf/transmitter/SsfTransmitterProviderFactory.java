package org.keycloak.ssf.transmitter;

import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderFactory;

public interface SsfTransmitterProviderFactory extends ProviderFactory<SsfTransmitterProvider>, EnvironmentDependentProviderFactory {

    @Override
    default void close() {
        // NOOP
    }

}
