package org.keycloak.testframework.infinispan;

import org.keycloak.testframework.annotations.InjectInfinispanServer;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.injection.SupplierOrder;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakServerConfigInterceptor;

import org.jboss.logging.Logger;

public class InfinispanExternalServerSupplier implements Supplier<InfinispanServer, InjectInfinispanServer>, KeycloakServerConfigInterceptor<InfinispanServer, InjectInfinispanServer> {

    private static final Logger LOGGER = Logger.getLogger(InfinispanExternalServerSupplier.class);

    @Override
    public InfinispanServer getValue(InstanceContext<InfinispanServer, InjectInfinispanServer> instanceContext) {
        InfinispanServer server = InfinispanExternalServer.create();
        getLogger().info("Starting Infinispan Server");

        long start = System.currentTimeMillis();

        server.start();

        getLogger().infov("Infinispan server started in {0} ms", System.currentTimeMillis() - start);
        return server;
    }

    @Override
    public void close(InstanceContext<InfinispanServer, InjectInfinispanServer> instanceContext) {
        instanceContext.getValue().stop();
    }

    @Override
    public boolean compatible(InstanceContext<InfinispanServer, InjectInfinispanServer> a, RequestedInstance<InfinispanServer, InjectInfinispanServer> b) {
        return a.getSupplier().getRef(a.getAnnotation()).equals(b.getSupplier().getRef(a.getAnnotation()));
    }

    @Override
    public int order() {
        return SupplierOrder.BEFORE_KEYCLOAK_SERVER;
    }

    @Override
    public KeycloakServerConfigBuilder intercept(KeycloakServerConfigBuilder config, InstanceContext<InfinispanServer, InjectInfinispanServer> instanceContext) {
        InfinispanServer ispnServer = instanceContext.getValue();

        return config.options(ispnServer.serverConfig());
    }

    public Logger getLogger() {
        return LOGGER;
    }
}
