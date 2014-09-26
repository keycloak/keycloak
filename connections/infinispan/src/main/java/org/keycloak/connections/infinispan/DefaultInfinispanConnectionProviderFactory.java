package org.keycloak.connections.infinispan;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;

import javax.naming.InitialContext;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultInfinispanConnectionProviderFactory implements InfinispanConnectionProviderFactory {

    protected static final Logger logger = Logger.getLogger(DefaultInfinispanConnectionProviderFactory.class);

    private Config.Scope config;

    private EmbeddedCacheManager cacheManager;

    private boolean containerManaged;

    @Override
    public InfinispanConnectionProvider create(KeycloakSession session) {
        lazyInit();

        return new DefaultInfinispanConnectionProvider(cacheManager);
    }

    @Override
    public void close() {
        if (cacheManager != null && !containerManaged) {
            cacheManager.stop();
        }
        cacheManager = null;
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
                        initContainerManaged(cacheContainer);
                    } else {
                        initEmbedded();
                    }
                }
            }
        }
    }

    private void initContainerManaged(String cacheContainerLookup) {
        try {
            cacheManager = (EmbeddedCacheManager) new InitialContext().lookup(cacheContainerLookup);
            containerManaged = true;

            logger.debugv("Using container managed Infinispan cache container, lookup={1}", cacheContainerLookup);
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve cache container", e);
        }
    }

    private void initEmbedded() {
        GlobalConfigurationBuilder gcb = new GlobalConfigurationBuilder();
        if (config.getBoolean("transport", false)) {
            gcb.transport().defaultTransport();
        }
        cacheManager = new DefaultCacheManager(gcb.build());
        containerManaged = false;

        logger.debug("Started embedded Infinispan cache container");

        cacheManager.defineConfiguration("sessions", createConfiguration("sessions"));
        cacheManager.defineConfiguration("realms", createConfiguration("realms"));
    }

    private Configuration createConfiguration(String cacheName) {
        Config.Scope cacheConfig = config.scope("caches", cacheName);
        ConfigurationBuilder cb = new ConfigurationBuilder();

        String cacheMode = cacheConfig.get("cacheMode", "local");
        boolean async = cacheConfig.getBoolean("async", false);

        if (cacheMode.equalsIgnoreCase("replicated")) {
            cb.clustering().cacheMode(async ? CacheMode.REPL_ASYNC : CacheMode.REPL_SYNC);
        } else if (cacheMode.equalsIgnoreCase("distributed")) {
            cb.clustering().cacheMode(async ? CacheMode.DIST_ASYNC : CacheMode.DIST_SYNC);

            int owners = cacheConfig.getInt("owners", 2);
            int segments = cacheConfig.getInt("segments", 60);

            cb.clustering().hash().numOwners(owners).numSegments(segments);
        } else if (cacheMode.equalsIgnoreCase("invalidation")) {
            cb.clustering().cacheMode(async ? CacheMode.INVALIDATION_ASYNC : CacheMode.INVALIDATION_SYNC);
        } else if (!cacheMode.equalsIgnoreCase("local")) {
            throw new RuntimeException("Invalid cache mode " + cacheMode);
        }

        logger.debugv("Configured cache {0} with mode={1}, async={2}", cacheName, cacheMode, async);

        return cb.build();
    }

}
