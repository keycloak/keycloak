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
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * KeyStore that can reload the wrapped delegate {@code KeyStore} when the backing files have been changed on disk.
 */
public class ReloadingKeyStore extends KeyStore {

    protected ReloadingKeyStore(KeyStoreSpi keyStoreSpi)
            throws NoSuchAlgorithmException, CertificateException, IOException {

        super(keyStoreSpi, null, "ReloadingKeyStore");

        // Calling load(), even with null arguments, will initialize the KeyStore to expected state.
        load(null, null);
    }

    /**
     * KeyStore.Builder creates reloading keystores for various types of credential files.
     */
    public static class Builder extends KeyStore.Builder {

        private final KeyStore keyStore;
        private final ProtectionParameter protection;

        // Map<alias, protection>
        private Map<String, ProtectionParameter> aliasProtection;

        private Builder(KeyStore keyStore, char[] password) {
            this.keyStore = keyStore;
            this.protection = new PasswordProtection(password);
        }

        private Builder(KeyStore keyStore, char[] password, Map<String, char[]> aliasPasswords) {
            this.keyStore = keyStore;
            this.protection = new PasswordProtection(password);
            this.aliasProtection = new HashMap<>();
            for (Map.Entry<String, char[]> entry : aliasPasswords.entrySet()) {
                aliasProtection.put(entry.getKey(), new PasswordProtection(entry.getValue()));
            }
        }

        /**
         * Returns the KeyStore described by this object.
         *
         * @return Keystore described by this object.
         */
        @Override
        public KeyStore getKeyStore() {
            return keyStore;
        }

        /**
         * Returns the ProtectionParameters (password) that should be used to obtain the Entry with the given alias.
         *
         * @param alias Alias for key entry.
         * @return ProtectionParameters (password) for the key entry.
         */
        @Override
        public ProtectionParameter getProtectionParameter(String alias) {
            // Use keystore password, if individual alias passwords are not defined.
            if (aliasProtection == null) {
                return protection;
            }

            // Certain Java versions pass alias in a format to getProtectionParameter(), which was meant to be internal
            // to NewSunX509 X509KeyManagerImpl. This was fixed in JDK17.
            //
            //   X509KeyManagerImpl calls getProtectionParameter with incorrect alias
            //   https://bugs.openjdk.org/browse/JDK-8264554
            //   https://github.com/openjdk/jdk/pull/3326
            //
            // For compatibility also with older versions, parse the plain alias from NewSunX509 KeyManager internal
            // (prefixed) alias.
            // https://github.com/openjdk/jdk/blob/6e55a72f25f7273e3a8a19e0b9a97669b84808e9/src/java.base/share/classes/sun/security/ssl/X509KeyManagerImpl.java#L237-L265
            Objects.requireNonNull(alias);
            int firstDot = alias.indexOf('.');
            int secondDot = alias.indexOf('.', firstDot + 1);
            if ((firstDot == -1) || (secondDot == firstDot)) {
                return aliasProtection.getOrDefault(alias, protection);
            }
            String requestedAlias = alias.substring(secondDot + 1);
            return aliasProtection.getOrDefault(requestedAlias, protection);
        }

        /**
         * Creates KeyStore builder for given PKCS#12 or JKS file.
         *
         * @param type KeyStore type such as {@code "PKCS12"} or {@code "JKS"}.
         * @param path Path to the keystore file.
         * @param password Password used to decrypt the KeyStore.
         * @return The KeyStore builder.
         */
        public static KeyStore.Builder fromKeyStoreFile(String type, Path path, String password)
                throws NoSuchAlgorithmException, CertificateException, KeyStoreException,
                IOException, NoSuchProviderException {
            return new Builder(new ReloadingKeyStore(new ReloadingKeyStoreFileSpi(type, path, password)),
                    password.toCharArray());
        }

