package org.keycloak.runtime;

import java.util.List;
import java.util.Map;

import org.keycloak.connections.liquibase.FastServiceLocator;
import org.keycloak.connections.liquibase.KeycloakLogger;

import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigProviderResolver;
import liquibase.logging.LogFactory;
import liquibase.servicelocator.ServiceLocator;

@Recorder
public class KeycloakRecorder {

    public static final SmallRyeConfig CONFIG;

    static {
        CONFIG = (SmallRyeConfig) SmallRyeConfigProviderResolver.instance().getConfig();
    }

    public void configureLiquibase(Map<String, List<String>> services) {
        LogFactory.setInstance(new LogFactory() {
            KeycloakLogger logger = new KeycloakLogger();

            @Override
            public liquibase.logging.Logger getLog(String name) {
                return logger;
            }

            @Override
            public liquibase.logging.Logger getLog() {
                return logger;
            }
        });
        ServiceLocator.setInstance(new FastServiceLocator(services));
    }
}
