package org.keycloak.testframework.database;

import org.keycloak.testframework.annotations.InjectTestDatabase;
import org.keycloak.testframework.config.Config;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

public class RemoteDatabaseSupplier extends AbstractDatabaseSupplier {

    public static final String NAME = "remote";

    @Override
    public String getAlias() {
        return NAME;
    }

    @Override
    TestDatabase getTestDatabase() {
        return new RemoteTestDatabase();
    }

    private String getDriverDependencyArtifact() {
        return Config.getValueTypeConfig(TestDatabase.class, "driver.artifact", null, String.class);
    }

    @Override
    public KeycloakServerConfigBuilder intercept(KeycloakServerConfigBuilder serverConfig, InstanceContext<TestDatabase, InjectTestDatabase> instanceContext) {
        serverConfig = super.intercept(serverConfig, instanceContext);

        String dependencyArtifact = getDriverDependencyArtifact();
        if (dependencyArtifact != null) {
            String[] artifact = dependencyArtifact.split(":");
            if (artifact.length != 2) {
                throw new IllegalArgumentException("Invalid dependency artifact " + dependencyArtifact);
            }
            serverConfig.dependency(artifact[0], artifact[1]);
        }
        return serverConfig;
    }

}
