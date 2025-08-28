package org.keycloak.testframework.cache;

import org.jboss.logging.Logger;
import org.keycloak.testframework.annotations.InjectInfinispanCache;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.injection.SupplierOrder;

public abstract class AbstractCacheSupplier implements Supplier<InfinispanCache, InjectInfinispanCache> {

    @Override
    public InfinispanCache getValue(InstanceContext<InfinispanCache, InjectInfinispanCache> instanceContext) {
        InfinispanCache cache = getCache();
        cache.start();
        return cache;
    }

    @Override
    public void close(InstanceContext<InfinispanCache, InjectInfinispanCache> instanceContext) {
        instanceContext.getValue().stop();
    }

    @Override
    public boolean compatible(InstanceContext<InfinispanCache, InjectInfinispanCache> a, RequestedInstance<InfinispanCache, InjectInfinispanCache> b) {
        return a.getSupplier().getRef(a.getAnnotation()).equals(b.getSupplier().getRef(a.getAnnotation()));
    }

    @Override
    public int order() {
        return SupplierOrder.BEFORE_KEYCLOAK_SERVER;
    }

    public abstract InfinispanCache getCache();

    public abstract Logger getLogger();
}
