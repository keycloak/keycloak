/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.cluster.infinispan;

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.StoreConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.persistence.remote.configuration.ExhaustedAction;
import org.infinispan.persistence.remote.configuration.RemoteStoreConfigurationChildBuilder;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
class TestCacheManagerFactory {


    <T extends StoreConfigurationBuilder<?, T> & RemoteStoreConfigurationChildBuilder<T>> EmbeddedCacheManager createManager(int threadId, String cacheName, Class<T> builderClass) {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("jgroups.tcp.port", "53715");
        GlobalConfigurationBuilder gcb = new GlobalConfigurationBuilder();

        boolean clustered = false;
        boolean async = false;
        boolean allowDuplicateJMXDomains = true;

        if (clustered) {
            gcb = gcb.clusteredDefault();
            gcb.transport().clusterName("test-clustering");
        }

        gcb.globalJmxStatistics().allowDuplicateDomains(allowDuplicateJMXDomains);

        EmbeddedCacheManager cacheManager = new DefaultCacheManager(gcb.build());

        Configuration invalidationCacheConfiguration = getCacheBackedByRemoteStore(threadId, cacheName, builderClass);

        cacheManager.defineConfiguration(cacheName, invalidationCacheConfiguration);
        cacheManager.defineConfiguration("local", new ConfigurationBuilder().build());
        return cacheManager;

    }


    private <T extends StoreConfigurationBuilder<?, T> & RemoteStoreConfigurationChildBuilder<T>> Configuration getCacheBackedByRemoteStore(int threadId, String cacheName, Class<T> builderClass) {
        ConfigurationBuilder cacheConfigBuilder = new ConfigurationBuilder();

        String host = "localhost";
        int port = threadId==1 ? 12232 : 13232;
        //int port = 11222;

        return cacheConfigBuilder.persistence().addStore(builderClass)
                .fetchPersistentState(false)
                .ignoreModifications(false)
                .purgeOnStartup(false)
                .preload(false)
                .shared(true)
                .remoteCacheName(cacheName)
                .rawValues(true)
                .forceReturnValues(false)
                .marshaller(KeycloakHotRodMarshallerFactory.class.getName())
                //.protocolVersion(ProtocolVersion.PROTOCOL_VERSION_26)
                //.maxBatchSize(5)
                .addServer()
                    .host(host)
                    .port(port)
                .connectionPool()
                    .maxActive(20)
                    .exhaustedAction(ExhaustedAction.CREATE_NEW)
                .async()
                .   enabled(false).build();
    }
}
