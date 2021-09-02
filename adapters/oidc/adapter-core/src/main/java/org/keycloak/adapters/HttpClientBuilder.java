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

package org.keycloak.adapters;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.keycloak.common.util.EnvUtil;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.representations.adapters.config.AdapterHttpClientConfig;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

/**
 * Abstraction for creating HttpClients. Allows SSL configuration.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class HttpClientBuilder {
    public enum HostnameVerificationPolicy {
        /**
         * Hostname verification is not done on the server's certificate
         */
        ANY,
        /**
         * Hostname verification for the server's certificate
         */
        DEFAULT
    }

    /**
     * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
     * @version $Revision: 1 $
     */
    private static class PassthroughTrustManager implements X509TrustManager {
        public void checkClientTrusted(X509Certificate[] chain,
                                       String authType) throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] chain,
                                       String authType) throws CertificateException {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }

    protected KeyStore truststore;
    protected KeyStore clientKeyStore;
    protected String clientPrivateKeyPassword;
    protected boolean disableTrustManager;
    protected boolean disableCookieCache = true;
    protected HostnameVerificationPolicy policy = HostnameVerificationPolicy.DEFAULT;
    protected SSLContext sslContext;
    protected int connectionPoolSize = 128;
    protected int maxPooledPerRoute = 64;
    protected long connectionTTL = -1;
    protected TimeUnit connectionTTLUnit = TimeUnit.MILLISECONDS;
    protected HostnameVerifier verifier = null;
    protected long socketTimeout = -1;
    protected TimeUnit socketTimeoutUnits = TimeUnit.MILLISECONDS;
    protected long establishConnectionTimeout = -1;
    protected TimeUnit establishConnectionTimeoutUnits = TimeUnit.MILLISECONDS;
    protected HttpHost proxyHost;


    /**
     * Socket inactivity timeout
     *
     * @param timeout
     * @param unit
     * @return
     */
    public HttpClientBuilder socketTimeout(long timeout, TimeUnit unit) {
        this.socketTimeout = timeout;
        this.socketTimeoutUnits = unit;
        return this;
    }

    /**
     * When trying to make an initial socket connection, what is the timeout?
     *
     * @param timeout
     * @param unit
     * @return
     */
    public HttpClientBuilder establishConnectionTimeout(long timeout, TimeUnit unit) {
        this.establishConnectionTimeout = timeout;
        this.establishConnectionTimeoutUnits = unit;
        return this;
    }

    public HttpClientBuilder connectionTTL(long ttl, TimeUnit unit) {
        this.connectionTTL = ttl;
        this.connectionTTLUnit = unit;
        return this;
    }

    public HttpClientBuilder maxPooledPerRoute(int maxPooledPerRoute) {
        this.maxPooledPerRoute = maxPooledPerRoute;
        return this;
    }

    public HttpClientBuilder connectionPoolSize(int connectionPoolSize) {
        this.connectionPoolSize = connectionPoolSize;
        return this;
    }

    /**
     * Disable trust management and hostname verification. <i>NOTE</i> this is a security
     * hole, so only set this option if you cannot or do not want to verify the identity of the
     * host you are communicating with.
     */
    public HttpClientBuilder disableTrustManager() {
        this.disableTrustManager = true;
        return this;
    }

    public HttpClientBuilder disableCookieCache(boolean disable) {
        this.disableCookieCache = disable;
        return this;
    }

    /**
     * SSL policy used to verify hostnames
     *
     * @param policy
     * @return
     */
    public HttpClientBuilder hostnameVerification(HostnameVerificationPolicy policy) {
        this.policy = policy;
        return this;
    }


    public HttpClientBuilder sslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
        return this;
    }

    public HttpClientBuilder trustStore(KeyStore truststore) {
        this.truststore = truststore;
        return this;
    }

    public HttpClientBuilder keyStore(KeyStore keyStore, String password) {
        this.clientKeyStore = keyStore;
        this.clientPrivateKeyPassword = password;
        return this;
    }

    public HttpClientBuilder keyStore(KeyStore keyStore, char[] password) {
        this.clientKeyStore = keyStore;
        this.clientPrivateKeyPassword = new String(password);
        return this;
    }

    public HttpClient build() {
        HostnameVerifier verifier = getHostnameVerifier();

        try {
            SSLConnectionSocketFactory sslCSF;
            SSLContext theContext = sslContext;
            if (disableTrustManager) {
                theContext = SSLContext.getInstance(SSLConnectionSocketFactory.TLS);
                theContext.init(null, new TrustManager[]{new PassthroughTrustManager()},
                        new SecureRandom());
                verifier = new DefaultHostnameVerifier();
                sslCSF = new SSLConnectionSocketFactory(theContext, verifier);
            } else if (theContext != null) {
                sslCSF = new SSLConnectionSocketFactory(theContext, verifier);
            } else if (clientKeyStore != null || truststore != null) {
                theContext = createSslContext(SSLConnectionSocketFactory.TLS, clientKeyStore, clientPrivateKeyPassword, truststore, null);
                sslCSF = new SSLConnectionSocketFactory(theContext, verifier);
            } else {
                final SSLContext tlsContext = SSLContext.getInstance(SSLConnectionSocketFactory.TLS);
                tlsContext.init(null, null, null);
                sslCSF = new SSLConnectionSocketFactory(tlsContext, verifier);
            }

            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", sslCSF)
                    .build();

            HttpClientConnectionManager cm;
            if (connectionPoolSize > 0) {
                PoolingHttpClientConnectionManager pcm = new PoolingHttpClientConnectionManager(registry, null, null, null, connectionTTL, connectionTTLUnit);
                pcm.setMaxTotal(connectionPoolSize);
                if (maxPooledPerRoute == 0) maxPooledPerRoute = connectionPoolSize;
                pcm.setDefaultMaxPerRoute(maxPooledPerRoute);
                cm = pcm;
            } else {
                cm = new BasicHttpClientConnectionManager(registry);
            }

            RequestConfig.Builder requestConfigBuilder = RequestConfig.custom().setCookieSpec(CookieSpecs.DEFAULT);

            if (proxyHost != null) {
                requestConfigBuilder.setProxy(proxyHost);
            }

            if (socketTimeout > -1) {
                requestConfigBuilder.setSocketTimeout((int) socketTimeoutUnits.toMillis(socketTimeout));
            }

            if (establishConnectionTimeout > -1) {
                requestConfigBuilder.setConnectTimeout((int) establishConnectionTimeoutUnits.toMillis(establishConnectionTimeout));
            }

            org.apache.http.impl.client.HttpClientBuilder builder = HttpClients.custom()
                    .setDefaultRequestConfig(requestConfigBuilder.build())
                    .setSSLSocketFactory(sslCSF)
                    .setConnectionManager(cm)
                    .setMaxConnTotal(connectionPoolSize)
                    .setMaxConnPerRoute(maxPooledPerRoute)
                    .setConnectionTimeToLive(connectionTTL, connectionTTLUnit);

            if (disableCookieCache) {
                builder.setDefaultCookieStore(new EmptyCookieStore());
            }

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public HttpClient build(AdapterHttpClientConfig adapterConfig) {
        disableCookieCache(true); // disable cookie cache as we don't want sticky sessions for load balancing

        String truststorePath = adapterConfig.getTruststore();
        if (truststorePath != null) {
            truststorePath = EnvUtil.replace(truststorePath);
            String truststorePassword = adapterConfig.getTruststorePassword();
            try {
                this.truststore = KeystoreUtil.loadKeyStore(truststorePath, truststorePassword);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load truststore", e);
            }
        }

        String clientKeystore = adapterConfig.getClientKeystore();
        if (clientKeystore != null) {
            clientKeystore = EnvUtil.replace(clientKeystore);
            String clientKeystorePassword = adapterConfig.getClientKeystorePassword();
            try {
                KeyStore clientCertKeystore = KeystoreUtil.loadKeyStore(clientKeystore, clientKeystorePassword);
                keyStore(clientCertKeystore, clientKeystorePassword);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load keystore", e);
            }
        }
        int size = 10;
        if (adapterConfig.getConnectionPoolSize() > 0) {
            size = adapterConfig.getConnectionPoolSize();
        }

        HttpClientBuilder.HostnameVerificationPolicy policy = HostnameVerificationPolicy.DEFAULT;

        if (adapterConfig.isAllowAnyHostname()) {
            policy = HttpClientBuilder.HostnameVerificationPolicy.ANY;
        }

        connectionPoolSize(size);
        hostnameVerification(policy);

        if (adapterConfig.isDisableTrustManager()) {
            disableTrustManager();
        } else {
            trustStore(truststore);
        }

        configureProxyForAuthServerIfProvided(adapterConfig);

        if (socketTimeout == -1 && adapterConfig.getSocketTimeout() > 0) {
            socketTimeout(adapterConfig.getSocketTimeout(), TimeUnit.MILLISECONDS);
        }

        if (establishConnectionTimeout == -1 && adapterConfig.getConnectionTimeout() > 0) {
            establishConnectionTimeout(adapterConfig.getConnectionTimeout(), TimeUnit.MILLISECONDS);
        }

        if (connectionTTL == -1 && adapterConfig.getConnectionTTL() > 0) {
            connectionTTL(adapterConfig.getConnectionTTL(), TimeUnit.MILLISECONDS);
        }

        return build();
    }

    /**
     * Configures a the proxy to use for auth-server requests if provided.
     * <p>
     * If the given {@link AdapterHttpClientConfig} contains the attribute {@code proxy-url} we use the
     * given URL as a proxy server, otherwise the proxy configuration is ignored.
     * </p>
     *
     * @param adapterConfig
     */
    private void configureProxyForAuthServerIfProvided(AdapterHttpClientConfig adapterConfig) {

        if (adapterConfig == null || adapterConfig.getProxyUrl() == null || adapterConfig.getProxyUrl().trim().isEmpty()) {
            return;
        }

        URI uri = URI.create(adapterConfig.getProxyUrl());
        this.proxyHost = new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
    }

    private SSLContext createSslContext(
            final String algorithm,
            final KeyStore keystore,
            final String keyPassword,
            final KeyStore truststore,
            final SecureRandom random)
            throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        return SSLContexts.custom()
                .setProtocol(algorithm)
                .setSecureRandom(random)
                .loadKeyMaterial(keystore, keyPassword != null ? keyPassword.toCharArray() : null)
                .loadTrustMaterial(truststore, null)
                .build();
    }

    /**
     * Get hostname verifier for SSLConnectionSocketFactory
     *
     * @return HostnameVerifier verifier
     */
    private HostnameVerifier getHostnameVerifier() {
        if (this.verifier != null) {
            return this.verifier;
        } else {
            switch (policy) {
                case ANY:
                    return new NoopHostnameVerifier();
                case DEFAULT:
                    return new DefaultHostnameVerifier();
            }
        }
        return null;
    }
}