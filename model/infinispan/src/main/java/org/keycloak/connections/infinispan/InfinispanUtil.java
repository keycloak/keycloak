/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.connections.infinispan;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.ProtocolVersion;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.time.TimeService;
import org.infinispan.commons.util.FileLookup;
import org.infinispan.commons.util.FileLookupFactory;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.global.TransportConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.eviction.EvictionType;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.factories.impl.BasicComponentRegistry;
import org.infinispan.factories.impl.ComponentRef;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.persistence.manager.PersistenceManager;
import org.infinispan.persistence.remote.RemoteStore;
import org.infinispan.remoting.transport.Transport;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.infinispan.util.EmbeddedTimeService;
import org.jboss.logging.Logger;
import org.jgroups.JChannel;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanUtil {

    protected static final Logger logger = Logger.getLogger(InfinispanUtil.class);

    public static final int MAXIMUM_REPLACE_RETRIES = 25;

    // See if we have RemoteStore (external JDG) configured for cross-Data-Center scenario
    public static Set<RemoteStore> getRemoteStores(Cache ispnCache) {
        return ispnCache.getAdvancedCache().getComponentRegistry().getComponent(PersistenceManager.class).getStores(RemoteStore.class);
    }


    public static RemoteCache getRemoteCache(Cache ispnCache) {
        Set<RemoteStore> remoteStores = getRemoteStores(ispnCache);
        if (remoteStores.isEmpty()) {
            return null;
        } else {
            return remoteStores.iterator().next().getRemoteCache();
        }
    }


    public static TopologyInfo getTopologyInfo(KeycloakSession session) {
        return session.getProvider(InfinispanConnectionProvider.class).getTopologyInfo();
    }


    /**
     *
     * @param cache
     * @return true if cluster coordinator OR if it's local cache
     */
    public static boolean isCoordinator(Cache cache) {
        Transport transport = cache.getCacheManager().getTransport();
        return transport == null || transport.isCoordinator();
    }

    /**
     * Convert the given value to the proper value, which can be used when calling operations for the infinispan remoteCache.
     *
     * Infinispan HotRod protocol of versions older than 3.0 uses the "lifespan" or "maxIdle" as the normal expiration time when the value is 30 days or less.
     * However for the bigger values, it assumes that the value is unix timestamp.
     *
     * @param ispnCache
     * @param lifespanOrigMs
     * @return
     */
    public static long toHotrodTimeMs(BasicCache ispnCache, long lifespanOrigMs) {
        if (ispnCache instanceof RemoteCache && lifespanOrigMs > 2592000000L) {
            RemoteCache remoteCache = (RemoteCache) ispnCache;
            ProtocolVersion protocolVersion = remoteCache.getRemoteCacheManager().getConfiguration().version();
            if (ProtocolVersion.PROTOCOL_VERSION_30.compareTo(protocolVersion) > 0) {
                return Time.currentTimeMillis() + lifespanOrigMs;
            }
        }

        return lifespanOrigMs;
    }

    private static final Object CHANNEL_INIT_SYNCHRONIZER = new Object();

    public static void configureTransport(GlobalConfigurationBuilder gcb, String nodeName, String siteName, String jgroupsUdpMcastAddr,
                                      String jgroupsConfigPath) {
        if (nodeName == null) {
            gcb.transport().defaultTransport();
        } else {
            FileLookup fileLookup = FileLookupFactory.newInstance();

            synchronized (CHANNEL_INIT_SYNCHRONIZER) {
                String originalMcastAddr = System.getProperty(InfinispanConnectionProvider.JGROUPS_UDP_MCAST_ADDR);
                if (jgroupsUdpMcastAddr == null) {
                    System.getProperties().remove(InfinispanConnectionProvider.JGROUPS_UDP_MCAST_ADDR);
                } else {
                    System.setProperty(InfinispanConnectionProvider.JGROUPS_UDP_MCAST_ADDR, jgroupsUdpMcastAddr);
                }
                try {
                    JChannel channel = new JChannel(fileLookup.lookupFileLocation(jgroupsConfigPath, InfinispanUtil.class.getClassLoader()).openStream());
                    channel.setName(nodeName);
                    JGroupsTransport transport = new JGroupsTransport(channel);

                    TransportConfigurationBuilder transportBuilder = gcb.transport()
                            .nodeName(nodeName)
                            .siteId(siteName)
                            .transport(transport);

                    // Use the cluster corresponding to current site. This is needed as the nodes in different DCs should not share same cluster
                    if (siteName != null) {
                        transportBuilder.clusterName(siteName);
                    }


                    transportBuilder.jmx()
                            .domain(InfinispanConnectionProvider.JMX_DOMAIN + "-" + nodeName)
                            .enable();

                    logger.infof("Configured jgroups transport with the channel name: %s", nodeName);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    if (originalMcastAddr == null) {
                        System.getProperties().remove(InfinispanConnectionProvider.JGROUPS_UDP_MCAST_ADDR);
                    } else {
                        System.setProperty(InfinispanConnectionProvider.JGROUPS_UDP_MCAST_ADDR, originalMcastAddr);
                    }
                }
            }
        }
    }

    public static ConfigurationBuilder createCacheConfigurationBuilder() {
        ConfigurationBuilder builder = new ConfigurationBuilder();

        // need to force the encoding to application/x-java-object to avoid unnecessary conversion of keys/values. See WFLY-14356.
        builder.encoding().mediaType(MediaType.APPLICATION_OBJECT_TYPE);

        return builder;
    }

    public static ConfigurationBuilder getActionTokenCacheConfig() {
        ConfigurationBuilder cb = createCacheConfigurationBuilder();

        cb.memory()
                .evictionStrategy(EvictionStrategy.NONE)
                .evictionType(EvictionType.COUNT)
                .size(InfinispanConnectionProvider.ACTION_TOKEN_CACHE_DEFAULT_MAX);
        cb.expiration()
                .maxIdle(InfinispanConnectionProvider.ACTION_TOKEN_MAX_IDLE_SECONDS, TimeUnit.SECONDS)
                .wakeUpInterval(InfinispanConnectionProvider.ACTION_TOKEN_WAKE_UP_INTERVAL_SECONDS, TimeUnit.SECONDS);

        return cb;
    }

    /**
     * Replaces the {@link TimeService} in infinispan with the one that respects Keycloak {@link Time}.
     * @param cacheManager
     * @return Runnable to revert replacement of the infinispan time service
     */
    public static Runnable setTimeServiceToKeycloakTime(EmbeddedCacheManager cacheManager) {
        TimeService previousTimeService = replaceComponent(cacheManager, TimeService.class, KEYCLOAK_TIME_SERVICE, true);
        AtomicReference<TimeService> ref = new AtomicReference<>(previousTimeService);
        return () -> {
            if (ref.get() == null) {
                logger.warn("Calling revert of the TimeService when testing TimeService was already reverted");
                return;
            }

            logger.info("Revert set KeycloakIspnTimeService to the infinispan cacheManager");

            replaceComponent(cacheManager, TimeService.class, ref.getAndSet(null), true);
        };
    }

    /**
     * Forked from org.infinispan.test.TestingUtil class
     *
     * Replaces a component in a running cache manager (global component registry).
     *
     * @param cacheMgr       cache in which to replace component
     * @param componentType        component type of which to replace
     * @param replacementComponent new instance
     * @param rewire               if true, ComponentRegistry.rewire() is called after replacing.
     *
     * @return the original component that was replaced
     */
    private static <T> T replaceComponent(EmbeddedCacheManager cacheMgr, Class<T> componentType, T replacementComponent, boolean rewire) {
        GlobalComponentRegistry cr = cacheMgr.getGlobalComponentRegistry();
        BasicComponentRegistry bcr = cr.getComponent(BasicComponentRegistry.class);
        ComponentRef<T> old = bcr.getComponent(componentType);
        bcr.replaceComponent(componentType.getName(), replacementComponent, true);
        if (rewire) {
            cr.rewire();
            cr.rewireNamedRegistries();
        }
        return old != null ? old.wired() : null;
    }

    public static final TimeService KEYCLOAK_TIME_SERVICE = new EmbeddedTimeService() {

        private long getCurrentTimeMillis() {
            return Time.currentTimeMillis();
        }

        @Override
        public long wallClockTime() {
            return getCurrentTimeMillis();
        }

        @Override
        public long time() {
            return TimeUnit.MILLISECONDS.toNanos(getCurrentTimeMillis());
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(getCurrentTimeMillis());
        }
    };
}
