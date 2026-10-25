/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tls;

import java.security.KeyStore;
import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Supplier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import nl.altindag.ssl.SSLFactory;
import nl.altindag.ssl.util.SSLSessionUtils;
import nl.altindag.ssl.util.TrustManagerUtils;

import org.keycloak.common.util.KeystoreUtil;

import nl.altindag.ssl.util.KeyManagerUtils;
import org.jboss.logging.Logger;

public class SslReloadService implements AutoCloseable {

    private static final Logger log = Logger.getLogger(SslReloadService.class);

    private String keyStorePath;
    private String keyStorePassword;
    private String keyPassword;
    private Supplier<KeyStore> truststoreSupplier;

    private X509ExtendedKeyManager baseKeyManager;
    private X509ExtendedTrustManager baseTrustManager;
    private SSLContext sslContext;

    private ScheduledFuture<?> reloadTask;

    public SslReloadService() {}

    public void setIdentityMaterial(String keyStorePath, String keyStorePassword, String keyPassword) throws Exception {
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keyStorePassword;
        this.keyPassword = keyPassword;

        KeyStore keyStore = KeystoreUtil.loadKeyStore(keyStorePath, keyStorePassword);
        X509ExtendedKeyManager initialKeyManager = KeyManagerUtils.createKeyManager(keyStore, keyPassword != null ? keyPassword.toCharArray() : null);
        baseKeyManager = KeyManagerUtils.createSwappableKeyManager(initialKeyManager);
    }

    public void setTrustMaterial(Supplier<KeyStore> truststoreSupplier) {
        this.truststoreSupplier = truststoreSupplier;
        X509ExtendedTrustManager initialTrustManager = TrustManagerUtils.createTrustManager(truststoreSupplier.get());
        baseTrustManager = TrustManagerUtils.createSwappableTrustManager(initialTrustManager);
    }

    public boolean isSslMaterialPresent() {
        return baseKeyManager != null || baseTrustManager != null;
    }

    public SSLContext getSslContext() {
        if (sslContext == null) {
            SSLFactory.Builder sslFactoryBuilder = SSLFactory.builder();
            if (baseKeyManager != null) {
                sslFactoryBuilder.withIdentityMaterial(baseKeyManager);
            }
            if (baseTrustManager != null) {
                sslFactoryBuilder.withTrustMaterial(baseTrustManager);
            }

            sslContext = sslFactoryBuilder.build().getSslContext();
        }
        return sslContext;
    }

    public X509KeyManager getKeyManager() {
        return baseKeyManager;
    }

    public X509TrustManager getTrustManager() {
        return baseTrustManager;
    }

    public SslReloadService scheduleReload(Duration period) {
        if (period != null && reloadTask == null) {
            reloadTask = CertificateReloadScheduler.schedule(period, this::reload);
        }
        return this;
    }

    public synchronized boolean reload() {
        if (baseKeyManager != null) {
            KeyStore keyStore;
            try {
                keyStore = KeystoreUtil.loadKeyStore(keyStorePath, keyStorePassword);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load key store " + keyStorePath, e);
            }

            X509ExtendedKeyManager newKeyManager = KeyManagerUtils.createKeyManager(keyStore, keyPassword != null ? keyPassword.toCharArray() : null);
            KeyManagerUtils.swapKeyManager(baseKeyManager, newKeyManager);
        }

        if (baseTrustManager != null) {
            KeyStore trustStore = truststoreSupplier.get();
            X509ExtendedTrustManager newTrustManager = TrustManagerUtils.createTrustManager(trustStore);
            TrustManagerUtils.swapTrustManager(baseTrustManager, newTrustManager);
        }

        if (sslContext != null) {
            SSLSessionUtils.invalidateCaches(sslContext);
        }

        return true;
    }

    @Override
    public synchronized void close() {
        if (reloadTask != null) {
            reloadTask.cancel(false);
            reloadTask = null;
        }
    }
}
