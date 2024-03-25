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

package org.keycloak.crls.infinispan;

import org.infinispan.Cache;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.*;
import org.infinispan.notifications.cachelistener.event.*;
import org.jboss.logging.Logger;
import org.keycloak.crls.CrlEntry;
import org.keycloak.crls.CrlLoader;
import org.keycloak.crls.CrlStorageProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.truststore.TruststoreProvider;
import org.wildfly.security.auth.server.SecurityIdentity;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.cert.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Crl Storage Provider backed by Infinispan cache
 *
 * @author Joshua Smith
 * @author Scott Tustison
 */
public class InfinispanCrlStorageProvider implements CrlStorageProvider {

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

    private static final Logger logger = Logger.getLogger(InfinispanCrlStorageProvider.class);

    private static final Object lock = new Object();

    private final KeycloakSession session;

    private final Cache<String, CrlEntry> crls;

    private final Map<String, Future<CrlEntry>> tasksInProgress;

    private final CrlLoader crlLoader;

    private final InfinispanCrlStorageProviderFactory.CacheExpirationMode cacheExpirationMode;

    private final long maxCacheTime;

    public InfinispanCrlStorageProvider(KeycloakSession session, Cache<String, CrlEntry> crls, Map<String, Future<CrlEntry>> tasksInProgress, CrlLoader crlLoader, InfinispanCrlStorageProviderFactory.CacheExpirationMode cacheExpirationMode, long maxCacheTime) {
        this.session = session;
        this.crls = crls;
        this.tasksInProgress = tasksInProgress;
        this.crlLoader = crlLoader;
        this.cacheExpirationMode = cacheExpirationMode;
        this.maxCacheTime = maxCacheTime;
    }

    @Override
    public void close() {

    }

