package org.keycloak.models.sessions.infinispan.initializer;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntriesEvicted;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryInvalidated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntriesEvictedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryInvalidatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;

/**
 * Just a simple test to see how distributed caches work
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Ignore
public class ClusteredCacheBehaviorTest {
    public EmbeddedCacheManager createManager() {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("jgroups.tcp.port", "53715");
        GlobalConfigurationBuilder gcb = new GlobalConfigurationBuilder();

        boolean clustered = true;
        boolean async = false;
        boolean allowDuplicateJMXDomains = true;

        if (clustered) {
            gcb = gcb.clusteredDefault();
            gcb.transport().clusterName("test-clustering");
        }
        gcb.globalJmxStatistics().allowDuplicateDomains(allowDuplicateJMXDomains);

        EmbeddedCacheManager cacheManager = new DefaultCacheManager(gcb.build());


        ConfigurationBuilder invalidationConfigBuilder = new ConfigurationBuilder();
        if (clustered) {
            invalidationConfigBuilder.clustering().cacheMode(async ? CacheMode.INVALIDATION_ASYNC : CacheMode.INVALIDATION_SYNC);
        }
        Configuration invalidationCacheConfiguration = invalidationConfigBuilder.build();

        cacheManager.defineConfiguration(InfinispanConnectionProvider.REALM_CACHE_NAME, invalidationCacheConfiguration);
        return cacheManager;

    }

    @Listener
    public static class CacheListener {
        String name;

        public CacheListener(String name) {
            this.name = name;
        }


        @CacheEntryCreated
        public void created(CacheEntryCreatedEvent event) {

            System.out.println("Listener '" + name + "' entry created  " + event.getKey() + " isPre: " + event.isPre());
        }

        @CacheEntryRemoved
        public void removed(CacheEntryRemovedEvent<String, Object> event) {
            System.out.println("Listener '" + name + "' entry removed  isPre: " + event.isPre());
        }

        @CacheEntryInvalidated
        public void removed(CacheEntryInvalidatedEvent<String, Object> event) {
            System.out.println("Listener '" + name + "' entry invalidated: isPre: " + event.isPre());
        }

        @CacheEntriesEvicted
        public void evicted(CacheEntriesEvictedEvent<String, Object> event) {
            System.out.println("Listener '" + name + "' entry evicted isPre: " + event.isPre());

        }

    }

    @Test
    public void testListener() throws Exception {
        EmbeddedCacheManager node1 = createManager();
        EmbeddedCacheManager node2 = createManager();
        Cache<String, Object> node1Cache = node1.getCache(InfinispanConnectionProvider.REALM_CACHE_NAME);
        node1Cache.addListener(new CacheListener("node1"));
        Cache<String, Object> node2Cache = node2.getCache(InfinispanConnectionProvider.REALM_CACHE_NAME);
        node2Cache.addListener(new CacheListener("node2"));

        System.out.println("node1 create entry");
        node1Cache.put("key", "node1");

        System.out.println("node1 create entry");
        node1Cache.put("key", "node111");

        System.out.println("node2 create entry");
        node2Cache.put("key", "node2");

        System.out.println("node1 remove entry");
        node1Cache.remove("key");

        System.out.println("node2 remove entry");
        node2Cache.remove("key");

        System.out.println("node2 put entry");
        node2Cache.put("key", "node2");
        System.out.println("node2 evict entry");
        node2Cache.evict("key");
        System.out.println("node1/node2 putExternal entry");
        node1Cache.putForExternalRead("key", "common");
        node2Cache.putForExternalRead("key", "common");
        System.out.println("node2 remove entry");
        node2Cache.remove("key");
        System.out.println("node1 remove entry");
        node1Cache.remove("key");

        // test remove non-existing node 2, existing node 1
        System.out.println("Test non existent remove");
        System.out.println("node1 create entry");
        node1Cache.put("key", "value");
        System.out.println("node2 remove non-existent entry");
        System.out.println("exists?: " + node2Cache.containsKey("key"));
        node2Cache.remove("key");

        // test clear
        System.out.println("Test clear cache");
        System.out.println("add key to node 1, key2 to node2");
        node1Cache.putForExternalRead("key", "value");
        node2Cache.putForExternalRead("key", "value");
        node2Cache.putForExternalRead("key2", "value");
        System.out.println("Clear from node1");
        node1Cache.clear();
        System.out.println("node 2 exists key2?: " + node2Cache.containsKey("key2"));
        System.out.println("node 2 exists key?: " + node2Cache.containsKey("key"));



    }
}
