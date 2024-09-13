package org.keycloak.test.framework.server;

import org.keycloak.test.framework.annotations.KeycloakIntegrationTest;
import org.keycloak.test.framework.config.Config;
import org.keycloak.test.framework.database.TestDatabase;
import org.keycloak.test.framework.events.SysLogServer;
import org.keycloak.test.framework.injection.InstanceContext;
import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.injection.RequestedInstance;
import org.keycloak.test.framework.injection.Supplier;
import org.keycloak.test.framework.injection.SupplierHelpers;

import java.util.LinkedList;
import java.util.List;

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

        List<String> rawOptions = new LinkedList<>();
        rawOptions.add("start-dev");
        rawOptions.add("--cache=local");

        rawOptions.add("--bootstrap-admin-client-id=" + Config.getAdminClientId());
        rawOptions.add("--bootstrap-admin-client-secret=" + Config.getAdminClientSecret());

        if (serverConfig.enableSysLog()) {
            SysLogServer sysLogServer = instanceContext.getDependency(SysLogServer.class);

            rawOptions.add("--log=console,syslog");
            rawOptions.add("--log-syslog-endpoint=" + sysLogServer.getEndpoint());
            rawOptions.add("--spi-events-listener-jboss-logging-success-level=INFO");
        }

        if (!serverConfig.features().isEmpty()) {
            rawOptions.add("--features=" + String.join(",", serverConfig.features()));
        }

        serverConfig.options().forEach((key, value) -> rawOptions.add("--" + key + "=" + value));

        if (requiresDatabase()) {
            TestDatabase testDatabase = instanceContext.getDependency(TestDatabase.class);
            testDatabase.getServerConfig().forEach((key, value) -> rawOptions.add("--" + key + "=" + value));
        }

        KeycloakTestServer server = getServer();
        server.start(rawOptions);
        return server;
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
