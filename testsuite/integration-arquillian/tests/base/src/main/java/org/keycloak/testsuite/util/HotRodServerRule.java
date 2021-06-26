package org.keycloak.testsuite.util;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.jboss.marshalling.core.JBossUserMarshaller;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.configuration.HotRodServerConfiguration;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;
import org.junit.rules.ExternalResource;
import org.keycloak.Config;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.connections.infinispan.TopologyInfo;

import java.util.concurrent.TimeUnit;

import static org.keycloak.models.sessions.infinispan.util.InfinispanUtil.configureTransport;
import static org.keycloak.models.sessions.infinispan.util.InfinispanUtil.createCacheConfigurationBuilder;
import static org.keycloak.models.sessions.infinispan.util.InfinispanUtil.getActionTokenCacheConfig;

public class HotRodServerRule extends ExternalResource {

    protected HotRodServer hotRodServer;

    protected RemoteCacheManager remoteCacheManager;

    protected DefaultCacheManager hotrodCacheManager;

    public void createEmbeddedHotRodServer(Config.Scope config) {
        TopologyInfo topologyInfo = new TopologyInfo(hotrodCacheManager, config, true);

        GlobalConfigurationBuilder gcb = new GlobalConfigurationBuilder();
        String jgroupsUdpMcastAddr = config.get("jgroupsUdpMcastAddr", System.getProperty(InfinispanConnectionProvider.JGROUPS_UDP_MCAST_ADDR));
        configureTransport(gcb, topologyInfo.getMyNodeName()+"-jdg", "jdg1", jgroupsUdpMcastAddr,
                "infinispan/hotrod-jgroups-udp.xml", HotRodServerRule.class.getClassLoader());
        gcb.jmx().domain(InfinispanConnectionProvider.JMX_DOMAIN + "-" + topologyInfo.getMyNodeName()+"-jdg").enable();

        // For Infinispan 10, we go with the JBoss marshalling.
        // TODO: This should be replaced later with the marshalling recommended by infinispan. Probably protostream.
        // See https://infinispan.org/docs/stable/titles/developing/developing.html#marshalling for the details
        gcb.serialization().marshaller(new JBossUserMarshaller());
        // create a new cache manager
        hotrodCacheManager = new DefaultCacheManager(gcb.build());

        HotRodServerConfiguration build = new HotRodServerConfigurationBuilder().build();
        hotRodServer = new HotRodServer();
        hotRodServer.start(build, hotrodCacheManager);

        // Create a Hot Rod client
        org.infinispan.client.hotrod.configuration.ConfigurationBuilder remoteBuilder = new org.infinispan.client.hotrod.configuration.ConfigurationBuilder();
        remoteBuilder.marshaller(new JBossUserMarshaller());
        org.infinispan.client.hotrod.configuration.Configuration cfg = remoteBuilder.addServers(hotRodServer.getHost()).build();
        remoteCacheManager = new RemoteCacheManager(cfg);

        boolean async = config.getBoolean("async", false);

        ConfigurationBuilder sessionConfigBuilder = createCacheConfigurationBuilder();
        Configuration sessionCacheConfigurationBase = sessionConfigBuilder.build();

        String sessionsMode = config.get("sessionsMode", "distributed");
        if (sessionsMode.equalsIgnoreCase("replicated")) {
            sessionConfigBuilder.clustering().cacheMode(async ? CacheMode.REPL_ASYNC : CacheMode.REPL_SYNC);
        } else if (sessionsMode.equalsIgnoreCase("distributed")) {
            sessionConfigBuilder.clustering().cacheMode(async ? CacheMode.DIST_ASYNC : CacheMode.DIST_SYNC);
        } else {
            throw new RuntimeException("Invalid value for sessionsMode");
        }

        int owners = config.getInt("sessionsOwners", 2);

        int l1Lifespan = config.getInt("l1Lifespan", 600000);
        boolean l1Enabled = l1Lifespan > 0;
        sessionConfigBuilder.clustering()
                .hash()
                .numOwners(owners)
                .numSegments(config.getInt("sessionsSegments", 60))
                .l1()
                .enabled(l1Enabled)
                .lifespan(l1Lifespan)
                .build();

        // create again all keycloak caches
        sessionConfigBuilder = createCacheConfigurationBuilder();
        sessionConfigBuilder.read(sessionCacheConfigurationBase);
        Configuration sessionCacheConfiguration = sessionConfigBuilder.build();
        hotrodCacheManager.defineConfiguration(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME, sessionCacheConfiguration);

        sessionConfigBuilder = createCacheConfigurationBuilder();
        sessionConfigBuilder.read(sessionCacheConfigurationBase);
        sessionCacheConfiguration = sessionConfigBuilder.build();
        hotrodCacheManager.defineConfiguration(InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME, sessionCacheConfiguration);

        sessionConfigBuilder = createCacheConfigurationBuilder();
        sessionConfigBuilder.read(sessionCacheConfigurationBase);
        sessionCacheConfiguration = sessionConfigBuilder.build();
        hotrodCacheManager.defineConfiguration(InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME, sessionCacheConfiguration);

        sessionConfigBuilder = createCacheConfigurationBuilder();
        sessionConfigBuilder.read(sessionCacheConfigurationBase);
        sessionCacheConfiguration = sessionConfigBuilder.build();
        hotrodCacheManager.defineConfiguration(InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME, sessionCacheConfiguration);

        sessionConfigBuilder = createCacheConfigurationBuilder();
        sessionConfigBuilder.read(sessionCacheConfigurationBase);
        sessionCacheConfiguration = sessionConfigBuilder.build();
        hotrodCacheManager.defineConfiguration(InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME, sessionCacheConfiguration);

        sessionCacheConfiguration = sessionConfigBuilder.build();
        hotrodCacheManager.defineConfiguration(InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME, sessionCacheConfiguration);

        ConfigurationBuilder replicationConfigBuilder = createCacheConfigurationBuilder();

        replicationConfigBuilder.clustering().cacheMode(async ? CacheMode.REPL_ASYNC : CacheMode.REPL_SYNC);

        Configuration replicationEvictionCacheConfiguration = replicationConfigBuilder
                .expiration().enableReaper().wakeUpInterval(15, TimeUnit.SECONDS)
                .build();
        hotrodCacheManager.defineConfiguration(InfinispanConnectionProvider.WORK_CACHE_NAME, replicationEvictionCacheConfiguration);

        final ConfigurationBuilder actionTokenCacheConfigBuilder = getActionTokenCacheConfig();

        actionTokenCacheConfigBuilder.clustering().cacheMode(async ? CacheMode.REPL_ASYNC : CacheMode.REPL_SYNC);

        hotrodCacheManager.defineConfiguration(InfinispanConnectionProvider.ACTION_TOKEN_CACHE, actionTokenCacheConfigBuilder.build());

        hotrodCacheManager.getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME, true);
        hotrodCacheManager.getCache(InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME, true);
        hotrodCacheManager.getCache(InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME, true);
        hotrodCacheManager.getCache(InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME, true);
        hotrodCacheManager.getCache(InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME, true);
        hotrodCacheManager.getCache(InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME, true);
        hotrodCacheManager.getCache(InfinispanConnectionProvider.WORK_CACHE_NAME, true);
        hotrodCacheManager.getCache(InfinispanConnectionProvider.ACTION_TOKEN_CACHE, true);

    }

    public RemoteCacheManager getRemoteCacheManager() {
        return remoteCacheManager;
    }

    public HotRodServer getHotRodServer() {
        return hotRodServer;
    }
}
