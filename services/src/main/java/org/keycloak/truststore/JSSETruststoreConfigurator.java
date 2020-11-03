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

import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.io.FileInputStream;
import java.security.KeyStore;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class JSSETruststoreConfigurator {

    private TruststoreProvider provider;
    private volatile javax.net.ssl.SSLSocketFactory sslFactory;
    private volatile TrustManager[] tm;

    public JSSETruststoreConfigurator(KeycloakSession session) {
        KeycloakSessionFactory factory = session.getKeycloakSessionFactory();
        TruststoreProviderFactory truststoreFactory = (TruststoreProviderFactory) factory.getProviderFactory(TruststoreProvider.class, "file");

        provider = truststoreFactory.create(session);
        if (provider != null && provider.getTruststore() == null) {
            provider = null;
        }
    }

    public JSSETruststoreConfigurator(TruststoreProvider provider) {
        this.provider = provider;
    }

    public javax.net.ssl.SSLSocketFactory getSSLSocketFactory() {
        if (provider == null) {
            return null;
        }

        if (sslFactory == null) {
            synchronized(this) {
                if (sslFactory == null) {
                    try {
                        SSLContext sslctx = SSLContext.getInstance("TLS");
                        // Construct default keyManager explicitely, since passing null as keyManager to SSLSocketFactory results in no
                        // keymanager at all in Oracle JDK (unlike IBM JDK).  If not doing this, it would be impossible to use client
                        // certificate based authentication for LDAP when TrustStore SPI is enabled.
                        sslctx.init(getDefaultKeyManager(), getTrustManagers(), null);
                        sslFactory = sslctx.getSocketFactory();
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to initialize SSLContext: ", e);
                    }
                }
            }
        }
        return sslFactory;
    }

    public TrustManager[] getTrustManagers() {
        if (provider == null) {
            return null;
        }

        if (tm == null) {
            synchronized (this) {
                if (tm == null) {
                    TrustManagerFactory tmf = null;
                    try {
                        tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                        tmf.init(provider.getTruststore());
                        tm = tmf.getTrustManagers();
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to initialize TrustManager: ", e);
                    }
                }
            }
        }
        return tm;
    }

    public HostnameVerifier getHostnameVerifier() {
        if (provider == null) {
            return null;
        }

        HostnameVerificationPolicy policy = provider.getPolicy();
        switch (policy) {
            case ANY:
                return new HostnameVerifier() {
                    @Override
                    public boolean verify(String s, SSLSession sslSession) {
                        return true;
                    }
                };
            case WILDCARD:
                return new BrowserCompatHostnameVerifier();
            case STRICT:
                return new StrictHostnameVerifier();
            default:
                throw new IllegalStateException("Unknown policy: " + policy.name());
        }
    }

    public TruststoreProvider getProvider() {
        return provider;
    }

    private KeyManager[] getDefaultKeyManager() throws Exception {
        String keyStore = System.getProperty("javax.net.ssl.keyStore");
        String keyStoreType = System.getProperty("javax.net.ssl.keyStoreType", KeyStore.getDefaultType());
        String keyStoreProvider = System.getProperty("javax.net.ssl.keyStoreProvider");
        String keyStorePassword = System.getProperty("javax.net.ssl.keyStorePassword");

        KeyStore ks = null;
        if (keyStoreProvider == null) {
            ks = KeyStore.getInstance(keyStoreType);
        } else {
            ks = KeyStore.getInstance(keyStoreType, keyStoreProvider);
        }

        if (keyStore != null) {
            try (FileInputStream fs = new FileInputStream(keyStore)) {
                ks.load(fs, keyStorePassword.toCharArray());
            }

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, keyStorePassword.toCharArray());

            return kmf.getKeyManagers();
        }
        return null;
    }
}
