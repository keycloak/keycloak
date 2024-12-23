/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.crl.infinispan;

import java.security.GeneralSecurityException;
import java.security.cert.X509CRL;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import org.infinispan.Cache;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.crl.CrlStorageProvider;

/**
 *
 * @author rmartinc
 */
public class InfinispanCrlStorageProvider implements CrlStorageProvider {

    private static final Logger log = Logger.getLogger(InfinispanCrlStorageProvider.class);

    private final Cache<String, X509CRLEntry> crlCache;
    private final Map<String, FutureTask<X509CRL>> tasksInProgress;
    private final long cacheTime;
    private final long minTimeBetweenRequests;

    public InfinispanCrlStorageProvider(Cache<String, X509CRLEntry> crlCache, Map<String, FutureTask<X509CRL>> tasksInProgress,
            int cacheTime, int minTimeBetweenRequests) {
        this.crlCache = crlCache;
        this.tasksInProgress = tasksInProgress;
        this.cacheTime = cacheTime > 0? TimeUnit.SECONDS.toMillis(cacheTime) : -1;
        this.minTimeBetweenRequests = minTimeBetweenRequests > 0? TimeUnit.SECONDS.toMillis(minTimeBetweenRequests) : 10000L;
    }

    @Override
    public X509CRL get(String key, Callable<X509CRL> loader) throws GeneralSecurityException {
        final X509CRLEntry crlEntry = crlCache.get(key);
        final long currentTime = Time.currentTimeMillis();
        if (crlEntry != null && (crlEntry.getCrl().getNextUpdate() == null || crlEntry.getCrl().getNextUpdate().compareTo(new Date(currentTime)) > 0)) {
            log.debugf("returning CRL '%s' from cache because it's cached OK", key);
            return crlEntry.getCrl();
        }

        // refresh the crl entry in the cache
        return reloadCrl(key, loader, currentTime, crlEntry);
    }

    @Override
    public boolean refreshCache(String key, Callable<X509CRL> loader) throws GeneralSecurityException {
        final X509CRLEntry entry = crlCache.get(key);
        final X509CRL crl = reloadCrl(key, loader, Time.currentTimeMillis(), entry);
        return  crl != null && (entry == null || entry.getCrl() != crl);
    }

    @Override
    public void close() {
        // no-op
    }

    private X509CRL reloadCrl(String key, Callable<X509CRL> loader, long currentTime, X509CRLEntry crlEntry) {
        if (crlEntry != null && currentTime < crlEntry.getLastRequestTime() + minTimeBetweenRequests){
            log.debugf("Avoiding loading crl with key '%s' again, last refreshed time %d", key, crlEntry.getLastRequestTime());
            return crlEntry.getCrl();
        }

        FutureTask<X509CRL> task = new FutureTask<>(() -> loadCrl(key, loader, currentTime));

        final FutureTask<X509CRL> existing = tasksInProgress.putIfAbsent(key, task);
        if (existing == null) {
            log.debugf("Reloading crl for model key '%s'.", key);
            task.run();
        } else {
            task = existing;
        }

        try {
            return task.get();
        } catch (ExecutionException ee) {
            throw new RuntimeException("Error when loading crl " + key + " : " + ee.getMessage(), ee);
        } catch (InterruptedException ie) {
            throw new RuntimeException("Error. Interrupted when loading crl " + key, ie);
        } finally {
            // Our thread inserted the task. Let's clean
            if (existing == null) {
                tasksInProgress.remove(key);
            }
        }
    }

    private X509CRL loadCrl(String key, Callable<X509CRL> loader, long currentTime) throws Exception {
        final X509CRL crl = loader.call();
        if (crl == null) {
            log.warnf("Loading crl with key '%s' returned null.", key);
            return null;
        }
        long lifespan = cacheTime;
        if (crl.getNextUpdate() != null) {
            final long nextUpdateTime = crl.getNextUpdate().getTime() - currentTime;
            if (nextUpdateTime <= 0) {
                // if the CRL is expired just cache the minimum time
                lifespan = minTimeBetweenRequests;
            } else if (lifespan <= 0 || nextUpdateTime < lifespan) {
                // get the minimum between cacheTime and nextUpdate
                lifespan = nextUpdateTime;
            }
        }
        if (lifespan > 0) {
            crlCache.put(key, new X509CRLEntry(crl, currentTime), lifespan, TimeUnit.MILLISECONDS);
            log.debugf("The crl with key '%s' was retrieved successfully and cached for %d millis.", key, lifespan);
        } else {
            crlCache.put(key, new X509CRLEntry(crl, currentTime));
            log.debugf("The crl with key '%s' was retrieved successfully and cached forever.", key);
        }
        return crl;
    }

}
