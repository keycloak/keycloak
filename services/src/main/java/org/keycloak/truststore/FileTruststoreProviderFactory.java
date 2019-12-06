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

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import javax.security.auth.x500.X500Principal;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class FileTruststoreProviderFactory implements TruststoreProviderFactory {

    private static final Logger log = Logger.getLogger(FileTruststoreProviderFactory.class);

    private TruststoreProvider provider;

    @Override
    public TruststoreProvider create(KeycloakSession session) {
        return provider;
    }

    @Override
    public void init(Config.Scope config) {

        String storepath = config.get("file");
        String pass = config.get("password");
        String policy = config.get("hostname-verification-policy");

        // if "truststore" . "file" is not configured then it is disabled
        if (storepath == null && pass == null && policy == null) {
            return;
        }

        HostnameVerificationPolicy verificationPolicy = null;
        KeyStore truststore = null;

        if (storepath == null) {
            throw new RuntimeException("Attribute 'file' missing in 'truststore':'file' configuration");
        }
        if (pass == null) {
            throw new RuntimeException("Attribute 'password' missing in 'truststore':'file' configuration");
        }

        try {
            truststore = loadStore(storepath, pass == null ? null :pass.toCharArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize TruststoreProviderFactory: " + new File(storepath).getAbsolutePath(), e);
        }
        if (policy == null) {
            verificationPolicy = HostnameVerificationPolicy.WILDCARD;
        } else {
            try {
                verificationPolicy = HostnameVerificationPolicy.valueOf(policy);
            } catch (Exception e) {
                throw new RuntimeException("Invalid value for 'hostname-verification-policy': " + policy + " (must be one of: ANY, WILDCARD, STRICT)");
            }
        }

        TruststoreCertificatesLoader certsLoader = new TruststoreCertificatesLoader(truststore);
        provider = new FileTruststoreProvider(truststore, verificationPolicy, certsLoader.trustedRootCerts, certsLoader.intermediateCerts);
        TruststoreProviderSingleton.set(provider);
        log.debug("File trustore provider initialized: " + new File(storepath).getAbsolutePath());
    }

    private KeyStore loadStore(String path, char[] password) throws Exception {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream is = new FileInputStream(path);
        try {
            ks.load(is, password);
            return ks;
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {
            }
        }
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



    private class TruststoreCertificatesLoader {

        private Map<X500Principal, X509Certificate> trustedRootCerts = new HashMap<>();
        private Map<X500Principal, X509Certificate> intermediateCerts = new HashMap<>();


        public TruststoreCertificatesLoader(KeyStore truststore) {
            readTruststore(truststore);
        }

        /**
         * Get all certificates from Keycloak Truststore, and classify them in two lists : root CAs and intermediates CAs
         */
        private void readTruststore(KeyStore truststore) {

            //Reading truststore aliases & certificates
            Enumeration enumeration;

            try {

                enumeration = truststore.aliases();
                log.trace("Checking " + truststore.size() + " entries from the truststore.");
                while(enumeration.hasMoreElements()) {

                    String alias = (String)enumeration.nextElement();
                    Certificate certificate = truststore.getCertificate(alias);

                    if (certificate instanceof X509Certificate) {
                        X509Certificate cax509cert = (X509Certificate) certificate;
                        if (isSelfSigned(cax509cert)) {
                            X500Principal principal = cax509cert.getSubjectX500Principal();
                            trustedRootCerts.put(principal, cax509cert);
                            log.debug("Trusted root CA found in trustore : alias : "+alias + " | Subject DN : " + principal);
                        } else {
                            X500Principal principal = cax509cert.getSubjectX500Principal();
                            intermediateCerts.put(principal, cax509cert);
                            log.debug("Intermediate CA found in trustore : alias : "+alias + " | Subject DN : " + principal);
                        }
                    } else
                        log.info("Skipping certificate with alias ["+ alias + "] from truststore, because it's not an X509Certificate");

                }
            } catch (KeyStoreException e) {
                log.error("Error while reading Keycloak truststore "+e.getMessage(),e);
            } catch (CertificateException e) {
                log.error("Error while reading Keycloak truststore "+e.getMessage(),e);
            } catch (NoSuchAlgorithmException e) {
                log.error("Error while reading Keycloak truststore "+e.getMessage(),e);
            } catch (NoSuchProviderException e) {
                log.error("Error while reading Keycloak truststore "+e.getMessage(),e);
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
