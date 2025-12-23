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
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStore.Builder;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import org.jboss.logging.Logger;

public class DefaultKeyStoreProvider implements KeyStoreProvider, KeyStoreProviderFactory {

    private static final Logger log = Logger.getLogger(DefaultKeyStoreProvider.class);

    private KeyStore.Builder ldapKeyStoreBuilder;

    @Override
    public void close() {
        // Nothing to do here.
    }

    /**
     * Returns named keystore.
     *
     * @param keyStoreIdentifier The identifier of requested keystore.
     * @return Reference to a keystore.
     * @throws KeyStoreException
     */
    @Override
    public KeyStore loadKeyStore(String keyStoreIdentifier) {
        try {
            return loadKeyStoreBuilder(keyStoreIdentifier).getKeyStore();
        } catch (KeyStoreException e) {
            log.errorv("Cannot load KeyStore {0}", keyStoreIdentifier);
            throw new RuntimeException("Cannot load KeyStore " + keyStoreIdentifier + ":" + e.getMessage());
        }
    }

    @Override
    public Builder loadKeyStoreBuilder(String keyStoreIdentifier) {
        log.infov("loadKeyStoreBuilder was called with keystore identifier {0}", keyStoreIdentifier);
        if (keyStoreIdentifier.equals(LDAP_CLIENT_KEYSTORE)) {
            return ldapKeyStoreBuilder;
        }

        log.errorv("loadKeyStoreBuilder was called with invalid keystore identifier {0}", keyStoreIdentifier);
        throw new IllegalArgumentException("invalid keystore requested, keyStoreIdentifier:" + keyStoreIdentifier);
    }

    @Override
    public KeyStoreProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Scope config) {
        // Allow changing the default duration that defines how frequently at most the backing file(s) will be checked
        // for modification. The value is parsed as ISO8601 time duration (e.g. "1s", "2m30s", "1h").
        String cacheTtl = config.get("keyStoreCacheTtl");
        if (cacheTtl != null) {
            log.infov("Setting reloading keyStore cache TTL to {0}", cacheTtl);
            ReloadingKeyStore.setDefaultKeyStoreCacheTtl(Duration.parse("PT" + cacheTtl));
        }

        ldapKeyStoreBuilder = getKeyStore(config, "ldap");
    }

    private KeyStore.Builder getKeyStore(Scope config, String prefix) {
        KeyStore.Builder keyStoreBuilder = null;

        // Check if credentials are given as PEM files.
        log.debugv("Checking for PEM files for {0}", prefix);
        String certificateFile = config.get(prefix + "CertificateFile");
        String certificateKeyFile = config.get(prefix + "CertificateKeyFile");
        if (certificateFile != null && certificateKeyFile != null) {
            log.infov("Loading credentials: {0}, {1}", certificateFile, certificateKeyFile);
            try {
                keyStoreBuilder = ReloadingKeyStore.Builder.fromPem(Paths.get(certificateFile),
                        Paths.get(certificateKeyFile));
            } catch (NoSuchAlgorithmException | CertificateException | IllegalArgumentException | KeyStoreException
                    | InvalidKeySpecException | IOException e) {
                throw new RuntimeException("Failed to initialize " + prefix + " keystore: " + e.toString());
            }
        }

        // Check if credentials are given as KeyStore file.
        String keyStoreFile = config.get(prefix + "KeystoreFile");
        String keyStorePassword = config.get(prefix + "KeystorePassword");
        String keyStoreType = config.get(prefix + "KeystoreType", "JKS");

        // Check if both PEM files and KeyStore is configured.
        if (keyStoreBuilder != null && keyStoreFile != null) {
            log.errorv("Both PEM files and KeyStore was configured for {0}. Choose only one.", prefix);
            throw new IllegalArgumentException("Both PEM files and KeyStore was configured for " + prefix + ". Choose only one.");
        }

        // Check if keyStore file is configured without password.
        if (keyStoreFile != null && keyStorePassword == null) {
            log.errorv("Password not given for {0} keystore {1}", prefix, keyStoreFile);
            throw new IllegalArgumentException("Password not given for " + prefix + " keystore: " + keyStoreFile);
        }

        log.debugv("Checking for keystore file for {0}", prefix);
        if (keyStoreFile != null) {
            try {
                log.infov("Loading credentials for {0}: {1}", prefix, keyStoreFile);
                keyStoreBuilder = ReloadingKeyStore.Builder
                        .fromKeyStoreFile(keyStoreType, Paths.get(keyStoreFile), keyStorePassword);
            } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | NoSuchProviderException e) {
                throw new RuntimeException("Failed to initialize " + prefix + " keystore: " + e.toString());
            }
        }

        return keyStoreBuilder;
    }

    @Override
    public String getId() {
        return "default";
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Nothing to do here.
    }



}
