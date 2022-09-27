package org.keycloak.testsuite.model;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.configuration.cache.BackupConfiguration;
import org.infinispan.configuration.cache.BackupFailurePolicy;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.jboss.marshalling.commons.GenericJBossMarshaller;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.configuration.HotRodServerConfiguration;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;
import org.junit.rules.ExternalResource;
import org.keycloak.Config;
import org.keycloak.connections.infinispan.InfinispanUtil;
import org.keycloak.models.map.storage.hotRod.common.HotRodUtils;

import java.io.IOException;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.ACTION_TOKEN_CACHE;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.USER_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.WORK_CACHE_NAME;

public class HotRodServerRule extends ExternalResource {

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
        remoteBuilder.marshaller(new GenericJBossMarshaller());
        org.infinispan.client.hotrod.configuration.Configuration cfg = remoteBuilder
                .addServers(hotRodServer.getHost() + ":" + hotRodServer.getPort() + ";"
                        + hotRodServer2.getHost() + ":" + hotRodServer2.getPort()).build();
        remoteCacheManager = new RemoteCacheManager(cfg);

        boolean async = config.getBoolean("async", false);

        // create remote keycloak caches
        createKeycloakCaches(async, USER_SESSION_CACHE_NAME, OFFLINE_USER_SESSION_CACHE_NAME, CLIENT_SESSION_CACHE_NAME,
                OFFLINE_CLIENT_SESSION_CACHE_NAME, LOGIN_FAILURE_CACHE_NAME, WORK_CACHE_NAME, ACTION_TOKEN_CACHE);

        getCaches(USER_SESSION_CACHE_NAME, OFFLINE_USER_SESSION_CACHE_NAME, CLIENT_SESSION_CACHE_NAME, OFFLINE_CLIENT_SESSION_CACHE_NAME,
                LOGIN_FAILURE_CACHE_NAME, WORK_CACHE_NAME, ACTION_TOKEN_CACHE);

        // Use Keycloak time service in remote caches
        InfinispanUtil.setTimeServiceToKeycloakTime(hotRodCacheManager);
        InfinispanUtil.setTimeServiceToKeycloakTime(hotRodCacheManager2);
    }

    private void getCaches(String... cache) {
        for (String c: cache) {
            hotRodCacheManager.getCache(c, true);
            hotRodCacheManager2.getCache(c, true);
        }
    }

    private void createKeycloakCaches(boolean async, String... cache) {
        ConfigurationBuilder sessionConfigBuilder1 = createCacheConfigurationBuilder();
        ConfigurationBuilder sessionConfigBuilder2 = createCacheConfigurationBuilder();
        sessionConfigBuilder1.clustering().cacheMode(async ? CacheMode.REPL_ASYNC: CacheMode.REPL_SYNC);
        sessionConfigBuilder2.clustering().cacheMode(async ? CacheMode.REPL_ASYNC: CacheMode.REPL_SYNC);

        sessionConfigBuilder1.sites().addBackup()
                .site("site-2").backupFailurePolicy(BackupFailurePolicy.IGNORE).strategy(BackupConfiguration.BackupStrategy.SYNC)
                .replicationTimeout(15000).enabled(true);
        sessionConfigBuilder2.sites().addBackup()
                .site("site-1").backupFailurePolicy(BackupFailurePolicy.IGNORE).strategy(BackupConfiguration.BackupStrategy.SYNC)
                .replicationTimeout(15000).enabled(true);

        Configuration sessionCacheConfiguration1 = sessionConfigBuilder1.build();
        Configuration sessionCacheConfiguration2 = sessionConfigBuilder2.build();
        for (String c: cache) {
            hotRodCacheManager.defineConfiguration(c, sessionCacheConfiguration1);
            hotRodCacheManager2.defineConfiguration(c, sessionCacheConfiguration2);
        }
    }

    public static ConfigurationBuilder createCacheConfigurationBuilder() {
        ConfigurationBuilder builder = new ConfigurationBuilder();

        // need to force the encoding to application/x-jboss-marshalling to avoid unnecessary conversion of keys/values. See WFLY-14356.
        builder.encoding().mediaType(MediaType.APPLICATION_JBOSS_MARSHALLING_TYPE);

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
}
