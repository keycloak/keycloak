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

package org.keycloak.storage.ldap.idm.store.ldap;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Comparator;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.jboss.logging.Logger;

/**
 * SSLSocketFactory for LDAP SASL EXTERNAL connections using client certificates
 * from a keystore specified by the javax.net.ssl.keyStore system property.
 * Automatically reloads the keystore when file modifications are detected.
 */
public class LDAPSSLSocketFactory extends SSLSocketFactory implements Comparator<String> {

    private static final Logger logger = Logger.getLogger(LDAPSSLSocketFactory.class);

    // Keystore file modification time is checked at most every CHECK_INTERVAL_MS milliseconds to reduce file system access.
    private static final long CHECK_INTERVAL_MS = 10_000;

    private static volatile SSLSocketFactory cachedFactory;
    private static volatile long lastCheckedMillis;
    private static long cachedKeyStoreMtime;

    public static SSLSocketFactory getDefault() {
        return new LDAPSSLSocketFactory();
    }

    private SSLSocketFactory delegate() {
        long now = System.currentTimeMillis();
        if (cachedFactory != null && (now - lastCheckedMillis) < CHECK_INTERVAL_MS) {
            return cachedFactory;
        }
        return reloadIfNeeded();
    }

    private static synchronized SSLSocketFactory reloadIfNeeded() {
        long now = System.currentTimeMillis();
        if (cachedFactory != null && (now - lastCheckedMillis) < CHECK_INTERVAL_MS) {
            return cachedFactory;
        }

        String keyStorePath = System.getProperty("javax.net.ssl.keyStore");
        if (keyStorePath == null) {
            throw new IllegalStateException("javax.net.ssl.keyStore system property is not set");
        }

        try {
            long currentMtime = Files.getLastModifiedTime(Path.of(keyStorePath)).toMillis();

            if (cachedFactory != null && currentMtime == cachedKeyStoreMtime) {
                lastCheckedMillis = now;
                return cachedFactory;
            }

            logger.debug("Loading keystore for LDAP client certificate authentication");

            String keyStorePassword = System.getProperty("javax.net.ssl.keyStorePassword", "");
            String keyStoreType = System.getProperty("javax.net.ssl.keyStoreType", KeyStore.getDefaultType());

            KeyStore ks = KeyStore.getInstance(keyStoreType);
            try (InputStream is = Files.newInputStream(Path.of(keyStorePath))) {
                ks.load(is, keyStorePassword.toCharArray());
            }

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, keyStorePassword.toCharArray());

            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(kmf.getKeyManagers(), null, null);

            cachedFactory = ctx.getSocketFactory();
            cachedKeyStoreMtime = currentMtime;
            lastCheckedMillis = now;
            return cachedFactory;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create SSL context for LDAP client certificate authentication", e);
        }
    }

    @Override
    public Socket createSocket() throws IOException {
        return delegate().createSocket();
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return delegate().createSocket(host, port);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return delegate().createSocket(host, port, localHost, localPort);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return delegate().createSocket(host, port);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return delegate().createSocket(address, port, localAddress, localPort);
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return delegate().createSocket(s, host, port, autoClose);
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate().getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate().getSupportedCipherSuites();
    }

    // Required by JNDI LDAP connection pooling (com.sun.jndi.ldap.ClientId).
    // JNDI reflectively calls compare(Object, Object) with the socket factory class names as strings
    // to determine if two connections can share a pool. Returning 0 means connections are poolable together.
    @Override
    public int compare(String o1, String o2) {
        return o1.compareTo(o2);
    }
}
