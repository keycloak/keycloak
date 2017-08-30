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

package org.keycloak.models.sessions.infinispan.remotestore;

import java.util.Optional;
import java.util.concurrent.Executor;

import org.infinispan.commons.CacheException;
import org.infinispan.commons.configuration.ConfiguredBy;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;
import org.infinispan.configuration.cache.StoreConfiguration;
import org.infinispan.filter.KeyFilter;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.marshall.core.MarshalledEntry;
import org.infinispan.metadata.InternalMetadata;
import org.infinispan.persistence.InitializationContextImpl;
import org.infinispan.persistence.remote.RemoteStore;
import org.infinispan.persistence.remote.configuration.RemoteStoreConfiguration;
import org.infinispan.persistence.remote.configuration.RemoteStoreConfigurationBuilder;
import org.infinispan.persistence.spi.InitializationContext;
import org.infinispan.persistence.spi.PersistenceException;
import org.jboss.logging.Logger;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@ConfiguredBy(KeycloakRemoteStoreConfiguration.class)
public class KeycloakRemoteStore extends RemoteStore {

    protected static final Logger logger = Logger.getLogger(KeycloakRemoteStore.class);

    private String remoteCacheName;

    @Override
    public void start() throws PersistenceException {
        this.remoteCacheName = getConfiguration().remoteCacheName();

        String cacheTemplateName = getConfiguration().useConfigTemplateFromCache();

        if (cacheTemplateName != null) {
            logger.debugf("Will override configuration of cache '%s' from template of cache '%s'", ctx.getCache().getName(), cacheTemplateName);

            // Just to ensure that dependent cache is started and it's configuration fully loaded
            EmbeddedCacheManager cacheManager = ctx.getCache().getCacheManager();
            cacheManager.getCache(cacheTemplateName, true);

            Optional<StoreConfiguration> optional = cacheManager.getCacheConfiguration(cacheTemplateName).persistence().stores().stream().filter((StoreConfiguration storeConfig) -> {

                return storeConfig instanceof RemoteStoreConfiguration;

            }).findFirst();

            if (!optional.isPresent()) {
                throw new CacheException("Unable to find remoteStore on cache '" + cacheTemplateName + ".");
            }

            RemoteStoreConfiguration templateConfig = (RemoteStoreConfiguration) optional.get();

            // We have template configuration, so create new configuration from it. Override just remoteCacheName
            PersistenceConfigurationBuilder readPersistenceBuilder = new ConfigurationBuilder().read(ctx.getCache().getCacheConfiguration()).persistence();
            RemoteStoreConfigurationBuilder configBuilder = new RemoteStoreConfigurationBuilder(readPersistenceBuilder);
            configBuilder.read(templateConfig);

            configBuilder.remoteCacheName(this.remoteCacheName);

            RemoteStoreConfiguration newCfg1 = configBuilder.create();
            KeycloakRemoteStoreConfiguration newCfg = new KeycloakRemoteStoreConfiguration(newCfg1);

            InitializationContext ctx = new InitializationContextImpl(newCfg, this.ctx.getCache(), this.ctx.getMarshaller(), this.ctx.getTimeService(),
                    this.ctx.getByteBufferFactory(), this.ctx.getMarshalledEntryFactory());

            init(ctx);

        } else {
            logger.debugf("Skip overriding configuration from template for cache '%s'", ctx.getCache().getName());
        }

        super.start();

        if (getRemoteCache() == null) {
            String cacheName = getConfiguration().remoteCacheName();
            throw new CacheException("Remote cache '" + cacheName + "' is not available.");
        }
    }

    @Override
    public MarshalledEntry load(Object key) throws PersistenceException {
        logger.debugf("Calling load: '%s' for remote cache '%s'", key, remoteCacheName);

        MarshalledEntry entry = super.load(key);
        if (entry == null) {
            return null;
        }

        // wrap remote entity
        SessionEntity entity = (SessionEntity) entry.getValue();
        SessionEntityWrapper entityWrapper = new SessionEntityWrapper(entity);

        MarshalledEntry wrappedEntry = marshalledEntry(entry.getKey(), entityWrapper, entry.getMetadata());

        logger.debugf("Found entry in load: %s", wrappedEntry.toString());

        return wrappedEntry;
    }


    // Don't do anything. Iterate over remoteCache.keySet() can have big performance impact. We handle bulk load by ourselves if needed.
    @Override
    public void process(KeyFilter filter, CacheLoaderTask task, Executor executor, boolean fetchValue, boolean fetchMetadata) {
        logger.debugf("Skip calling process with filter '%s' on cache '%s'", filter, remoteCacheName);
        // super.process(filter, task, executor, fetchValue, fetchMetadata);
    }


    // Don't do anything. Writes handled by KC itself as we need more flexibility
    @Override
    public void write(MarshalledEntry entry) throws PersistenceException {
    }


    @Override
    public boolean delete(Object key) throws PersistenceException {
        logger.debugf("Calling delete for key '%s' on cache '%s'", key, remoteCacheName);

        // Optimization - we don't need to know the previous value.
        // TODO: For some usecases (bulk removal of user sessions), it may be better for performance to call removeAsync and wait for all futures to be finished
        getRemoteCache().remove(key);

        return true;
    }

    protected MarshalledEntry marshalledEntry(Object key, Object value, InternalMetadata metadata) {
        return ctx.getMarshalledEntryFactory().newMarshalledEntry(key, value, metadata);
    }


    @Override
    public KeycloakRemoteStoreConfiguration getConfiguration() {
        return (KeycloakRemoteStoreConfiguration) super.getConfiguration();
    }
}
