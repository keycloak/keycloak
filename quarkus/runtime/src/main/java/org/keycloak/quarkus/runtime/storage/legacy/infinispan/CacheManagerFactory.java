/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.storage.legacy.infinispan;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.jboss.marshalling.core.JBossUserMarshaller;
import org.infinispan.manager.DefaultCacheManager;
import org.jboss.logging.Logger;
import org.keycloak.quarkus.runtime.configuration.Configuration;

public class CacheManagerFactory {

    private String config;
    private DefaultCacheManager cacheManager;
    private Future<DefaultCacheManager> cacheManagerFuture;
    private ExecutorService executor;
    private boolean initialized;

    public CacheManagerFactory(String config) {
        this.config = config;
        this.executor = createThreadPool();
        this.cacheManagerFuture = executor.submit(this::startCacheManager);
    }

    public DefaultCacheManager getOrCreate() {
        if (cacheManager == null) {
            if (initialized) {
                return null;
            }

            try {
                // for now, we don't have any explicit property for setting the cache start timeout
                return cacheManager = cacheManagerFuture.get(getStartTimeout(), TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new RuntimeException("Failed to start caches", e);
            } finally {
                shutdownThreadPool();
            }
        }

        return cacheManager;
    }

    private ExecutorService createThreadPool() {
        return Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "keycloak-cache-init");
            }
        });
    }

    private DefaultCacheManager startCacheManager() {
        ConfigurationBuilderHolder builder = new ParserRegistry().parse(config);

        if (builder.getNamedConfigurationBuilders().get("sessions").clustering().cacheMode().isClustered()) {
            configureTransportStack(builder);
        }

        // For Infinispan 10, we go with the JBoss marshalling.
        // TODO: This should be replaced later with the marshalling recommended by infinispan. Probably protostream.
        // See https://infinispan.org/docs/stable/titles/developing/developing.html#marshalling for the details
        builder.getGlobalConfigurationBuilder().serialization().marshaller(new JBossUserMarshaller());

        return new DefaultCacheManager(builder, isStartEagerly());
    }

    private boolean isStartEagerly() {
        // eagerly starts caches by default
        return Boolean.parseBoolean(System.getProperty("kc.cache-ispn-start-eagerly", Boolean.TRUE.toString()));
    }

    private Integer getStartTimeout() {
        return Integer.getInteger("kc.cache-ispn-start-timeout", 120);
    }

    private void shutdownThreadPool() {
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                        Logger.getLogger(CacheManagerFactory.class).warn("Cache init thread pool not terminated");
                    }
                }
            } catch (Exception cause) {
                executor.shutdownNow();
            } finally {
                executor = null;
                cacheManagerFuture = null;
                config = null;
                initialized = true;
            }
        }
    }

    private void configureTransportStack(ConfigurationBuilderHolder builder) {
        String transportStack = Configuration.getRawValue("kc.cache-stack");

        if (transportStack != null) {
            builder.getGlobalConfigurationBuilder().transport().defaultTransport()
                    .addProperty("configurationFile", "default-configs/default-jgroups-" + transportStack + ".xml");
        }
    }
}
