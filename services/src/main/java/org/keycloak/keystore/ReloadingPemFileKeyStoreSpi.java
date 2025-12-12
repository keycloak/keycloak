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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

import org.keycloak.common.util.PemUtils;

import org.jboss.logging.Logger;

/**
 * {@code KeyStoreSpi} implementation that hot-reloads certificates and private keys from PEM files when the backing files change.
 */
public class ReloadingPemFileKeyStoreSpi extends DelegatingKeyStoreSpi {

    // Empty password used for the in-memory KeyStore that holds the credentials loaded from PEM files.
    protected static final char[] IN_MEMORY_KEYSTORE_PASSWORD = "".toCharArray();

    private static final Logger log = Logger.getLogger(ReloadingPemFileKeyStoreSpi.class);

    // List of objects holding the path of certificates and private keys and their last known modification timestamps.
    private final List<KeyFileEntry> keyFileEntries = new ArrayList<>();

    // List of objects holding the path of the certificates and their last known modification timestamps.
    private final List<CertificateFileEntry> certificateFileEntries = new ArrayList<>();

    public ReloadingPemFileKeyStoreSpi() {
        // Empty.
    }

    /**
     * Adds new key entry (certificate and private key) and recreates the {@code KeyStore}.
     *
     * @param cert Path to certificate file in PEM format.
     * @param key Path to private key file in PEM format.
     * @throws IOException
     * @throws CertificateException
     * @throws InvalidKeySpecException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     */
    public void addKeyEntry(Path cert, Path key) throws IOException, KeyStoreException, InvalidKeySpecException, NoSuchAlgorithmException, CertificateException {
        keyFileEntries.add(new KeyFileEntry(cert, key));
        setKeyStoreDelegate(createKeyStore());
    }

    /**
     * Adds new certificate entry and recreates the {@code KeyStore}.
     *
     * @param cert Path to certificate file in PEM format.
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws KeyStoreException
     */
    public void addCertificateEntry(Path cert) throws IOException, KeyStoreException, InvalidKeySpecException, NoSuchAlgorithmException, CertificateException {
        certificateFileEntries.add(new CertificateFileEntry(cert));
        setKeyStoreDelegate(createKeyStore());
    }

    /**
     * Reload certificate and key files if they have been modified on disk since they were last loaded.
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws KeyStoreException
     */
    void refresh() throws KeyStoreException, InvalidKeySpecException, NoSuchAlgorithmException, CertificateException,
            IOException {
        // Check if any of the files has been updated.
        // If yes, update the last modification timestamp for the file(s) and recreate delegate KeyStore with new content.
        boolean wasReloaded = false;
        int i = 0;
        for (KeyFileEntry e : keyFileEntries) {
            if (e.needsReload()) {
                keyFileEntries.set(i, new KeyFileEntry(e.certPath, e.keyPath));
                wasReloaded = true;
            }
            i++;
        }
        i = 0;
        for (CertificateFileEntry e : certificateFileEntries) {
            if (e.needsReload()) {
                certificateFileEntries.set(i, new CertificateFileEntry(e.certPath));
                wasReloaded = true;
            }
            i++;
        }
        // Re-generate KeyStore.
        if (wasReloaded) {
            log.debug("Refreshing KeyStore");
            setKeyStoreDelegate(createKeyStore());
        }
    }

    /**
     * Create KeyStore containing given certificates and private keys.
     * @throws KeyStoreException
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    private KeyStore createKeyStore() throws KeyStoreException, InvalidKeySpecException, NoSuchAlgorithmException,
            CertificateException, IOException {
        log.debug("Creating new in-memory PKCS12 KeyStore.");
        KeyStore ks = KeyStore.getInstance("PKCS12");

        // Calling load(), even with null arguments, will initialize the KeyStore to expected state.
        ks.load(null, null);

        int i = 0;

        // Load certificates + private keys.
        for (KeyFileEntry e : keyFileEntries) {
            String alias = String.format("%04d", i++);
            log.debugv("Adding key entry with alias {0}: {1}, {2}", alias, e.keyPath, e.certPath);
            ks.setKeyEntry(alias, PemUtils.decodePrivateKey(new String(Files.readAllBytes(e.keyPath))), IN_MEMORY_KEYSTORE_PASSWORD,
                    PemUtils.decodeCertificates(new String(Files.readAllBytes(e.certPath))));
        }
        // Load trusted certificates.
        for (CertificateFileEntry e : certificateFileEntries) {
            String alias = String.format("%04d", i++);
            log.debugv("Adding certificate entry with alias {0}: {1}", alias, e.certPath);
            for (Certificate c : PemUtils.decodeCertificates(new String(Files.readAllBytes(e.certPath)))) {
                ks.setCertificateEntry(alias, c);
            }
        }

        return ks;
    }

    /**
     * Holds the path of certificate and private key and their last known modification timestamp.
     */
    class KeyFileEntry {
        private final Path certPath;
        private final Path keyPath;
        private final FileTime certLastModified;
        private final FileTime keyLastModified;

        KeyFileEntry(Path certPath, Path keyPath) throws IOException {
            this.certPath = certPath;
            this.keyPath = keyPath;
            this.certLastModified = Files.getLastModifiedTime(certPath);
            this.keyLastModified = Files.getLastModifiedTime(keyPath);
        }

        boolean needsReload() throws IOException {
            return (certLastModified.compareTo(Files.getLastModifiedTime(certPath)) < 0) ||
                    (keyLastModified.compareTo(Files.getLastModifiedTime(keyPath)) < 0);
        }
    }

    /**
     * Holds the path of a certificate and its last known modification timestamp.
     */
    class CertificateFileEntry {
        private final Path certPath;
        private final FileTime certLastModified;

        CertificateFileEntry(Path certPath) throws IOException {
            this.certPath = certPath;
            this.certLastModified = Files.getLastModifiedTime(certPath);
        }

        boolean needsReload() throws IOException {
            return certLastModified.compareTo(Files.getLastModifiedTime(certPath)) < 0;
        }
    }

}
