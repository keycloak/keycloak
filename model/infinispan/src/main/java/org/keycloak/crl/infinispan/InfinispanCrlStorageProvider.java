/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.common.util.Time;
import org.keycloak.crl.CrlStorageProvider;

import org.infinispan.Cache;
import org.jboss.logging.Logger;

/**
 *
 * @author rmartinc
 */
public class InfinispanCrlStorageProvider implements CrlStorageProvider {

    private static final Logger log = Logger.getLogger(InfinispanCrlStorageProvider.class);

    private final SharedData data;

    public InfinispanCrlStorageProvider(SharedData data) {
        this.data = data;
    }

    @Override
    public X509CRL get(String key, Callable<X509CRL> loader) throws GeneralSecurityException {
        final X509CRLEntry crlEntry = data.cache().get(key);
        final long currentTime = Time.currentTimeMillis();
        if (crlEntry != null && (crlEntry.crl().getNextUpdate() == null || crlEntry.crl().getNextUpdate().compareTo(new Date(currentTime)) > 0)) {
            log.debugf("returning CRL '%s' from cache because it's cached OK", key);
            return crlEntry.crl();
        }

        // refresh the crl entry in the cache
        return reloadCrl(key, loader, currentTime, crlEntry);
    }

    @Override
    public boolean refreshCache(String key, Callable<X509CRL> loader) throws GeneralSecurityException {
        final X509CRLEntry entry = data.cache().get(key);
        final X509CRL crl = reloadCrl(key, loader, Time.currentTimeMillis(), entry);
        return  crl != null && (entry == null || entry.crl() != crl);
    }

    @Override
    public void close() {
        // no-op
    }

    private X509CRL reloadCrl(String key, Callable<X509CRL> loader, long currentTime, X509CRLEntry crlEntry) {
        if (crlEntry != null && currentTime < crlEntry.lastRequestTime()+ data.minTimeBetweenRequests()){
            log.debugf("Avoiding loading crl with key '%s' again, last refreshed time %d", key, crlEntry.lastRequestTime());
            return crlEntry.crl();
        }

        FutureTask<X509CRL> task = new FutureTask<>(() -> loadCrl(key, loader, currentTime));

        final FutureTask<X509CRL> existing = data.tasksInProgress().putIfAbsent(key, task);
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
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error. Interrupted when loading crl " + key, ie);
        } finally {
            // Our thread inserted the task. Let's clean
            if (existing == null) {
                data.tasksInProgress().remove(key);
            }
        }
    }

    private X509CRL loadCrl(String key, Callable<X509CRL> loader, long currentTime) throws Exception {
        final X509CRL crl = loader.call();
        if (crl == null) {
            log.warnf("Loading crl with key '%s' returned null.", key);
            return null;
        }
        long lifespan = getLifespan(crl, currentTime);
        if (lifespan > 0) {
            data.cache().put(key, new X509CRLEntry(crl, currentTime), lifespan, TimeUnit.MILLISECONDS);
            log.debugf("The crl with key '%s' was retrieved successfully and cached for %d millis.", key, lifespan);
        } else {
            data.cache().put(key, new X509CRLEntry(crl, currentTime));
            log.debugf("The crl with key '%s' was retrieved successfully and cached forever.", key);
        }
        return crl;
    }

    private long getLifespan(X509CRL crl, long currentTime) {
        final long cacheTime = data.cacheTime();

        if (crl.getNextUpdate() == null) {
            return cacheTime;
        }

        final long nextUpdateTime = crl.getNextUpdate().getTime() - currentTime;
        if (nextUpdateTime <= 0) {
            // if the CRL is expired just cache the minimum time
            return data.minTimeBetweenRequests();
        } else if (cacheTime > 0) {
            // get the minimum between cacheTime and nextUpdate
            return Math.min(cacheTime, nextUpdateTime);
        } else {
            // just return the next update because default cache is infinite
            return nextUpdateTime;
        }
    }

    protected interface SharedData {
        Cache<String, X509CRLEntry> cache();
        Map<String, FutureTask<X509CRL>> tasksInProgress();
        long cacheTime();
        long minTimeBetweenRequests();
    }
}
