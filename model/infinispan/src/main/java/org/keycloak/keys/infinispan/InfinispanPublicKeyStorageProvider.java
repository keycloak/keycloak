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

package org.keycloak.keys.infinispan;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.infinispan.Cache;
import org.jboss.logging.Logger;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.keys.PublicKeyLoader;
import org.keycloak.keys.PublicKeyStorageProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.cache.infinispan.ClearCacheEvent;


/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanPublicKeyStorageProvider implements PublicKeyStorageProvider {

    private static final Logger log = Logger.getLogger(InfinispanPublicKeyStorageProvider.class);

    private final KeycloakSession session;

    private final Cache<String, PublicKeysEntry> keys;

    private final Map<String, FutureTask<PublicKeysEntry>> tasksInProgress;

    private final int minTimeBetweenRequests ;

    private Set<String> invalidations = new HashSet<>();

    private boolean transactionEnlisted = false;

    public InfinispanPublicKeyStorageProvider(KeycloakSession session, Cache<String, PublicKeysEntry> keys, Map<String, FutureTask<PublicKeysEntry>> tasksInProgress, int minTimeBetweenRequests) {
        this.session = session;
        this.keys = keys;
        this.tasksInProgress = tasksInProgress;
        this.minTimeBetweenRequests = minTimeBetweenRequests;
    }


    @Override
    public void clearCache() {
        keys.clear();
        ClusterProvider cluster = session.getProvider(ClusterProvider.class);
        cluster.notify(InfinispanPublicKeyStorageProviderFactory.KEYS_CLEAR_CACHE_EVENTS, new ClearCacheEvent(), true, ClusterProvider.DCNotify.ALL_DCS);
    }


    void addInvalidation(String cacheKey) {
        if (!transactionEnlisted) {
            session.getTransactionManager().enlistAfterCompletion(getAfterTransaction());
            transactionEnlisted = true;
        }

        this.invalidations.add(cacheKey);
    }


    protected KeycloakTransaction getAfterTransaction() {
        return new KeycloakTransaction() {

            @Override
            public void begin() {
            }

            @Override
            public void commit() {
                runInvalidations();
            }

            @Override
            public void rollback() {
                runInvalidations();
            }

            @Override
            public void setRollbackOnly() {
            }

            @Override
            public boolean getRollbackOnly() {
                return false;
            }

            @Override
            public boolean isActive() {
                return true;
            }
        };
    }


    protected void runInvalidations() {
        ClusterProvider cluster = session.getProvider(ClusterProvider.class);

        for (String cacheKey : invalidations) {
            keys.remove(cacheKey);
            cluster.notify(InfinispanPublicKeyStorageProviderFactory.PUBLIC_KEY_STORAGE_INVALIDATION_EVENT, PublicKeyStorageInvalidationEvent.create(cacheKey), true, ClusterProvider.DCNotify.ALL_DCS);
        }
    }


    @Override
    public KeyWrapper getPublicKey(String modelKey, String kid, PublicKeyLoader loader) {
        return getPublicKey(modelKey, kid, null, loader);
    }

    @Override
    public KeyWrapper getFirstPublicKey(String modelKey, String algorithm, PublicKeyLoader loader) {
        return getPublicKey(modelKey, null, algorithm, loader);
    }

    private KeyWrapper getPublicKey(String modelKey, String kid, String algorithm, PublicKeyLoader loader) {
        // Check if key is in cache
        PublicKeysEntry entry = keys.get(modelKey);
        if (entry != null) {
            KeyWrapper publicKey = algorithm != null ? getPublicKeyByAlg(entry.getCurrentKeys(), algorithm) : getPublicKey(entry.getCurrentKeys(), kid);
            if (publicKey != null) {
                return publicKey;
            }
        }

        int lastRequestTime = entry==null ? 0 : entry.getLastRequestTime();
        int currentTime = Time.currentTime();

        // Check if we are allowed to send request
        if (currentTime > lastRequestTime + minTimeBetweenRequests) {

            WrapperCallable wrapperCallable = new WrapperCallable(modelKey, loader);
            FutureTask<PublicKeysEntry> task = new FutureTask<>(wrapperCallable);
            FutureTask<PublicKeysEntry> existing = tasksInProgress.putIfAbsent(modelKey, task);

            if (existing == null) {
                task.run();
            } else {
                task = existing;
            }

            try {
                entry = task.get();

                // Computation finished. Let's see if key is available
                KeyWrapper publicKey = algorithm != null ? getPublicKeyByAlg(entry.getCurrentKeys(), algorithm) : getPublicKey(entry.getCurrentKeys(), kid);
                if (publicKey != null) {
                    return publicKey;
                }

            } catch (ExecutionException ee) {
                throw new RuntimeException("Error when loading public keys: " + ee.getMessage(), ee);
            } catch (InterruptedException ie) {
                throw new RuntimeException("Error. Interrupted when loading public keys", ie);
            } finally {
                // Our thread inserted the task. Let's clean
                if (existing == null) {
                    tasksInProgress.remove(modelKey);
                }
            }
        } else {
            log.warnf("Won't load the keys for model '%s' . Last request time was %d", modelKey, lastRequestTime);
        }

        Set<String> availableKids = entry==null ? Collections.emptySet() : entry.getCurrentKeys().keySet();
        log.warnf("PublicKey wasn't found in the storage. Requested kid: '%s' . Available kids: '%s'", kid, availableKids);

        return null;
    }

    private KeyWrapper getPublicKey(Map<String, KeyWrapper> publicKeys, String kid) {
        // Backwards compatibility
        if (kid == null && !publicKeys.isEmpty()) {
            return publicKeys.values().iterator().next();
        } else {
            return publicKeys.get(kid);
        }
    }

    private KeyWrapper getPublicKeyByAlg(Map<String, KeyWrapper> publicKeys, String algorithm) {
        if (algorithm == null) return null;
        for(KeyWrapper keyWrapper : publicKeys.values())
            if (algorithm.equals(keyWrapper.getAlgorithmOrDefault())) return keyWrapper;
        return null;
    }

    @Override
    public void close() {

    }

    private class WrapperCallable implements Callable<PublicKeysEntry> {

        private final String modelKey;
        private final PublicKeyLoader delegate;

        public WrapperCallable(String modelKey, PublicKeyLoader delegate) {
            this.modelKey = modelKey;
            this.delegate = delegate;
        }

        @Override
        public PublicKeysEntry call() throws Exception {
            PublicKeysEntry entry = keys.get(modelKey);

            int lastRequestTime = entry==null ? 0 : entry.getLastRequestTime();
            int currentTime = Time.currentTime();

            // Check again if we are allowed to send request. There is a chance other task was already finished and removed from tasksInProgress in the meantime.
            if (currentTime > lastRequestTime + minTimeBetweenRequests) {

                Map<String, KeyWrapper> publicKeys = delegate.loadKeys();

                if (log.isDebugEnabled()) {
                    log.debugf("Public keys retrieved successfully for model %s. New kids: %s", modelKey, publicKeys.keySet().toString());
                }

                entry = new PublicKeysEntry(currentTime, publicKeys);

                keys.put(modelKey, entry);
            }
            return entry;
        }
    }
}
