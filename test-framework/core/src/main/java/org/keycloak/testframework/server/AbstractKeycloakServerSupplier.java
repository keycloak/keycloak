package org.keycloak.testframework.server;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.config.Config;
import org.keycloak.testframework.database.TestDatabase;
import org.keycloak.testframework.https.ManagedCertificates;
import org.keycloak.testframework.infinispan.InfinispanServer;
import org.keycloak.testframework.injection.AbstractInterceptorHelper;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.injection.Registry;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.injection.SupplierHelpers;
import org.keycloak.testframework.injection.SupplierOrder;

import org.jboss.logging.Logger;

public abstract class AbstractKeycloakServerSupplier implements Supplier<KeycloakServer, KeycloakIntegrationTest> {

    @Override
    public KeycloakServer getValue(InstanceContext<KeycloakServer, KeycloakIntegrationTest> instanceContext) {
        KeycloakIntegrationTest annotation = instanceContext.getAnnotation();
        KeycloakServerConfig serverConfig = SupplierHelpers.getInstance(annotation.config());

        KeycloakServerConfigBuilder command = KeycloakServerConfigBuilder.startDev()
                .bootstrapAdminClient(Config.getAdminClientId(), Config.getAdminClientSecret())
                .bootstrapAdminUser(Config.getAdminUsername(), Config.getAdminPassword());

        command.log().handlers(KeycloakServerConfigBuilder.LogHandlers.CONSOLE);

        String supplierConfig = Config.getSupplierConfig(KeycloakServer.class);
        if (supplierConfig != null) {
            KeycloakServerConfig serverConfigOverride = SupplierHelpers.getInstance(supplierConfig);
            serverConfigOverride.configure(command);
        }

        command = serverConfig.configure(command);

        // Database startup and Keycloak connection setup
        if (requiresDatabase()) {
            instanceContext.getDependency(TestDatabase.class);
        }

        // External Infinispan startup and Keycloak connection setup
        if (command.isExternalInfinispanEnabled()) {
            instanceContext.getDependency(InfinispanServer.class);
        }

        ServerConfigInterceptorHelper interceptor = new ServerConfigInterceptorHelper(instanceContext.getRegistry());
        command = interceptor.intercept(command, instanceContext);

        if (command.tlsEnabled()) {
            ManagedCertificates managedCert = instanceContext.getDependency(ManagedCertificates.class);
            command.option("https-key-store-file", managedCert.getKeycloakServerKeyStorePath());
            command.option("https-key-store-password", managedCert.getKeycloakServerKeyStorePassword());
        }

        command.log().fromConfig(Config.getConfig());

        getLogger().info("Starting Keycloak test server");
        if (getLogger().isDebugEnabled()) {
            getLogger().debugv("Startup command and options: \n\t{0}", String.join("\n\t", command.toArgs()));
        }

        long start = System.currentTimeMillis();

        KeycloakServer server = getServer();
        server.start(command);

        getLogger().infov("Keycloak test server started in {0} ms", System.currentTimeMillis() - start);

        return server;
    }

    @Override
    public LifeCycle getDefaultLifecycle() {
        return LifeCycle.GLOBAL;
    }

    @Override
    public boolean compatible(InstanceContext<KeycloakServer, KeycloakIntegrationTest> a, RequestedInstance<KeycloakServer, KeycloakIntegrationTest> b) {
        return a.getAnnotation().config().equals(b.getAnnotation().config());
    }

    @Override
    public void close(InstanceContext<KeycloakServer, KeycloakIntegrationTest> instanceContext) {
        instanceContext.getValue().stop();
    }

    public abstract KeycloakServer getServer();

    public abstract boolean requiresDatabase();

    public abstract Logger getLogger();

    @Override
    public int order() {
        return SupplierOrder.KEYCLOAK_SERVER;
    }

    private static class ServerConfigInterceptorHelper extends AbstractInterceptorHelper<KeycloakServerConfigInterceptor, KeycloakServerConfigBuilder> {

        private ServerConfigInterceptorHelper(Registry registry) {
            super(registry, KeycloakServerConfigInterceptor.class);
        }

        @Override
        public KeycloakServerConfigBuilder intercept(KeycloakServerConfigBuilder value, Supplier<?, ?> supplier, InstanceContext<?, ?> existingInstance) {
            if (supplier instanceof KeycloakServerConfigInterceptor keycloakServerConfigInterceptor) {
                value = keycloakServerConfigInterceptor.intercept(value, existingInstance);
            }
            return value;
        }
    }

}
