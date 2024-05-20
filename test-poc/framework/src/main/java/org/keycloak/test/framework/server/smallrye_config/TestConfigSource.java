package org.keycloak.test.framework.server.smallrye_config;

import org.eclipse.microprofile.config.spi.ConfigSource;

import java.util.Set;

public interface TestConfigSource extends ConfigSource {

    @Override
    int getOrdinal();

    @Override
    Set<String> getPropertyNames();

    @Override
    String getValue(final String propertyName);

    @Override
    String getName();
}
