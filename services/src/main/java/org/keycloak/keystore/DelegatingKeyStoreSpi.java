/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.keystore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.logging.Logger;

/**
 * Implementation of {@code KeyStoreSpi} that delegates calls to an instance of {@code KeyStore}.
 * The delegate keystore can be replaced on demand when the underlying certificate(s) and key(s) change.
 */
public abstract class DelegatingKeyStoreSpi extends KeyStoreSpi {

    private static final Logger log = Logger.getLogger(DelegatingKeyStoreSpi.class);

    // Use Clock instance instead of Instant.now(). This allows mocked clock to be injected from test cases(s).
    static Clock now = Clock.systemUTC();

    // Defines how often the delegate keystore should be checked for updates.
    static Duration cacheTtl = Duration.of(1, ChronoUnit.SECONDS);

    private AtomicReference<Delegate> delegate = new AtomicReference<>();

    // Defines the next time when to check updates.
    private Instant cacheExpiredTime = Instant.MIN;

    /**
     * Reloads the delegate KeyStore if the underlying files have changed on disk.
     */
    abstract void refresh() throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException,
            InvalidKeySpecException, NoSuchProviderException;

    /**
     * Calls {@link #refresh()} to refresh the cached KeyStore and if more than
     * {@link #cacheTtl} has passed since last
     * refresh.
     */
    private void refreshCachedKeyStore() {
        // Return if not enough time has passed for the delegate KeyStore to be refreshed.
        if (now.instant().isBefore(cacheExpiredTime)) {
            return;
        }

        // Set the time when refresh should be checked next.
        cacheExpiredTime = now.instant().plus(cacheTtl);

        try {
            refresh();
        } catch (Exception e) {
            log.debug("Failed to refresh:", e);
            e.printStackTrace();
        }
    }

    /**
     * Replace the {@code KeyStore} delegate,
     *
     * @param delegate KeyStore instance that becomes the delegate.
     */
    void setKeyStoreDelegate(KeyStore delegate) {
        log.debug("Setting new KeyStore delegate");
        this.delegate.set(new Delegate(delegate));
    }

    @Override
    public Key engineGetKey(String alias, char[] password) throws NoSuchAlgorithmException, UnrecoverableKeyException {
        refreshCachedKeyStore();

        try {
            return delegate.get().keyStore.getKey(alias, password);
        } catch (KeyStoreException e) {
            log.error("getKey() failed", e);
            return null;
        } catch (UnrecoverableKeyException e) {
            // UnrecoverableKeyException is thrown when given password for keystore entry was incorrect.
            // JSSE X509KeyManagerImpl.getEntry() unfortunately hides the error by catching and ignoring the exception.
            // To help troubleshooting, we catch the exception here as well and print the error.
            log.error("getKey() failed", e);
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public Certificate[] engineGetCertificateChain(String alias) {
        refreshCachedKeyStore();

        try {
            return delegate.get().keyStore.getCertificateChain(alias);
        } catch (KeyStoreException e) {
            log.error("getCertificateChain() failed:", e);
            return new Certificate[0];
        }
    }

    @Override
    public Certificate engineGetCertificate(String alias) {
        refreshCachedKeyStore();

        try {
            return delegate.get().keyStore.getCertificate(alias);
        } catch (KeyStoreException e) {
            log.error("getCertificate() failed:", e);
            return null;
        }
    }

    @Override
    public Date engineGetCreationDate(String alias) {
        refreshCachedKeyStore();

        try {
            return delegate.get().keyStore.getCreationDate(alias);
        } catch (KeyStoreException e) {
            log.error("getCreationDate() failed:", e);
            return null;
        }
    }

    /**
     * Return aliases in sorted order.
     * This is different than the order used by underlying KeyStore.
     * Sorting aliases enables user to have expected fallback behavior when KeyManager selects server certificate.
     * This can be useful in cases when client does not set TLS SNI or unknown SNI servername is requested.
     */
    @Override
    public Enumeration<String> engineAliases() {
        refreshCachedKeyStore();
        return Collections.enumeration(new ArrayList<>(delegate.get().sortedAliases));
    }

    @Override
    public boolean engineContainsAlias(String alias) {
        refreshCachedKeyStore();

        try {
            return delegate.get().keyStore.containsAlias(alias);
        } catch (KeyStoreException e) {
            log.error("containsAlias() failed:", e);
            return false;
        }
    }

    @Override
    public int engineSize() {
        refreshCachedKeyStore();

        try {
            return delegate.get().keyStore.size();
        } catch (KeyStoreException e) {
            log.error("size() failed:", e);
            return 0;
        }
    }

    @Override
    public boolean engineIsKeyEntry(String alias) {
        refreshCachedKeyStore();

        try {
            return delegate.get().keyStore.isKeyEntry(alias);
        } catch (KeyStoreException e) {
            log.error("isKeyEntry() failed:", e);
            return false;
        }
    }

    @Override
    public boolean engineIsCertificateEntry(String alias) {
        refreshCachedKeyStore();

        try {
            return delegate.get().keyStore.isCertificateEntry(alias);
        } catch (KeyStoreException e) {
            log.error("isCertificateEntry() failed;", e);
            return false;
        }
    }

    @Override
    public String engineGetCertificateAlias(Certificate cert) {
        refreshCachedKeyStore();

        try {
            return delegate.get().keyStore.getCertificateAlias(cert);
        } catch (KeyStoreException e) {
            log.error("getCertificateAlias() failed:", e);
            return null;
        }
    }

    @Override
    public void engineLoad(InputStream stream, char[] password)
            throws IOException, NoSuchAlgorithmException, CertificateException {
        // Nothing to do here since implementations of this class have their own means to load certificates and keys.
        log.debug("engineLoad()");
    }

    private static final String IMMUTABLE_KEYSTORE_ERR = "Modifying keystore is not supported";

    @Override
    public void engineSetKeyEntry(String alias, Key key, char[] password, Certificate[] chain)
            throws KeyStoreException {
        throw new UnsupportedOperationException(IMMUTABLE_KEYSTORE_ERR);
    }

    @Override
    public void engineSetKeyEntry(String alias, byte[] key, Certificate[] chain) throws KeyStoreException {
        throw new UnsupportedOperationException(IMMUTABLE_KEYSTORE_ERR);
    }

    @Override
    public void engineSetCertificateEntry(String alias, Certificate cert) throws KeyStoreException {
        throw new UnsupportedOperationException(IMMUTABLE_KEYSTORE_ERR);
    }

    @Override
    public void engineDeleteEntry(String alias) throws KeyStoreException {
        throw new UnsupportedOperationException(IMMUTABLE_KEYSTORE_ERR);
    }

    @Override
    public void engineStore(OutputStream stream, char[] password)
            throws IOException, NoSuchAlgorithmException, CertificateException {
        throw new UnsupportedOperationException(IMMUTABLE_KEYSTORE_ERR);
    }

    class Delegate {
        KeyStore keyStore;
        List<String> sortedAliases;

        Delegate(KeyStore ks) {
            this.keyStore = ks;

            try {
                // Keep aliases sorted to entries returned.
                sortedAliases = Collections.list(ks.aliases());
                Collections.sort(sortedAliases);
            } catch (KeyStoreException e) {
                // Ignore exception.
                log.error("Failed getting aliases:", e);
            }
        }
    }

}
