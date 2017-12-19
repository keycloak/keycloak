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

package org.keycloak.adapters.cloned;

import org.apache.http.HttpHost;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.keycloak.common.util.EnvUtil;
import org.keycloak.common.util.KeystoreUtil;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;

/**
 * Abstraction for creating HttpClients. Allows SSL configuration.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class HttpClientBuilder {
    public static enum HostnameVerificationPolicy {
        /**
         * Hostname verification is not done on the server's certificate
         */
        ANY,
        /**
         * Allows wildcards in subdomain names i.e. *.foo.com
         */
        WILDCARD,
        /**
         * CN must match hostname connecting to
         */
        STRICT
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
    protected HostnameVerificationPolicy policy = HostnameVerificationPolicy.WILDCARD;
    protected SSLContext sslContext;
    protected int connectionPoolSize = 100;
    protected int maxPooledPerRoute = 0;
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

    public HttpClientBuilder disableCookieCache() {
        this.disableCookieCache = true;
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


    static class VerifierWrapper implements X509HostnameVerifier {
        protected HostnameVerifier verifier;

        VerifierWrapper(HostnameVerifier verifier) {
            this.verifier = verifier;
        }

        @Override
        public void verify(String host, SSLSocket ssl) throws IOException {
            if (!verifier.verify(host, ssl.getSession())) throw new SSLException("Hostname verification failure");
        }

        @Override
        public void verify(String host, X509Certificate cert) throws SSLException {
            throw new SSLException("This verification path not implemented");
        }

        @Override
        public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {
            throw new SSLException("This verification path not implemented");
        }

        @Override
        public boolean verify(String s, SSLSession sslSession) {
            return verifier.verify(s, sslSession);
        }
    }

    public HttpClient build() {
        X509HostnameVerifier verifier = null;
        if (this.verifier != null) verifier = new VerifierWrapper(this.verifier);
        else {
            switch (policy) {
                case ANY:
                    verifier = new AllowAllHostnameVerifier();
                    break;
                case WILDCARD:
                    verifier = new BrowserCompatHostnameVerifier();
                    break;
                case STRICT:
                    verifier = new StrictHostnameVerifier();
                    break;
            }
        }
        try {
            SSLSocketFactory sslsf = null;
            SSLContext theContext = sslContext;
            if (disableTrustManager) {
                theContext = SSLContext.getInstance("SSL");
                theContext.init(null, new TrustManager[]{new PassthroughTrustManager()},
                        new SecureRandom());
                verifier = new AllowAllHostnameVerifier();
                sslsf = new SniSSLSocketFactory(theContext, verifier);
            } else if (theContext != null) {
                sslsf = new SniSSLSocketFactory(theContext, verifier);
            } else if (clientKeyStore != null || truststore != null) {
                sslsf = new SniSSLSocketFactory(SSLSocketFactory.TLS, clientKeyStore, clientPrivateKeyPassword, truststore, null, verifier);
            } else {
                final SSLContext tlsContext = SSLContext.getInstance(SSLSocketFactory.TLS);
                tlsContext.init(null, null, null);
                sslsf = new SniSSLSocketFactory(tlsContext, verifier);
            }
            SchemeRegistry registry = new SchemeRegistry();
            registry.register(
                    new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
            Scheme httpsScheme = new Scheme("https", 443, sslsf);
            registry.register(httpsScheme);
            ClientConnectionManager cm = null;
            if (connectionPoolSize > 0) {
                ThreadSafeClientConnManager tcm = new ThreadSafeClientConnManager(registry, connectionTTL, connectionTTLUnit);
                tcm.setMaxTotal(connectionPoolSize);
                if (maxPooledPerRoute == 0) maxPooledPerRoute = connectionPoolSize;
                tcm.setDefaultMaxPerRoute(maxPooledPerRoute);
                cm = tcm;

            } else {
                cm = new SingleClientConnManager(registry);
            }
            BasicHttpParams params = new BasicHttpParams();
            params.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);

            if (proxyHost != null) {
                params.setParameter(ConnRoutePNames.DEFAULT_PROXY, proxyHost);
            }

            if (socketTimeout > -1) {
                HttpConnectionParams.setSoTimeout(params, (int) socketTimeoutUnits.toMillis(socketTimeout));

            }
            if (establishConnectionTimeout > -1) {
                HttpConnectionParams.setConnectionTimeout(params, (int) establishConnectionTimeoutUnits.toMillis(establishConnectionTimeout));
            }
            DefaultHttpClient client = new DefaultHttpClient(cm, params);

            if (disableCookieCache) {
                client.setCookieStore(new CookieStore() {
                    @Override
                    public void addCookie(Cookie cookie) {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public List<Cookie> getCookies() {
                        return Collections.emptyList();
                    }

                    @Override
                    public boolean clearExpired(Date date) {
                        return false;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public void clear() {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }
                });

            }
            return client;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public HttpClient build(AdapterHttpClientConfig adapterConfig) {
        disableCookieCache(); // disable cookie cache as we don't want sticky sessions for load balancing

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
        if (adapterConfig.getConnectionPoolSize() > 0)
            size = adapterConfig.getConnectionPoolSize();
        HttpClientBuilder.HostnameVerificationPolicy policy = HttpClientBuilder.HostnameVerificationPolicy.WILDCARD;
        if (adapterConfig.isAllowAnyHostname())
            policy = HttpClientBuilder.HostnameVerificationPolicy.ANY;
        connectionPoolSize(size);
        hostnameVerification(policy);
        if (adapterConfig.isDisableTrustManager()) {
            disableTrustManager();
        } else {
            trustStore(truststore);
        }

        configureProxyForAuthServerIfProvided(adapterConfig);

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
}