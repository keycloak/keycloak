package org.keycloak.connections.infinispan;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;

import javax.naming.InitialContext;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultInfinispanConnectionProviderFactory implements InfinispanConnectionProviderFactory {

    private Config.Scope config;

    private EmbeddedCacheManager cacheManager;

    private boolean managed;

    @Override
    public InfinispanConnectionProvider create(KeycloakSession session) {
        lazyInit();

        return new DefaultInfinispanConnectionProvider(cacheManager);
    }

    @Override
    public void close() {
        if (cacheManager != null && managed) {
            cacheManager.stop();
            cacheManager = null;
        }
    }

    @Override
    public String getId() {
        return "default";
    }

    @Override
    public void init(Config.Scope config) {
        this.config = config;
    }

    private void lazyInit() {
        if (cacheManager == null) {
            synchronized (this) {
                if (cacheManager == null) {
                    String cacheContainer = config.get("cacheContainer");
                    if (cacheContainer != null) {
                        try {
                            cacheManager = (EmbeddedCacheManager) new InitialContext().lookup(cacheContainer);
                            managed = false;
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to retrieve cache container", e);
                        }
                    } else {
                        GlobalConfigurationBuilder gcb = new GlobalConfigurationBuilder();
                        ConfigurationBuilder cb = new ConfigurationBuilder();

                        CacheMode cacheMode = getCacheMode(config.get("cacheMode", "local"), config.getBoolean("async", false));
                        if (!cacheMode.equals(CacheMode.LOCAL)) {
                            gcb.transport().defaultTransport();

                            int owners = config.getInt("owners", 2);
                            int segments = config.getInt("segments", 60);

                            cb.clustering().cacheMode(cacheMode).hash().numOwners(owners).numSegments(segments);
                        }

                        cacheManager = new DefaultCacheManager(gcb.build(), cb.build());
                        managed = true;
                    }
                }
            }
        }
    }

    private CacheMode getCacheMode(String cacheModeString, boolean async) {
        if (cacheModeString.equalsIgnoreCase("local")) {
            return CacheMode.LOCAL;
        }

        if (cacheModeString.equalsIgnoreCase("replicated")) {
            return async ? CacheMode.REPL_ASYNC : CacheMode.REPL_SYNC;
        }

        if (cacheModeString.equalsIgnoreCase("distributed")) {
            return async ? CacheMode.DIST_ASYNC : CacheMode.DIST_SYNC;
        }

        throw new RuntimeException("Invalid cacheMode " + cacheModeString);
    }

}
