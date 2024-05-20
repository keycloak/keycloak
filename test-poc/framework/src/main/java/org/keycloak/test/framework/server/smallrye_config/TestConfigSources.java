package org.keycloak.test.framework.server.smallrye_config;

import org.eclipse.microprofile.config.spi.ConfigSource;

import java.util.ArrayList;
import java.util.List;

public class TestConfigSources {

    private List<ConfigSource> configSources = new ArrayList<>();

    public TestConfigSources() {
    }

    public void addConfigSource(ConfigSource configSource) {
        this.configSources.add(configSource);
    }

    public List<ConfigSource> getConfigSources() {
        return this.configSources;
    }
}
