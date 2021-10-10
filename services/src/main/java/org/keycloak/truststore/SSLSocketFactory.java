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

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Comparator;


/**
 * Using this class is ugly, but it is the only way to push our truststore to the default LDAP client implementation.
 * <p>
 * This SSLSocketFactory can only use truststore configured by TruststoreProvider after the ProviderFactory was
 * initialized using standard Spi load / init mechanism. That will only happen if "truststore" provider is configured
 * in standalone.xml or domain.xml.
 * <p>
 * If TruststoreProvider is not available this SSLSocketFactory will delegate all operations to javax.net.ssl.SSLSocketFactory.getDefault().
 *
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */

public class SSLSocketFactory extends javax.net.ssl.SSLSocketFactory implements Comparator {

    private static final Logger log = Logger.getLogger(SSLSocketFactory.class);

    private static SSLSocketFactory instance;

    private final javax.net.ssl.SSLSocketFactory sslsf;

    private SSLSocketFactory() {

        TruststoreProvider provider = TruststoreProviderSingleton.get();
        javax.net.ssl.SSLSocketFactory sf = null;
        if (provider != null) {
            sf = new JSSETruststoreConfigurator(provider).getSSLSocketFactory();
        }

        if (sf == null) {
            log.info("No truststore provider found - using default SSLSocketFactory");
            sf = (javax.net.ssl.SSLSocketFactory) javax.net.ssl.SSLSocketFactory.getDefault();
        }

        sslsf = sf;
    }

    public static synchronized SSLSocketFactory getDefault() {
        if (instance == null) {
            instance = new SSLSocketFactory();
        }
        return instance;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return sslsf.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return sslsf.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
        return sslsf.createSocket(socket, host, port, autoClose);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return sslsf.createSocket(host, port);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return sslsf.createSocket(host, port, localHost, localPort);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return sslsf.createSocket(host, port);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return sslsf.createSocket(address, port, localAddress, localPort);
    }

    @Override
    public Socket createSocket() throws IOException {
        return sslsf.createSocket();
    }

    @Override
    public int compare(Object socketFactory1, Object socketFactory2) {
        return socketFactory1.equals(socketFactory2) ? 0 : -1;
    }
}