        /**
         * Creates KeyStore builder for given PKCS#12 or JKS file.
         *
         * @param type KeyStore type such as {@code "PKCS12"} or {@code "JKS"}.
         * @param path Path to the keystore file.
         * @param password Password used to decrypt the KeyStore.
         * @param aliasPasswords Passwords used to decrypt keystore entries. Map of: alias (key), password (value).
         * @return The KeyStore builder.
         */
        public static KeyStore.Builder fromKeyStoreFile(String type, Path path, String password,
                Map<String, char[]> aliasPasswords)
                throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException, NoSuchProviderException {
            return new Builder(new ReloadingKeyStore(new ReloadingKeyStoreFileSpi(type, path, password)),
                    password.toCharArray(), aliasPasswords);
        }

        /**
         * Creates KeyStore builder for given certificate and private key files.
         * Certificate in position {@code certs[n]} must match the private key in position {@code keys[n]}.
         *
         * @param certs List of paths to certificates.
         * @param keys List of keys to private keys.
         * @return The KeyStore builder.
         */
        public static KeyStore.Builder fromPem(List<Path> certs, List<Path> keys)
                throws NoSuchAlgorithmException, CertificateException, IllegalArgumentException, KeyStoreException,
                InvalidKeySpecException, IOException {

            if (keys.size() < certs.size()) {
                throw new IllegalArgumentException("Missing private key");
            } else if (keys.size() > certs.size()) {
                throw new IllegalArgumentException("Missing X.509 certificate");
            } else if (keys.isEmpty()) {
                throw new IllegalArgumentException("No credentials configured");
            }

            ReloadingPemFileKeyStoreSpi spi = new ReloadingPemFileKeyStoreSpi();

            Iterator<Path> cpi = certs.iterator();
            Iterator<Path> kpi = keys.iterator();
            while (cpi.hasNext() && kpi.hasNext()) {
                spi.addKeyEntry(cpi.next(), kpi.next());
            }

            return new Builder(new ReloadingKeyStore(spi), ReloadingPemFileKeyStoreSpi.IN_MEMORY_KEYSTORE_PASSWORD);
        }

        /**
         * Creates KeyStore builder for given certificate and private key file.
         *
         * @param cert Path to certificate.
         * @param key Path to private key.
         * @return The KeyStore builder.
         */
        public static KeyStore.Builder fromPem(Path cert, Path key)
                throws NoSuchAlgorithmException, CertificateException, IllegalArgumentException, KeyStoreException,
                InvalidKeySpecException, IOException {

            ReloadingPemFileKeyStoreSpi spi = new ReloadingPemFileKeyStoreSpi();
            spi.addKeyEntry(cert, key);
            return new Builder(new ReloadingKeyStore(spi), ReloadingPemFileKeyStoreSpi.IN_MEMORY_KEYSTORE_PASSWORD);
        }

        /**
         * Creates KeyStore builder for certificate path(s).
         *
         * @param cert Path to certificate.
         * @return The KeyStore builder.
         */
        public static KeyStore.Builder fromPem(Path... cert) throws KeyStoreException, InvalidKeySpecException,
                NoSuchAlgorithmException, CertificateException, IOException {

            ReloadingPemFileKeyStoreSpi spi = new ReloadingPemFileKeyStoreSpi();
            for (Path c : cert) {
                spi.addCertificateEntry(c);
            }
            return new Builder(new ReloadingKeyStore(spi), ReloadingPemFileKeyStoreSpi.IN_MEMORY_KEYSTORE_PASSWORD);
        }
    }

    /**
     * Set how frequently the KeyStore(s) will check if the underlying files have changed and reload is required.
     * The check still happens only when credentials are used. TTL of one second means that the file modification
     * will be checked at most once per second, depending when the KeyStore is used next.
     *
     * @param ttl Minimum time-to-live for in-memory delegate {@code KeyStore}.
     */
    public static void setDefaultKeyStoreCacheTtl(Duration ttl) {
        DelegatingKeyStoreSpi.cacheTtl = ttl;
    }
}
