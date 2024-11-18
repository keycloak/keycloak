package org.keycloak.test.framework.server;

import io.quarkus.maven.dependency.Dependency;
import org.jboss.logging.Logger;
import org.keycloak.test.framework.annotations.KeycloakIntegrationTest;
import org.keycloak.test.framework.config.Config;
import org.keycloak.test.framework.database.TestDatabase;
import org.keycloak.test.framework.events.SysLogServer;
import org.keycloak.test.framework.injection.InstanceContext;
import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.injection.RequestedInstance;
import org.keycloak.test.framework.injection.Supplier;
import org.keycloak.test.framework.injection.SupplierHelpers;

import java.util.HashSet;
import java.util.Set;

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

        CommandBuilder command = CommandBuilder.startDev()
                .cache("local")
                .bootstrapAdminClient(Config.getAdminClientId(), Config.getAdminClientSecret());

        if (serverConfig.enableSysLog()) {
            SysLogServer sysLogServer = instanceContext.getDependency(SysLogServer.class);
            command.log().enableSyslog(sysLogServer.getEndpoint());
        }

        command.log().fromConfig(Config.getConfig());

        if (!serverConfig.features().isEmpty()) {
            command.features(serverConfig.features());
        }

        command.options(serverConfig.options());

        Set<Dependency> dependencies = new HashSet<>(serverConfig.dependencies());

        if (requiresDatabase()) {
            TestDatabase testDatabase = instanceContext.getDependency(TestDatabase.class);
            command.databaseConfig(testDatabase.serverConfig());

            Dependency jdbcDriver = testDatabase.jdbcDriver();
            if (jdbcDriver != null) {
                dependencies.add(jdbcDriver);
            }
        }

        getLogger().info("Starting Keycloak test server");
        if (getLogger().isDebugEnabled()) {
            getLogger().debugv("Startup command and options: \n\t{0}", String.join("\n\t", command.toArgs()));
        }

        long start = System.currentTimeMillis();

        KeycloakTestServer server = getServer();
        server.start(command, dependencies);

        getLogger().infov("Keycloak test server started in {0} ms", System.currentTimeMillis() - start);

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

    public abstract Logger getLogger();

}
