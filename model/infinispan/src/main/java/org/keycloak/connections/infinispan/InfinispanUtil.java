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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.spi.infinispan.impl.embedded.CacheConfigurator;

import org.infinispan.commons.time.TimeService;
import org.infinispan.commons.util.FileLookup;
import org.infinispan.commons.util.FileLookupFactory;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.global.TransportConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.factories.impl.BasicComponentRegistry;
import org.infinispan.factories.impl.ComponentRef;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.infinispan.util.EmbeddedTimeService;
import org.jboss.logging.Logger;
import org.jgroups.JChannel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanUtil {

    protected static final Logger logger = Logger.getLogger(InfinispanUtil.class);

    public static final int MAXIMUM_REPLACE_RETRIES = 25;

    /**
     * @deprecated For removal. Use {@link InfinispanConnectionProvider#getNodeInfo()} instead.
     * @see TopologyInfo
     */
    @Deprecated
    public static TopologyInfo getTopologyInfo(KeycloakSession session) {
        return session.getProvider(InfinispanConnectionProvider.class).getTopologyInfo();
    }


    private static final Object CHANNEL_INIT_SYNCHRONIZER = new Object();

    /**
     * @deprecated to be removed without replacement.
     */
    @Deprecated(since = "26.3", forRemoval = true)
    public static void configureTransport(GlobalConfigurationBuilder gcb, String nodeName, String siteName, String jgroupsUdpMcastAddr,
                                          String jgroupsBindAddr, String jgroupsConfigPath) {
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
                var originalBindAddr = System.getProperty(InfinispanConnectionProvider.JGROUPS_BIND_ADDR);
                if (jgroupsBindAddr == null) {
                    System.getProperties().remove(InfinispanConnectionProvider.JGROUPS_BIND_ADDR);
                } else {
                    System.setProperty(InfinispanConnectionProvider.JGROUPS_BIND_ADDR, jgroupsBindAddr);
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
                    if (originalBindAddr == null) {
                        System.getProperties().remove(InfinispanConnectionProvider.JGROUPS_BIND_ADDR);
                    } else {
                        System.setProperty(InfinispanConnectionProvider.JGROUPS_BIND_ADDR, originalBindAddr);
                    }
                }
            }
        }
    }

    /**
     * @deprecated to be removed. Use {@link CacheConfigurator#createCacheConfigurationBuilder()}.
     */
    @Deprecated(since = "26.3", forRemoval = true)
    public static ConfigurationBuilder createCacheConfigurationBuilder() {
        return CacheConfigurator.createCacheConfigurationBuilder();
    }

    /**
     * @deprecated to be removed without replacement.
     */
    @Deprecated(since = "26.3", forRemoval = true)
    public static ConfigurationBuilder getActionTokenCacheConfig() {
        var cb = CacheConfigurator.createCacheConfigurationBuilder();

        cb.memory()
                .whenFull(EvictionStrategy.MANUAL)
                .maxCount(InfinispanConnectionProvider.ACTION_TOKEN_CACHE_DEFAULT_MAX);
        cb.expiration()
                .maxIdle(InfinispanConnectionProvider.ACTION_TOKEN_MAX_IDLE_SECONDS, TimeUnit.SECONDS)
                .wakeUpInterval(InfinispanConnectionProvider.ACTION_TOKEN_WAKE_UP_INTERVAL_SECONDS, TimeUnit.SECONDS);

        return cb;
    }

    /**
     * @deprecated to be removed. Use {@link CacheConfigurator#getCrlCacheConfig()}.
     */
    @Deprecated(since = "26.3", forRemoval = true)
    public static ConfigurationBuilder getCrlCacheConfig() {
        return CacheConfigurator.getCrlCacheConfig();
    }

    /**
     * @deprecated to be removed. Use {@link CacheConfigurator#getRevisionCacheConfig(long)}.
     */
    @Deprecated(since = "26.3", forRemoval = true)
    public static ConfigurationBuilder getRevisionCacheConfig(long maxEntries) {
        return CacheConfigurator.getRevisionCacheConfig(maxEntries);
    }

    /**
     * Replaces the {@link TimeService} in infinispan with the one that respects Keycloak {@link Time}.
     *
     * @param cacheManager The {@link EmbeddedCacheManager} to inject the Keycloak {@link Time}.
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
     * <p>
     * Replaces a component in a running cache manager (global component registry).
     *
     * @param cacheMgr             cache in which to replace component
     * @param componentType        component type of which to replace
     * @param replacementComponent new instance
     * @param rewire               if true, ComponentRegistry.rewire() is called after replacing.
     * @return the original component that was replaced
     */
    private static <T> T replaceComponent(EmbeddedCacheManager cacheMgr, Class<T> componentType, T replacementComponent, boolean rewire) {
        GlobalComponentRegistry cr = GlobalComponentRegistry.of(cacheMgr);
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
