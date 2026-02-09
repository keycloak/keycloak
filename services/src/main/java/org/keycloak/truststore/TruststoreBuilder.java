/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.truststore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.common.util.KeystoreUtil.KeystoreFormat;

import org.jboss.logging.Logger;

/**
 * Builds a system-wide truststore from the given config options.
 */
public class TruststoreBuilder {

    public static final String SYSTEM_TRUSTSTORE_KEY = "javax.net.ssl.trustStore";
    public static final String SYSTEM_TRUSTSTORE_PASSWORD_KEY = "javax.net.ssl.trustStorePassword";
    public static final String SYSTEM_TRUSTSTORE_TYPE_KEY = "javax.net.ssl.trustStoreType";
    private static final String CERT_PROTECTION_ALGORITHM_KEY = "keystore.pkcs12.certProtectionAlgorithm";
    public static final String DUMMY_PASSWORD = "keycloakchangeit"; // fips length compliant dummy password
    static final String PKCS12 = "PKCS12";

    private static final Logger LOGGER = Logger.getLogger(TruststoreBuilder.class);

    public static void setSystemTruststore(String[] truststores, boolean trustStoreIncludeDefault, String dataDir) {
        KeyStore truststore = createMergedTruststore(truststores, trustStoreIncludeDefault);

        // save with a dummy password just in case some logic that uses the system properties needs to have one
        File file = saveTruststore(truststore, dataDir, DUMMY_PASSWORD.toCharArray());

        // finally update the system properties
        System.setProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_KEY, file.getAbsolutePath());
        System.setProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_TYPE_KEY, PKCS12);
        System.setProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_PASSWORD_KEY, DUMMY_PASSWORD);
    }

    static File saveTruststore(KeyStore truststore, String dataDir, char[] password) {
        File file = new File(dataDir, "keycloak-truststore.p12");
        file.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            // this should inhibit the use of encryption in storing the certs
            // it's of course not concurrency safe, but it should only be run at startup
            String oldValue = System.setProperty(CERT_PROTECTION_ALGORITHM_KEY, "NONE");
            truststore.store(fos, password);
            if (oldValue != null) {
                System.setProperty(CERT_PROTECTION_ALGORITHM_KEY, oldValue);
            } else {
                System.getProperties().remove(CERT_PROTECTION_ALGORITHM_KEY);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to save truststore: " + file.getAbsolutePath(), e);
        }
        return file;
    }

    static KeyStore createMergedTruststore(String[] truststores, boolean trustStoreIncludeDefault) {
        KeyStore truststore = createPkcs12KeyStore();

        if (trustStoreIncludeDefault) {
            includeDefaultTruststore(truststore);
        }

        List<String> discoveredFiles = new ArrayList<>();
        mergeFiles(truststores, truststore, true, discoveredFiles);
        if (!discoveredFiles.isEmpty()) {
            LOGGER.infof("Found the following truststore files under directories specified in the truststore paths %s",
                    discoveredFiles);
        }
        return truststore;
    }

    private static void mergeFiles(String[] truststores, KeyStore truststore, boolean topLevel, List<String> discoveredFiles) {
        for (String file : truststores) {
            File f = new File(file);
            if (f.isDirectory()) {
                mergeFiles(Stream.of(f.listFiles()).map(File::getAbsolutePath).toArray(String[]::new), truststore, false, discoveredFiles);
            } else {
                var format = KeystoreUtil.getKeystoreFormat(file).orElse(null);
                if (format == KeystoreFormat.PKCS12) {
                    mergeTrustStore(truststore, file, loadStore(file, PKCS12, null));
                    if (!topLevel) {
                        discoveredFiles.add(f.getAbsolutePath());
                    }
                } else if (mergePemFile(truststore, file, topLevel) && !topLevel) {
                    discoveredFiles.add(f.getAbsolutePath());
                }
            }
        }
    }

    static KeyStore createPkcs12KeyStore() {
        try {
            KeyStore truststore = KeyStore.getInstance(PKCS12);
            truststore.load(null, null);
            return truststore;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize truststore: cannot create a PKCS12 keystore", e);
        }
    }


    /**
     * Include the default truststore, if it can be found.
     * <p>
     * The existing system properties will be preserved so that this logic can be rerun without consuming
     * the newly created merged truststore.
     */
    static void includeDefaultTruststore(KeyStore truststore) {
        String originalTruststoreKey = TruststoreBuilder.SYSTEM_TRUSTSTORE_KEY + ".orig";
        String originalTruststoreTypeKey = TruststoreBuilder.SYSTEM_TRUSTSTORE_TYPE_KEY + ".orig";
        String originalTruststorePasswordKey = TruststoreBuilder.SYSTEM_TRUSTSTORE_PASSWORD_KEY + ".orig";

        String trustStorePath = System.getProperty(originalTruststoreKey);
        String type = PKCS12;
        String password = null;
        File defaultTrustStore = null;
        if (trustStorePath == null) {
            trustStorePath = System.getProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_KEY);
            if (trustStorePath == null) {
                defaultTrustStore = getJRETruststore();
            } else {
                type = System.getProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_TYPE_KEY, KeyStore.getDefaultType());
                password = System.getProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_PASSWORD_KEY);
                // save the original information
                System.setProperty(originalTruststoreKey, trustStorePath);
                System.setProperty(originalTruststoreTypeKey, type);
                if (password == null) {
                    System.getProperties().remove(originalTruststorePasswordKey);
                } else {
                    System.setProperty(originalTruststorePasswordKey, password);
                }
                defaultTrustStore = new File(trustStorePath);
            }
        } else {
            type = System.getProperty(originalTruststoreTypeKey);
            password = System.getProperty(originalTruststorePasswordKey);
            defaultTrustStore = new File(trustStorePath);
        }

        if (defaultTrustStore.exists()) {
            String path = defaultTrustStore.getAbsolutePath();
            mergeTrustStore(truststore, path, loadStore(path, type, password));
        } else {
            LOGGER.warnf("Default truststore was to be included, but could not be found at: %s", defaultTrustStore);
        }
    }

    static File getJRETruststore() {
        // try jre locations - there doesn't seem to be a good default mechanism for this
        String securityDirectory = System.getProperty("java.home") + File.separator + "lib" + File.separator
                + "security";
        File jssecacertsFile = new File(securityDirectory, "jssecacerts");
        if (jssecacertsFile.exists() && jssecacertsFile.isFile()) {
            return jssecacertsFile;
        }
        return new File(securityDirectory, "cacerts");
    }

    static KeyStore loadStore(String path, String type, String password) {
        try {
            return KeystoreUtil.loadKeyStore(path, password, type);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to initialize truststore: " + new File(path).getAbsolutePath() + ", type: " + type, e);
        }
    }

    private static boolean mergePemFile(KeyStore truststore, String file, boolean isPem) {
        try (FileInputStream pemInputStream = new FileInputStream(file)) {
            CertificateFactory certFactory = CertificateFactory.getInstance("X509");
            boolean loadedAny = false;
            while (pemInputStream.available() > 0) {
                X509Certificate cert;
                try {
                    cert = (X509Certificate) certFactory.generateCertificate(pemInputStream);
                    loadedAny = true;
                } catch (CertificateException e) {
                    if (pemInputStream.available() > 0 || !loadedAny) {
                        // any remaining input means there is an actual problem with the key contents or
                        // file format
                        if (isPem || loadedAny) {
                            throw e;
                        }
                        LOGGER.debugf(e,
                                "The file %s may not be in PEM format, it will not be used to create the merged truststore",
                                new File(file).getAbsolutePath());
                        continue;
                    }
                    LOGGER.debugf(e,
                            "The trailing entry for %s generated a certificate exception, assuming instead that the file ends with comments",
                            new File(file).getAbsolutePath());
                    continue;
                }
                setCertificateEntry(truststore, cert);
            }
            return loadedAny;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to initialize truststore, could not merge: " + new File(file).getAbsolutePath(), e);
        }
    }

    private static void setCertificateEntry(KeyStore truststore, Certificate cert) throws KeyStoreException {
        String alias = null;
        if (cert instanceof X509Certificate) {
            X509Certificate x509Cert = (X509Certificate)cert;
            // use an alias that should be unique, yet deterministic
            alias = x509Cert.getSubjectX500Principal().getName() + "_" + x509Cert.getSerialNumber().toString(16);
        } else {
            // isn't expected
            alias = String.valueOf(Collections.list(truststore.aliases()).size());
        }
        truststore.setCertificateEntry(alias, cert);
    }

    private static void mergeTrustStore(KeyStore truststore, String file, KeyStore additionalStore) {
        try {
            for (String alias : Collections.list(additionalStore.aliases())) {
                if (additionalStore.isCertificateEntry(alias)) {
                    setCertificateEntry(truststore, additionalStore.getCertificate(alias));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to initialize truststore, could not merge: " + new File(file).getAbsolutePath(), e);
        }
    }
}
