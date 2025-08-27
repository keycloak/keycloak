package org.keycloak.testframework.cache;

import org.jboss.logging.Logger;

public class InfinispanLocalCacheSupplier extends AbstractCacheSupplier {

    private static final Logger LOGGER = Logger.getLogger(InfinispanLocalCacheSupplier.class);

    @Override
    public String getAlias() {
        return "local";
    }

    @Override
    public InfinispanCache getCache() {
        return new InfinispanLocalCache();
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
