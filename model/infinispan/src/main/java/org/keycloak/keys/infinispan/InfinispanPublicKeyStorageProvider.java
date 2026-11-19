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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.keycloak.cluster.ClusterProvider;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.PublicKeysWrapper;
import org.keycloak.executors.ExecutorsProvider;
import org.keycloak.keys.PublicKeyLoader;
import org.keycloak.keys.PublicKeyStorageProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.utils.KeycloakModelUtils;

import org.infinispan.Cache;
import org.jboss.logging.Logger;


/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanPublicKeyStorageProvider implements PublicKeyStorageProvider {

    private static final Logger log = Logger.getLogger(InfinispanPublicKeyStorageProvider.class);

    private static final String BACKGROUND_RELOAD_EXECUTOR = "public-keys-reload";

    private final KeycloakSession session;

    private final Cache<String, PublicKeysEntry> keys;

    private final Map<String, FutureTask<PublicKeysEntry>> tasksInProgress;

    // Model keys that cannot start a background reload before the time in the value, in seconds.
    // The value is Integer.MAX_VALUE while a background reload is running.
    private final Map<String, Integer> backgroundReloads;

    private final int minTimeBetweenRequests ;
    private final int maxCacheTime;

    private final Set<String> invalidations = new HashSet<>();

    private boolean transactionEnlisted = false;

    public InfinispanPublicKeyStorageProvider(KeycloakSession session, Cache<String, PublicKeysEntry> keys, Map<String, FutureTask<PublicKeysEntry>> tasksInProgress,
            Map<String, Integer> backgroundReloads, int minTimeBetweenRequests, int maxCacheTime) {
        this.session = session;
        this.keys = keys;
        this.tasksInProgress = tasksInProgress;
        this.backgroundReloads = backgroundReloads;
        this.minTimeBetweenRequests = minTimeBetweenRequests;
        this.maxCacheTime = maxCacheTime;
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

        var events = invalidations.stream()
                .peek(keys::remove)
                .map(PublicKeyStorageInvalidationEvent::create)
                .toList();
        cluster.notify(InfinispanCachePublicKeyProviderFactory.PUBLIC_KEY_STORAGE_INVALIDATION_EVENT, events, true);
    }

    @Override
    public KeyWrapper getFirstPublicKey(String modelKey, String algorithm, PublicKeyLoader loader) {
        return getPublicKey(modelKey, null, algorithm, loader);
    }

    @Override
    public KeyWrapper getPublicKey(String modelKey, String kid, String algorithm, PublicKeyLoader loader) {
        PublicKeysEntry entry = keys.get(modelKey);
        int lastRequestTime = entry == null? 0 : entry.getLastRequestTime();
        int currentTime = Time.currentTime();
        boolean isSendingRequestAllowed = currentTime > lastRequestTime + minTimeBetweenRequests;

        // Check if key is in cache, but only if KID is provided or if the key cache has been loaded recently,
        // in order to get a key based on partial match with alg param.
        if (!isExpired(entry, currentTime) && (kid != null || !isSendingRequestAllowed)) {
            KeyWrapper publicKey = entry.getCurrentKeys().getKeyByKidAndAlg(kid, algorithm);
            if (publicKey != null) {
                // return a copy of the key to not modify the cached one
                return publicKey.cloneKey();
            }
        }

        PublicKeysEntry updatedEntry = reloadKeys(modelKey, entry, currentTime, loader);
        entry = updatedEntry == null? entry : updatedEntry;
        KeyWrapper publicKey = entry == null? null : entry.getCurrentKeys().getKeyByKidAndAlg(kid, algorithm);
        if (publicKey != null) {
            // return a copy of the key to not modify the cached one
            return publicKey.cloneKey();
        }

        List<String> availableKids = entry == null? Collections.emptyList() : entry.getCurrentKeys().getKids();
        log.warnf("PublicKey wasn't found in the storage. Requested kid: '%s' . Available kids: '%s'", kid, availableKids);

        return null;
    }

    /**
     * If the key is found in the cache that is returned straight away. If not in cache,
     * the keys are reloaded if allowed by the minTimeBetweenRequests and key
     * is searched again.
     *
     * @param modelKey The model key
     * @param predicate The predicate to search the key
     * @param loader The loader to reload keys
     * @return The key or null
     */
    @Override
    public KeyWrapper getFirstPublicKey(String modelKey, Predicate<KeyWrapper> predicate, PublicKeyLoader loader) {
        PublicKeysEntry entry = keys.get(modelKey);
        int currentTime = Time.currentTime();
        if (!isExpired(entry, currentTime)) {
            // if in cache just try to return if found
            KeyWrapper key = entry.getCurrentKeys().getKeyByPredicate(predicate);
            if (key != null) {
                return key.cloneKey();
            }
        }
        // if not found try a second time if reload allowed by minTimeBetweenRequests
        entry = reloadKeys(modelKey, entry, currentTime, loader);
        if (entry != null) {
            KeyWrapper key = entry.getCurrentKeys().getKeyByPredicate(predicate);
            if (key != null) {
                return key.cloneKey();
            }
        }
        return null;
    }

    /**
     * return all keys under the model key. The maxCacheTime is used to reload the
     * keys from time to time.
     * @param modelKey The model key
     * @param loader The loader to reload keys id maxCacheTime reached
     * @return The keys in the model
     */
    @Override
    public List<KeyWrapper> getKeys(String modelKey, PublicKeyLoader loader) {
        PublicKeysEntry entry = keys.get(modelKey);
        int currentTime = Time.currentTime();

        if (isExpired(entry, currentTime) || (hasNoExpiration(entry) && currentTime > entry.getLastRequestTime() + maxCacheTime)) {
            // reload preemptively
            PublicKeysEntry updatedEntry = reloadKeys(modelKey, entry, currentTime, loader);
            if (updatedEntry != null) {
                entry = updatedEntry;
            }
        }

        return entry == null
                ? Collections.emptyList()
                : entry.getCurrentKeys().getKeys().stream().map(KeyWrapper::cloneKey).collect(Collectors.toList());
    }

    /**
     * return all keys under the model key like {@link #getKeys(String, PublicKeyLoader)},
     * but the caller only waits for the loader if there are no valid keys in the cache.
     * Valid keys are returned straight away, and they are reloaded in the background
     * when maxCacheTime is reached.
     * @param modelKey The model key
     * @param loader The loader to load the keys if there are no valid keys in the cache
     * @param backgroundLoader Creates the loader to reload valid keys in the background
     * @return The keys in the model
     */
    @Override
    public List<KeyWrapper> getKeys(String modelKey, PublicKeyLoader loader, Function<KeycloakSession, PublicKeyLoader> backgroundLoader) {
        PublicKeysEntry entry = keys.get(modelKey);
        int currentTime = Time.currentTime();

        if (isExpired(entry, currentTime)) {
            // no valid keys to return, the caller has to wait for them
            return getKeys(modelKey, loader);
        }

        if (hasNoExpiration(entry) && currentTime > entry.getLastRequestTime() + maxCacheTime) {
            // reload preemptively, the keys in the cache are used until the reload finishes
            reloadKeysInBackground(modelKey, entry, currentTime, backgroundLoader);
        }

        return entry.getCurrentKeys().getKeys().stream().map(KeyWrapper::cloneKey).collect(Collectors.toList());
    }

    @Override
    public boolean reloadKeys(String modelKey, PublicKeyLoader loader) {
        PublicKeysEntry entry = keys.get(modelKey);
        int currentTime = Time.currentTime();
        return reloadKeys(modelKey, entry, currentTime, loader) != null;
    }

    @Override
    public boolean reloadKeysInBackground(String modelKey, Function<KeycloakSession, PublicKeyLoader> backgroundLoader) {
        PublicKeysEntry entry = keys.get(modelKey);
        int currentTime = Time.currentTime();
        // keys that are missing or expired are loaded by the caller that needs them
        return !isExpired(entry, currentTime) && reloadKeysInBackground(modelKey, entry, currentTime, backgroundLoader);
    }

    private boolean hasNoExpiration(PublicKeysEntry entry) {
        return entry == null || entry.getCurrentKeys().getExpirationTime() == null;
    }

    private boolean isExpired(PublicKeysEntry entry, int currentTime) {
        if (entry == null) {
            return true;
        }

        if (entry.getCurrentKeys().getExpirationTime() != null) {
            return currentTime > TimeUnit.MILLISECONDS.toSeconds(entry.getCurrentKeys().getExpirationTime());
        }

        return false;
    }

    private PublicKeysEntry reloadKeys(String modelKey, PublicKeysEntry entry, int currentTime, PublicKeyLoader loader) {
        // Check if we are allowed to send request
        if (entry == null || currentTime > entry.getLastRequestTime() + minTimeBetweenRequests) {
            WrapperCallable wrapperCallable = new WrapperCallable(modelKey, loader);
            FutureTask<PublicKeysEntry> task = new FutureTask<>(wrapperCallable);
            FutureTask<PublicKeysEntry> existing = tasksInProgress.putIfAbsent(modelKey, task);

            if (existing == null) {
                log.debugf("Reloading keys for model key '%s'.", modelKey);
                task.run();
            } else {
                task = existing;
            }

            try {
                return task.get();
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
            log.warnf("Won't load the keys for model '%s'. Last request time was %d", modelKey, entry.getLastRequestTime());
        }
        return null;
    }

    private boolean reloadKeysInBackground(String modelKey, PublicKeysEntry entry, int currentTime, Function<KeycloakSession, PublicKeyLoader> backgroundLoader) {
        // Check if we are allowed to send request
        if (currentTime <= entry.getLastRequestTime() + minTimeBetweenRequests) {
            log.debugf("Won't reload the keys for model '%s' in the background. Last request time was %d", modelKey, entry.getLastRequestTime());
            return false;
        }

        // Only one background reload at a time, and a failed one is not repeated until minTimeBetweenRequests passes
        backgroundReloads.values().removeIf(notBefore -> notBefore < currentTime);
        if (backgroundReloads.putIfAbsent(modelKey, Integer.MAX_VALUE) != null) {
            log.debugf("Won't reload the keys for model '%s' in the background. The previous reload is running or has failed recently", modelKey);
            return false;
        }

        try {
            log.debugf("Reloading keys for model key '%s' in the background.", modelKey);
            session.getProvider(ExecutorsProvider.class).getExecutor(BACKGROUND_RELOAD_EXECUTOR).execute(new BackgroundReload(
                    session.getKeycloakSessionFactory(), keys, backgroundReloads, minTimeBetweenRequests, modelKey, backgroundLoader));
            return true;
        } catch (RuntimeException e) {
            backgroundReloads.put(modelKey, currentTime + minTimeBetweenRequests);
            log.warnf(e, "Reload of the keys for model key '%s' in the background could not be started.", modelKey);
            return false;
        }
    }

    @Override
    public void close() {

    }

    /**
     * Reloads the keys of a model key using its own session, as the session that
     * requested the reload does not wait for it.
     */
    private static class BackgroundReload implements Runnable {

        private final KeycloakSessionFactory sessionFactory;
        private final Cache<String, PublicKeysEntry> keys;
        private final Map<String, Integer> backgroundReloads;
        private final int minTimeBetweenRequests;
        private final String modelKey;
        private final Function<KeycloakSession, PublicKeyLoader> loader;

        private BackgroundReload(KeycloakSessionFactory sessionFactory, Cache<String, PublicKeysEntry> keys, Map<String, Integer> backgroundReloads,
                int minTimeBetweenRequests, String modelKey, Function<KeycloakSession, PublicKeyLoader> loader) {
            this.sessionFactory = sessionFactory;
            this.keys = keys;
            this.backgroundReloads = backgroundReloads;
            this.minTimeBetweenRequests = minTimeBetweenRequests;
            this.modelKey = modelKey;
            this.loader = loader;
        }

        @Override
        public void run() {
            try {
                KeycloakModelUtils.runJobInTransaction(sessionFactory, session -> {
                    // The entry is removed when the model is updated or removed. The next caller loads the keys in that case.
                    if (keys.containsKey(modelKey)) {
                        session.getProvider(PublicKeyStorageProvider.class, InfinispanPublicKeyStorageProviderFactory.PROVIDER_ID)
                                .reloadKeys(modelKey, loader.apply(session));
                    }
                });
                backgroundReloads.remove(modelKey);
            } catch (Throwable t) {
                // The keys in the cache are not modified, they are used until a reload works
                backgroundReloads.put(modelKey, Time.currentTime() + minTimeBetweenRequests);
                log.warnf("Error reloading keys for model key '%s' in the background, the keys in the cache are kept. %s", modelKey, t.getMessage());
                log.debug("Error reloading keys in the background", t);
            }
        }
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

                PublicKeysWrapper publicKeys = delegate.loadKeys();

                if (log.isDebugEnabled()) {
                    log.debugf("Public keys retrieved successfully for model %s. New kids: %s", modelKey, publicKeys.getKids());
                }

                entry = new PublicKeysEntry(currentTime, publicKeys);

                if (publicKeys.getExpirationTime() != null) {
                    keys.put(modelKey, entry, publicKeys.getExpirationTime(), TimeUnit.MILLISECONDS);
                } else {
                    keys.put(modelKey, entry);
                }
            }
            return entry;
        }
    }
}
