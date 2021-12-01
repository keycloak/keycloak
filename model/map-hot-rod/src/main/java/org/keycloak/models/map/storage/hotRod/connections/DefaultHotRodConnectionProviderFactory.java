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
package org.keycloak.models.map.storage.hotRod.connections;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.RemoteCacheManagerAdmin;
import org.infinispan.client.hotrod.configuration.ClientIntelligence;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.map.storage.hotRod.common.HotRodEntityDescriptor;
import org.keycloak.models.map.storage.hotRod.common.HotRodUtils;
import org.keycloak.models.map.storage.hotRod.common.ProtoSchemaInitializer;
import org.keycloak.models.map.storage.hotRod.HotRodMapStorageProviderFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class DefaultHotRodConnectionProviderFactory implements HotRodConnectionProviderFactory {

    public static final String PROVIDER_ID = "default";

    private static final Logger LOG = Logger.getLogger(DefaultHotRodConnectionProviderFactory.class);

    private org.keycloak.Config.Scope config;

    private RemoteCacheManager remoteCacheManager;

    @Override
    public HotRodConnectionProvider create(KeycloakSession session) {
        if (remoteCacheManager == null) {
            lazyInit();
        }
        return new DefaultHotRodConnectionProvider(remoteCacheManager);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void init(org.keycloak.Config.Scope config) {
        this.config = config;
    }

    public void lazyInit() {
        if (config.getBoolean("embedded", false)) {
            HotRodUtils.createHotRodMapStoreServer(config.getInt("embeddedPort", 11444));
        }

        ConfigurationBuilder remoteBuilder = new ConfigurationBuilder();
        remoteBuilder.addServer()
                .host(config.get("host", "localhost"))
                .port(config.getInt("port", 11444))
                .clientIntelligence(ClientIntelligence.HASH_DISTRIBUTION_AWARE)
                .marshaller(new ProtoStreamMarshaller());

        if (config.getBoolean("enableSecurity", true)) {
            remoteBuilder.security()
                    .authentication()
                    .saslMechanism("SCRAM-SHA-512")
                    .username(config.get("username", "admin"))
                    .password(config.get("password", "admin"))
                    .realm(config.get("realm", "default"));
        }

        boolean configureRemoteCaches = config.getBoolean("configureRemoteCaches", false);
        if (configureRemoteCaches) {
            configureRemoteCaches(remoteBuilder);
        }

        remoteBuilder.addContextInitializer(ProtoSchemaInitializer.INSTANCE);
        remoteCacheManager = new RemoteCacheManager(remoteBuilder.build());

        Set<String> remoteCaches = HotRodMapStorageProviderFactory.ENTITY_DESCRIPTOR_MAP.values().stream()
                .map(HotRodEntityDescriptor::getCacheName).collect(Collectors.toSet());

        if (configureRemoteCaches) {
            // access the caches to force their creation
            remoteCaches.forEach(remoteCacheManager::getCache);
        }

        registerSchemata(ProtoSchemaInitializer.INSTANCE);

        RemoteCacheManagerAdmin administration = remoteCacheManager.administration();
        if (config.getBoolean("reindexAllCaches", false)) {
            LOG.infof("Reindexing all caches. This can take a long time to complete. While the rebuild operation is in progress, queries might return fewer results.");
            remoteCaches.forEach(administration::reindexCache);
        } else {
            String reindexCaches = config.get("reindexCaches", "");
            if (reindexCaches != null) {
                Arrays.stream(reindexCaches.split(","))
                    .map(String::trim)
                        .filter(e -> !e.isEmpty())
                        .filter(remoteCaches::contains)
                        .peek(cacheName -> LOG.infof("Reindexing %s cache. This can take a long time to complete. While the rebuild operation is in progress, queries might return fewer results.", cacheName))
                        .forEach(administration::reindexCache);
            }
        }
    }

    private void registerSchemata(GeneratedSchema initializer) {
        final RemoteCache<String, String> protoMetadataCache = remoteCacheManager.getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);

        protoMetadataCache.put(initializer.getProtoFileName(), initializer.getProtoFile());

        String errors = protoMetadataCache.get(ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX);
        if (errors != null) {
            throw new IllegalStateException("Some Protobuf schema files contain errors: " + errors + "\nSchema :\n" + initializer.getProtoFileName());
        }
    }

    private void configureRemoteCaches(ConfigurationBuilder builder) {
        URI uri;
        try {
            uri = DefaultHotRodConnectionProviderFactory.class.getClassLoader().getResource("config/cacheConfig.xml").toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Cannot read the cache configuration!", e);
        }

        HotRodMapStorageProviderFactory.ENTITY_DESCRIPTOR_MAP.values().stream()
                .map(HotRodEntityDescriptor::getCacheName)
                .forEach(name -> builder.remoteCache(name).configurationURI(uri));
    }
}
