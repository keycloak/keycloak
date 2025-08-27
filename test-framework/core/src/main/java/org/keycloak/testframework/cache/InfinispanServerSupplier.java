package org.keycloak.testframework.cache;

import org.jboss.logging.Logger;
import org.keycloak.testframework.annotations.InjectCacheDeployment;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.injection.SupplierOrder;

public class InfinispanServerSupplier implements Supplier<CacheDeployment, InjectCacheDeployment> {

    private static final Logger LOGGER = Logger.getLogger(InfinispanServerSupplier.class);

    @Override
    public CacheDeployment getValue(InstanceContext<CacheDeployment, InjectCacheDeployment> instanceContext) {
        CacheDeployment server = new InfinispanServer();
        getLogger().info("Starting Infinispan Server");

        long start = System.currentTimeMillis();

        server.start();

        getLogger().infov("Infinispan server started in {0} ms", System.currentTimeMillis() - start);
        return server;
    }

    @Override
    public void close(InstanceContext<CacheDeployment, InjectCacheDeployment> instanceContext) {
        instanceContext.getValue().stop();
    }

    @Override
    public boolean compatible(InstanceContext<CacheDeployment, InjectCacheDeployment> a, RequestedInstance<CacheDeployment, InjectCacheDeployment> b) {
        return a.getSupplier().getRef(a.getAnnotation()).equals(b.getSupplier().getRef(a.getAnnotation()));
    }

    @Override
    public int order() {
        return SupplierOrder.BEFORE_REALM;
    }

    public Logger getLogger() {
        return LOGGER;
    }
}
