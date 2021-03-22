package org.keycloak.models.sessions.infinispan.initializer;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.lookup.EmbeddedTransactionManagerLookup;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Ignore
public class ConcurrencyLockingTest {

    @Test
    public void testLocking() throws Exception {
        final DefaultCacheManager cacheManager = getVersionedCacheManager();
        Cache<String, String> cache = cacheManager.getCache("COUNTER_CACHE");

        Map<String, String> map = new HashMap<>();
        map.put("key1", "val1");
        map.put("key2", "val2");
        cache.putAll(map);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Cache<String, String> cache = cacheManager.getCache("COUNTER_CACHE");
                cache.startBatch();
                System.out.println("thread lock");
                cache.getAdvancedCache().lock("key");
                try {
                    Thread.sleep(100000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                cache.endBatch(true);

            }
        });
        Thread.sleep(10);
        cache.startBatch();
        cache.getAdvancedCache().lock("key");
        cache.put("key", "1234");
        System.out.println("after put");
        cache.endBatch(true);

        Thread.sleep(1000000);



    }

    protected DefaultCacheManager getVersionedCacheManager() {
        GlobalConfigurationBuilder gcb = new GlobalConfigurationBuilder();


        boolean allowDuplicateJMXDomains = true;

        gcb.globalJmxStatistics().allowDuplicateDomains(allowDuplicateJMXDomains);

        final DefaultCacheManager cacheManager = new DefaultCacheManager(gcb.build());
        ConfigurationBuilder invalidationConfigBuilder = new ConfigurationBuilder();
        Configuration invalidationCacheConfiguration = invalidationConfigBuilder.build();
        cacheManager.defineConfiguration(InfinispanConnectionProvider.REALM_CACHE_NAME, invalidationCacheConfiguration);

        ConfigurationBuilder counterConfigBuilder = new ConfigurationBuilder();
        counterConfigBuilder.invocationBatching().enable();
        counterConfigBuilder.transaction().transactionManagerLookup(new EmbeddedTransactionManagerLookup());
        counterConfigBuilder.transaction().lockingMode(LockingMode.PESSIMISTIC);
        Configuration counterCacheConfiguration = counterConfigBuilder.build();

        cacheManager.defineConfiguration("COUNTER_CACHE", counterCacheConfiguration);
        return cacheManager;
    }

}
