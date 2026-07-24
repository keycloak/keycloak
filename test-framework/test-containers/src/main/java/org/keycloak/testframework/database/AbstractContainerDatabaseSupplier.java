package org.keycloak.testframework.database;

import org.keycloak.testframework.annotations.InjectTestDatabase;
import org.keycloak.testframework.config.Config;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.server.KeycloakServer;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

public abstract class AbstractContainerDatabaseSupplier extends AbstractDatabaseSupplier {

    @Override
    public KeycloakServerConfigBuilder intercept(KeycloakServerConfigBuilder serverConfig, InstanceContext<TestDatabase, InjectTestDatabase> instanceContext) {
        serverConfig = super.intercept(serverConfig, instanceContext);

        // If both KeycloakServer and TestDatabase run in container, we need to configure Keycloak with internal url that is accessible within docker network.
        // Right now it's supported by the cluster server mode only.
        if ("cluster".equals(Config.getSelectedSupplier(KeycloakServer.class)) &&
                instanceContext.getValue() instanceof AbstractContainerTestDatabase containerDatabase) {
            return serverConfig.options(containerDatabase.serverConfig(true));
        } else {
            return serverConfig;
        }
    }
}
