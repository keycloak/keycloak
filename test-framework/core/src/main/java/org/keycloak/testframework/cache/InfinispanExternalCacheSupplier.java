package org.keycloak.testframework.cache;

import org.jboss.logging.Logger;

public class InfinispanExternalCacheSupplier extends AbstractCacheSupplier {

    private static final Logger LOGGER = Logger.getLogger(InfinispanExternalCacheSupplier.class);

    @Override
    public String getAlias() {
        return "external";
    }

    @Override
    public InfinispanCache getCache() {
        return new InfinispanExternalCache();
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
