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
import java.net.InetAddress;
import java.net.Socket;
import java.util.Comparator;
import java.util.function.Supplier;
import javax.net.ssl.SSLSocketFactory;

/**
 * SSLSocketFactory for LDAP connections that obtains a fresh SSLSocketFactory
 * from a configured supplier on each createSocket() call.
 */
public class LDAPSSLSocketFactory extends SSLSocketFactory implements Comparator<String> {

    private static volatile Supplier<SSLSocketFactory> sslSocketFactorySupplier;

    public static void setSSLSocketFactorySupplier(Supplier<SSLSocketFactory> supplier) {
        sslSocketFactorySupplier = supplier;
    }

    public static SSLSocketFactory getDefault() {
        return new LDAPSSLSocketFactory();
    }

    private SSLSocketFactory delegate() {
        Supplier<SSLSocketFactory> supplier = sslSocketFactorySupplier;
        if (supplier == null) {
            throw new IllegalStateException("LDAPSSLSocketFactory has no SSLSocketFactory supplier configured");
        }
        return supplier.get();
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

    @Override
    public int compare(String o1, String o2) {
        return o1.compareTo(o2);
    }
}