    @Override
    public CrlEntry get(List<String> urls, X500Principal issuer) throws GeneralSecurityException {
        Objects.requireNonNull(issuer);

        List<Future<CrlEntry>> inProgressCrls = new ArrayList<>();
        List<String> otherCrls = new ArrayList<>();
        CompletionService<CrlEntry> completionService;

        synchronized (lock) {
            // There are three cases
            //   1. The CRL is in the cache
            //   2. The CRL is currently being processed
            //   3. The CRL needs to be processed
            // We need to know immediately which urls are being processed as that will change as we're progressing
            // through this method. After doing so, we can check for the remaining crls in the cache. If we find
            // our CRL there, return it. If not, we will begin processing any CRLs that were not in the cache.

            for (String url : urls) {
                Future<CrlEntry> task = tasksInProgress.get(url);
                if (task != null) {
                    inProgressCrls.add(task);
                } else {
                    otherCrls.add(url);
                }
            }

            Iterator<String> iterator = otherCrls.iterator();
            while(iterator.hasNext()) {
                CrlEntry crlEntry = this.crls.get(iterator.next());
                if (crlEntry != null) {
                    if (crlEntry.getIssuerCertificate().getSubjectX500Principal().equals(issuer)) {
                        return crlEntry;
                    } else {
                        iterator.remove();
                    }
                }
            }

            // Now otherCrls contains only the CRLs that were 1) not being processed already and 2) not in the cache.
            // We need to begin processing these CRLs
            completionService = new ExecutorCompletionService<>(EXECUTOR_SERVICE);
            for (String url : otherCrls) {
                Callable<CrlEntry> callable = () -> loadCrl(url);
                tasksInProgress.put(url, completionService.submit(callable));
            }

            // Add the crls currently being processed to the completion service as well
            for (Future<CrlEntry> future : inProgressCrls) {
                completionService.submit(new CallableFuture<>(future));
            }
        }

        // Wait for the CRLs to finish processing in parallel. As each completes, we check to see if it is the CRL
        // we need. If so we can return early and let the remaining CRLs process in the background.
        try {
            for (int i = 0; i < inProgressCrls.size() + otherCrls.size(); i++) {
                Future<CrlEntry> nextEntry = completionService.poll(60, TimeUnit.SECONDS);
                CrlEntry crlEntry = nextEntry.get();
                if (crlEntry.getIssuerCertificate().getSubjectX500Principal().equals(issuer)) {
                    return crlEntry;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    @Override
    public void refreshCache(String url) {
        if (!tasksInProgress.containsKey(url)) {
            Callable<CrlEntry> callable = () -> loadCrl(url);
            tasksInProgress.put(url, EXECUTOR_SERVICE.submit(callable));
        }
    }

    private CrlEntry loadCrl(String url) throws GeneralSecurityException {
        X509CRL crl = crlLoader.loadCrl(url);
        X509Certificate crlIssuerCert = getCrlIssuerCertFromTruststore(session, crl.getIssuerX500Principal());
        crl.verify(crlIssuerCert.getPublicKey());

        Map<BigInteger, Object> revokedCertificates = crl.getRevokedCertificates()
                .stream()
                .collect(Collectors.toMap(X509CRLEntry::getSerialNumber, Function.identity()));
        CrlEntry entry = new CrlEntry(url, crlIssuerCert, revokedCertificates);

        switch (cacheExpirationMode) {
            case NEVER_EXPIRE:
                crls.put(url, entry);
                break;
            case NEXT_UPDATE:
                long lifespan =
                        crl.getNextUpdate().toInstant().getEpochSecond() - Instant.now().getEpochSecond();
                crls.put(url, entry, lifespan, TimeUnit.SECONDS);
                break;
            case MAX_CACHE_TIME:
                crls.put(url, entry, maxCacheTime, TimeUnit.SECONDS);
                break;
        }

        return entry;
    }

    private X509Certificate getCrlIssuerCertFromTruststore(KeycloakSession session, X500Principal crlIssuerPrincipal) throws GeneralSecurityException {
        TruststoreProvider truststoreProvider = session.getProvider(TruststoreProvider.class);
        if (truststoreProvider == null || truststoreProvider.getTruststore() == null) {
            throw new GeneralSecurityException("Truststore not available");
        }

        Map<X500Principal, X509Certificate> rootCerts = truststoreProvider.getRootCertificates();
        Map<X500Principal, X509Certificate> intermediateCerts = truststoreProvider.getIntermediateCertificates();

        X509Certificate crlSignatureCertificate = intermediateCerts.get(crlIssuerPrincipal);
        if (crlSignatureCertificate == null) {
            crlSignatureCertificate = rootCerts.get(crlIssuerPrincipal);
        }

        if (crlSignatureCertificate == null) {
            throw new GeneralSecurityException("Not available certificate for CRL issuer '" + crlIssuerPrincipal + "' in the truststore");
        }

        return crlSignatureCertificate;
    }

    @Listener
    public static class CrlCacheListener {

        private final Logger logger = Logger.getLogger(CrlCacheListener.class);
        private final InfinispanCrlStorageProvider infinispanCrlStorageProvider;

        public CrlCacheListener(InfinispanCrlStorageProvider infinispanCrlStorageProvider) {
            this.infinispanCrlStorageProvider = infinispanCrlStorageProvider;
        }

        @CacheEntryCreated
        public void entryCreated(CacheEntryCreatedEvent<String, String> event) {
            this.printLog("Adding key '" + event.getKey()
                    + "' to cache.", event);
            infinispanCrlStorageProvider.tasksInProgress.remove(event.getKey());
        }

        @CacheEntryExpired
        public void entryExpired(CacheEntryExpiredEvent<String, String> event) {
            this.printLog("Expiring key '" + event.getKey()
                    + "' from cache", event);
            infinispanCrlStorageProvider.refreshCache(event.getKey());
        }

        @CacheEntryVisited
        public void entryVisited(CacheEntryVisitedEvent<String, String> event) {
            this.printLog("Key '" + event.getKey() + "' was visited", event);
        }

        private void printLog(String log, CacheEntryEvent event) {
            if (!event.isPre()) {
                logger.debug(log);
            }
        }
    }
}
