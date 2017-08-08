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

import java.util.concurrent.Executor;

import org.infinispan.commons.configuration.ConfiguredBy;
import org.infinispan.filter.KeyFilter;
import org.infinispan.marshall.core.MarshalledEntry;
import org.infinispan.metadata.InternalMetadata;
import org.infinispan.persistence.remote.RemoteStore;
import org.infinispan.persistence.spi.PersistenceException;
import org.jboss.logging.Logger;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@ConfiguredBy(KcRemoteStoreConfiguration.class)
public class KcRemoteStore extends RemoteStore {

    protected static final Logger logger = Logger.getLogger(KcRemoteStore.class);

    private String cacheName;

    @Override
    public void start() throws PersistenceException {
        super.start();
        if (getRemoteCache() == null) {
            String cacheName = getConfiguration().remoteCacheName();
            throw new IllegalStateException("Remote cache '" + cacheName + "' is not available.");
        }
        this.cacheName = getRemoteCache().getName();
    }

    @Override
    public MarshalledEntry load(Object key) throws PersistenceException {
        logger.debugf("Calling load: '%s' for remote cache '%s'", key, cacheName);

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


    @Override
    public void process(KeyFilter filter, CacheLoaderTask task, Executor executor, boolean fetchValue, boolean fetchMetadata) {
        logger.infof("Calling process with filter '%s' on cache '%s'", filter, cacheName);
        super.process(filter, task, executor, fetchValue, fetchMetadata);
    }


    // Don't do anything. Writes handled by KC itself as we need more flexibility
    @Override
    public void write(MarshalledEntry entry) throws PersistenceException {
    }


    @Override
    public boolean delete(Object key) throws PersistenceException {
        logger.debugf("Calling delete for key '%s' on cache '%s'", key, cacheName);

        // Optimization - we don't need to know the previous value.
        getRemoteCache().remove(key);

        return true;
    }

    protected MarshalledEntry marshalledEntry(Object key, Object value, InternalMetadata metadata) {
        return ctx.getMarshalledEntryFactory().newMarshalledEntry(key, value, metadata);
    }



}
