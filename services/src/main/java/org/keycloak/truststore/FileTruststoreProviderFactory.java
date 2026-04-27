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

package org.keycloak.truststore;

import java.io.File;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.security.auth.x500.X500Principal;

import org.keycloak.Config;
import org.keycloak.common.enums.HostnameVerificationPolicy;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class FileTruststoreProviderFactory implements TruststoreProviderFactory {

    static final String HOSTNAME_VERIFICATION_POLICY = "hostname-verification-policy";

    private static final Logger log = Logger.getLogger(FileTruststoreProviderFactory.class);

    private TruststoreProvider provider;

    @Override
    public TruststoreProvider create(KeycloakSession session) {
        return provider;
    }

    // For testing purposes
    public void setProvider(TruststoreProvider provider) {
        this.provider = provider;
    }

    @Override
    public void init(Config.Scope config) {

        String storepath = config.get("file");
        String pass = config.get("password");
        String policy = config.get(HOSTNAME_VERIFICATION_POLICY);
        String configuredType = config.get("type");

        if (storepath != null || pass != null || configuredType != null) {
            log.warn("Using deprecated 'spi-truststore-file-*' options. Consider using 'truststore-paths' option.");
        }

        HostnameVerificationPolicy verificationPolicy = null;
        KeyStore truststore = null;
        boolean system = false;
        if (storepath == null) {
            storepath = System.getProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_KEY);
            if (storepath == null) {
                File defaultTrustStore = TruststoreBuilder.getJRETruststore();
                if (!defaultTrustStore.exists()) {
                    throw new RuntimeException("Attribute 'file' missing in 'truststore':'file' configuration, and could not find the system truststore");
                }
                storepath = defaultTrustStore.getAbsolutePath();
                system = true;
            }
            // should there be an exception if pass / type are configured for the spi-truststore
            pass = System.getProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_PASSWORD_KEY, system ? "changeit" : null);
            configuredType = System.getProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_TYPE_KEY);
        }
        String type = KeystoreUtil.getKeystoreType(configuredType, storepath, KeyStore.getDefaultType());
        try {
            truststore = KeystoreUtil.loadKeyStore(storepath, pass, type);
        } catch (Exception e) {
            // in fips mode the default truststore type can be pkcs12, but the cacerts file will still be jks
            if (system && !"jks".equalsIgnoreCase(type)) {
                try {
                    truststore = KeystoreUtil.loadKeyStore(storepath, pass, "jks");
                } catch (Exception e1) {
                }
            }
            if (truststore == null) {
                throw new RuntimeException("Failed to initialize TruststoreProviderFactory: " + new File(storepath).getAbsolutePath() + ", truststore type: " + type, e);
            }
        }
        if (policy == null) {
            verificationPolicy = HostnameVerificationPolicy.DEFAULT;
        } else {
            try {
                verificationPolicy = HostnameVerificationPolicy.valueOf(policy);
            } catch (Exception e) {
                throw new RuntimeException("Invalid value for 'hostname-verification-policy': " + policy
                        + " (must be one of: " + Stream.of(HostnameVerificationPolicy.values())
                                .map(HostnameVerificationPolicy::name).collect(Collectors.joining(", "))
                        + ")");
            }
        }

        TruststoreCertificatesLoader certsLoader = new TruststoreCertificatesLoader(truststore);
        provider = new FileTruststoreProvider(truststore, verificationPolicy, Collections.unmodifiableMap(certsLoader.trustedRootCerts)
                , Collections.unmodifiableMap(certsLoader.intermediateCerts));
        TruststoreProviderSingleton.set(provider);
        log.debugf("File truststore provider initialized: %s, Truststore type: %s",  new File(storepath).getAbsolutePath(), type);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "file";
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name("file")
                .type("string")
                .helpText("DEPRECATED: The file path of the trust store from where the certificates are going to be read from to validate TLS connections.")
                .add()
                .property()
                .name("password")
                .type("string")
                .helpText("DEPRECATED: The trust store password.")
                .add()
                .property()
                .name(HOSTNAME_VERIFICATION_POLICY)
                .type("string")
                .helpText("DEPRECATED: The hostname verification policy.")
                .options(Arrays.stream(HostnameVerificationPolicy.values()).map(HostnameVerificationPolicy::name).toArray(String[]::new))
                .defaultValue(HostnameVerificationPolicy.DEFAULT.name())
                .add()
                .property()
                .name("type")
                .type("string")
                .helpText("DEPRECATED: Type of the truststore. If not provided, the type would be detected based on the truststore file extension or platform default type.")
                .add()
                .build();
    }

    private static class TruststoreCertificatesLoader {

        private Map<X500Principal, List<X509Certificate>> trustedRootCerts = new HashMap<>();
        private Map<X500Principal, List<X509Certificate>> intermediateCerts = new HashMap<>();


        public TruststoreCertificatesLoader(KeyStore truststore) {
            readTruststore(truststore);
        }

        /**
         * Get all certificates from Keycloak Truststore, and classify them in two lists : root CAs and intermediates CAs
         */
        private void readTruststore(KeyStore truststore) {

            //Reading truststore aliases & certificates
            Enumeration<String> enumeration;

            try {

                enumeration = truststore.aliases();
                log.trace("Checking " + truststore.size() + " entries from the truststore.");
                while(enumeration.hasMoreElements()) {
                    String alias = enumeration.nextElement();
                    readTruststoreEntry(truststore, alias);
                }
            } catch (KeyStoreException e) {
                log.error("Error while reading Keycloak truststore "+e.getMessage(),e);
            }
        }

        private void readTruststoreEntry(KeyStore truststore, String alias) {
            try {
                Certificate certificate = truststore.getCertificate(alias);

                if (certificate instanceof X509Certificate) {
                    X509Certificate cax509cert = (X509Certificate) certificate;
                    if (isSelfSigned(cax509cert)) {
                        X500Principal principal = cax509cert.getSubjectX500Principal();
                        List<X509Certificate> certs = trustedRootCerts.get(principal);
                        if (certs == null) {
                            certs = new ArrayList<>();
                            trustedRootCerts.put(principal, certs);
                        }
                        certs.add(cax509cert);
                        log.debug("Trusted root CA found in truststore : alias : " + alias + " | Subject DN : " + principal);
                    } else {
                        X500Principal principal = cax509cert.getSubjectX500Principal();
                        List<X509Certificate> certs = intermediateCerts.get(principal);
                        if (certs == null) {
                            certs = new ArrayList<>();
                            intermediateCerts.put(principal, certs);
                        }
                        certs.add(cax509cert);
                        log.debug("Intermediate CA found in truststore : alias : " + alias + " | Subject DN : " + principal);
                    }
                } else
                    log.info("Skipping certificate with alias [" + alias + "] from truststore, because it's not an X509Certificate");
            } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | NoSuchProviderException e) {
                log.warnf("Error while reading Keycloak truststore entry [%s]. Exception message: %s", alias, e.getMessage(), e);
            }
        }

        /**
         * Checks whether given X.509 certificate is self-signed.
         */
        private boolean isSelfSigned(X509Certificate cert)
                throws CertificateException, NoSuchAlgorithmException,
                NoSuchProviderException {
            try {
                // Try to verify certificate signature with its own public key
                PublicKey key = cert.getPublicKey();
                cert.verify(key);
                log.trace("certificate " + cert.getSubjectDN() + " detected as root CA");
                return true;
            } catch (SignatureException sigEx) {
                // Invalid signature --> not self-signed
                log.trace("certificate " + cert.getSubjectDN() + " detected as intermediate CA");
            } catch (InvalidKeyException keyEx) {
                // Invalid key --> not self-signed
                log.trace("certificate " + cert.getSubjectDN() + " detected as intermediate CA");
            }
            return false;
        }
    }
}
