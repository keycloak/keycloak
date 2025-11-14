package org.keycloak.testsuite.model;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.keycloak.Config;
import org.keycloak.connections.infinispan.InfinispanUtil;
import org.keycloak.marshalling.Marshalling;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.api.CacheContainerAdmin;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.configuration.cache.BackupConfiguration;
import org.infinispan.configuration.cache.BackupFailurePolicy;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.configuration.HotRodServerConfiguration;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.junit.rules.ExternalResource;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.ACTION_TOKEN_CACHE;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.USER_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.WORK_CACHE_NAME;

public class HotRodServerRule extends ExternalResource {

    private static final List<String> CACHES_NAME = List.of(
            USER_SESSION_CACHE_NAME, OFFLINE_USER_SESSION_CACHE_NAME, CLIENT_SESSION_CACHE_NAME,
            OFFLINE_CLIENT_SESSION_CACHE_NAME, LOGIN_FAILURE_CACHE_NAME, WORK_CACHE_NAME, ACTION_TOKEN_CACHE,
            AUTHENTICATION_SESSIONS_CACHE_NAME
    );

    protected HotRodServer hotRodServer;

    protected HotRodServer hotRodServer2;

    protected RemoteCacheManager remoteCacheManager;

    protected DefaultCacheManager hotRodCacheManager;

    protected DefaultCacheManager hotRodCacheManager2;

    @Override
    protected void after() {
        if (remoteCacheManager != null) {
            remoteCacheManager.stop();
        }
    }

    public void createEmbeddedHotRodServer(Config.Scope config) {
        try {
            hotRodCacheManager = new DefaultCacheManager("hotrod/hotrod1.xml");
            hotRodCacheManager2 = new DefaultCacheManager("hotrod/hotrod2.xml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        HotRodServerConfiguration build = new HotRodServerConfigurationBuilder().build();
        hotRodServer = new HotRodServer();
        hotRodServer.start(build, hotRodCacheManager);

        HotRodServerConfiguration build2 = new HotRodServerConfigurationBuilder().port(11333).build();
        hotRodServer2 = new HotRodServer();
        hotRodServer2.start(build2, hotRodCacheManager2);

        // Create a Hot Rod client
        org.infinispan.client.hotrod.configuration.ConfigurationBuilder remoteBuilder = new org.infinispan.client.hotrod.configuration.ConfigurationBuilder();
        Marshalling.configure(remoteBuilder);
        org.infinispan.client.hotrod.configuration.Configuration cfg = remoteBuilder
                .addServers(hotRodServer.getHost() + ":" + hotRodServer.getPort() + ";"
                        + hotRodServer2.getHost() + ":" + hotRodServer2.getPort()).build();
        remoteCacheManager = new RemoteCacheManager(cfg);

        // create remote keycloak caches
        createKeycloakCaches(config.getBoolean("async", false) ? CacheMode.REPL_ASYNC : CacheMode.REPL_SYNC);

        // Use Keycloak time service in remote caches
        InfinispanUtil.setTimeServiceToKeycloakTime(hotRodCacheManager);
        InfinispanUtil.setTimeServiceToKeycloakTime(hotRodCacheManager2);
    }

    private void createKeycloakCaches(CacheMode cacheMode) {
        var builder = createCacheConfigurationBuilder();
        builder.clustering().cacheMode(cacheMode);

        // cross-site configuration
        builder.sites().addBackup()
                .site("site-1")
                .backupFailurePolicy(BackupFailurePolicy.FAIL)
                .strategy(BackupConfiguration.BackupStrategy.SYNC)
                .replicationTimeout(15000);
        builder.sites().addBackup()
                .site("site-2")
                .backupFailurePolicy(BackupFailurePolicy.FAIL)
                .strategy(BackupConfiguration.BackupStrategy.SYNC)
                .replicationTimeout(15000);

        // reduce locking timeout as deadlocks are expected
        builder.locking()
                .lockAcquisitionTimeout(1, TimeUnit.SECONDS);

        // enable transactions to keep data consistent when deadlocks happen
        builder.transaction().transactionMode(TransactionMode.TRANSACTIONAL)
                .lockingMode(LockingMode.PESSIMISTIC)
                .useSynchronization(false);

        var config = builder.build();
        var admin1 = hotRodCacheManager.administration().withFlags(CacheContainerAdmin.AdminFlag.VOLATILE);
        var admin2 = hotRodCacheManager2.administration().withFlags(CacheContainerAdmin.AdminFlag.VOLATILE);
        for (String c: CACHES_NAME) {
            admin1.getOrCreateCache(c, config);
            admin2.getOrCreateCache(c, config);
        }
    }

    public static ConfigurationBuilder createCacheConfigurationBuilder() {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.encoding().mediaType(MediaType.APPLICATION_PROTOSTREAM);
        return builder;
    }

    public RemoteCacheManager getRemoteCacheManager() {
        return remoteCacheManager;
    }

    public HotRodServer getHotRodServer() {
        return hotRodServer;
    }

    public HotRodServer getHotRodServer2() {
        return hotRodServer2;
    }

    public DefaultCacheManager getHotRodCacheManager() {
        return hotRodCacheManager;
    }

    public DefaultCacheManager getHotRodCacheManager2() {
        return hotRodCacheManager2;
    }

    public Stream<DefaultCacheManager> streamCacheManagers() {
        return Stream.of(hotRodCacheManager, hotRodCacheManager2);
    }
}
