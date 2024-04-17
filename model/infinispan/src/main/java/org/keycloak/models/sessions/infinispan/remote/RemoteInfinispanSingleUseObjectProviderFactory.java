package org.keycloak.models.sessions.infinispan.remote;

import org.infinispan.client.hotrod.RemoteCache;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.SingleUseObjectProviderFactory;
import org.keycloak.models.sessions.infinispan.entities.SingleUseObjectValueEntity;

import java.lang.invoke.MethodHandles;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.ACTION_TOKEN_CACHE;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.getRemoteCache;

public class RemoteInfinispanSingleUseObjectProviderFactory implements SingleUseObjectProviderFactory<RemoteInfinispanSingleUseObjectProvider> {

    private final static Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    private volatile RemoteCache<String, SingleUseObjectValueEntity> cache;

    @Override
    public RemoteInfinispanSingleUseObjectProvider create(KeycloakSession session) {
        assert cache != null;
        return new RemoteInfinispanSingleUseObjectProvider(session, cache);
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        cache = getRemoteCache(factory, ACTION_TOKEN_CACHE);
        logger.debug("Provided initialized.");
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return InfinispanUtils.REMOTE_PROVIDER_ID;
    }

    @Override
    public int order() {
        return InfinispanUtils.PROVIDER_ORDER;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return InfinispanUtils.isRemoteInfinispan();
    }
}
