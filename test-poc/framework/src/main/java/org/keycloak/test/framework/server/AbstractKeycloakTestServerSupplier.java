package org.keycloak.test.framework.server;

import org.keycloak.test.framework.annotations.KeycloakIntegrationTest;
import org.keycloak.test.framework.database.TestDatabase;
import org.keycloak.test.framework.injection.InstanceContext;
import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.injection.RequestedInstance;
import org.keycloak.test.framework.injection.Supplier;
import org.keycloak.test.framework.injection.SupplierHelpers;

import java.util.Collections;
import java.util.Map;

public abstract class AbstractKeycloakTestServerSupplier implements Supplier<KeycloakTestServer, KeycloakIntegrationTest> {

    @Override
    public Class<KeycloakTestServer> getValueType() {
        return KeycloakTestServer.class;
    }

    @Override
    public Class<KeycloakIntegrationTest> getAnnotationClass() {
        return KeycloakIntegrationTest.class;
    }

    @Override
    public KeycloakTestServer getValue(InstanceContext<KeycloakTestServer, KeycloakIntegrationTest> instanceContext) {
        KeycloakIntegrationTest annotation = instanceContext.getAnnotation();
        KeycloakTestServerConfig serverConfig = SupplierHelpers.getInstance(annotation.config());

        Map<String, String> databaseConfig;
        if (requiresDatabase()) {
            TestDatabase testDatabase = instanceContext.getDependency(TestDatabase.class);
            databaseConfig = testDatabase.getServerConfig();
        } else {
            databaseConfig = Collections.emptyMap();
        }

        KeycloakTestServer keycloakTestServer = getServer();
        keycloakTestServer.start(serverConfig, databaseConfig);
        return keycloakTestServer;
    }

    @Override
    public LifeCycle getDefaultLifecycle() {
        return LifeCycle.GLOBAL;
    }

    @Override
    public boolean compatible(InstanceContext<KeycloakTestServer, KeycloakIntegrationTest> a, RequestedInstance<KeycloakTestServer, KeycloakIntegrationTest> b) {
        return a.getAnnotation().config().equals(b.getAnnotation().config());
    }

    @Override
    public void close(InstanceContext<KeycloakTestServer, KeycloakIntegrationTest> instanceContext) {
        instanceContext.getValue().stop();
    }

    public abstract KeycloakTestServer getServer();

    public abstract boolean requiresDatabase();

}
