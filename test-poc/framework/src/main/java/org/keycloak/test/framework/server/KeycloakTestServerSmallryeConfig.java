package org.keycloak.test.framework.server;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.keycloak.test.framework.server.smallrye_config.DefaultTestConfigSource;
import org.keycloak.test.framework.server.smallrye_config.KeycloakTestServerConfigMapping;
import org.keycloak.test.framework.server.smallrye_config.TestConfigSource;
import org.keycloak.test.framework.server.smallrye_config.TestConfigSources;
import org.keycloak.test.framework.server.smallrye_config.TestOption;

import java.util.Map;
import java.util.Set;

public class KeycloakTestServerSmallryeConfig {

    private SmallRyeConfig config;

    public KeycloakTestServerSmallryeConfig(TestConfigSource configSource) {
        TestConfigSources configSources = new TestConfigSources();
        if (!configSource.getClass().getName().equals(new DefaultTestConfigSource().getName())) {
            configSources.addConfigSource(new DefaultTestConfigSource());
        }
        configSources.addConfigSource(configSource);

        this.config = new SmallRyeConfigBuilder().addDefaultSources().addDefaultInterceptors()
                .withSources(configSources.getConfigSources())
                .withMapping(KeycloakTestServerConfigMapping.class)
                .build();
    }

    public String getServerType() {
        return this.config.getConfigMapping(KeycloakTestServerConfigMapping.class).server().orElse(TestOption.DEFAULT_SERVER_VAL);
    }

    public Set<String> getServerFeatures() {
        return this.config.getConfigMapping(KeycloakTestServerConfigMapping.class).features().orElse(TestOption.DEFAULT_FEATURES_VAL);
    }

    public Map<String, String> getServerOptions() {
        return this.config.getConfigMapping(KeycloakTestServerConfigMapping.class).options();
    }
}
