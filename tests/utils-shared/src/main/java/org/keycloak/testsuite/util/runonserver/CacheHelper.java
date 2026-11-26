package org.keycloak.testsuite.util.runonserver;

import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.testframework.remote.providers.runonserver.FetchOnServer;
import org.keycloak.testframework.remote.providers.runonserver.FetchOnServerWrapper;

public final class CacheHelper {

    public static FetchOnServerWrapper<Boolean> contains(String cacheName, String id) {
        return new FetchOnServerWrapper<>() {

            @Override
            public FetchOnServer getRunOnServer() {
                return session -> {
                    InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);
                    return provider.getCache(cacheName).containsKey(id);
                };
            }

            @Override
            public Class<Boolean> getResultClass() {
                return Boolean.class;
            }
        };
    }

    public static FetchOnServerWrapper<Integer> size(String cacheName) {
        return new FetchOnServerWrapper<>() {

            @Override
            public FetchOnServer getRunOnServer() {
                return session -> {
                    InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);
                    return provider.getCache(cacheName).size();
                };
            }

            @Override
            public Class<Integer> getResultClass() {
                return Integer.class;
            }
        };
    }
}
