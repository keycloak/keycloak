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

package org.keycloak.connections.httpclient;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.util.EnvUtil;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.truststore.TruststoreProvider;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;

/**
 * The default {@link HttpClientFactory} for {@link HttpClientProvider HttpClientProvider's} used by Keycloak for outbound HTTP calls.
 * <p>
 * The constructed clients can be configured via Keycloaks SPI configuration, e.g. {@code standalone.xml, standalone-ha.xml, domain.xml}.
 * </p>
 * <p>
 * Examples for jboss-cli
 * </p>
 * <pre>
 * {@code
 *
 * /subsystem=keycloak-server/spi=connectionsHttpClient/provider=default:add(enabled=true)
 * /subsystem=keycloak-server/spi=connectionsHttpClient/provider=default:write-attribute(name=properties.connection-pool-size,value=128)
 * /subsystem=keycloak-server/spi=connectionsHttpClient/provider=default:write-attribute(name=properties.proxy-mappings,value=[".*\\.(google|googleapis)\\.com;http://www-proxy.acme.corp.com:8080",".*\\.acme\\.corp\\.com;NO_PROXY",".*;http://fallback:8080"])
 * }
 * </pre>
 * </p>
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultHttpClientFactory implements HttpClientFactory {

    private static final Logger logger = Logger.getLogger(DefaultHttpClientFactory.class);

    private volatile CloseableHttpClient httpClient;
    private Config.Scope config;

    @Override
    public HttpClientProvider create(KeycloakSession session) {
        lazyInit(session);

        return new HttpClientProvider() {
            @Override
            public HttpClient getHttpClient() {
                return httpClient;
            }

            @Override
            public void close() {

            }

            @Override
            public int postText(String uri, String text) throws IOException {
                HttpPost request = new HttpPost(uri);
                request.setEntity(EntityBuilder.create().setText(text).setContentType(ContentType.TEXT_PLAIN).build());
                HttpResponse response = httpClient.execute(request);
                try {
                    return response.getStatusLine().getStatusCode();
                } finally {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        InputStream is = entity.getContent();
                        if (is != null) is.close();
                    }

                }
            }

            @Override
            public InputStream get(String uri) throws IOException {
                HttpGet request = new HttpGet(uri);
                HttpResponse response = httpClient.execute(request);
                HttpEntity entity = response.getEntity();
                if (entity == null) return null;
                return entity.getContent();

            }
        };
    }

    @Override
    public void close() {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
        } catch (IOException e) {

        }
    }

    @Override
    public String getId() {
        return "default";
    }

    @Override
    public void init(Config.Scope config) {
        this.config = config;
    }

    private void lazyInit(KeycloakSession session) {
        if (httpClient == null) {
            synchronized(this) {
                if (httpClient == null) {
                    long socketTimeout = config.getLong("socket-timeout-millis", -1L);
                    long establishConnectionTimeout = config.getLong("establish-connection-timeout-millis", -1L);
                    int maxPooledPerRoute = config.getInt("max-pooled-per-route", 64);
                    int connectionPoolSize = config.getInt("connection-pool-size", 128);
                    long connectionTTL = config.getLong("connection-ttl-millis", -1L);
                    long maxConnectionIdleTime = config.getLong("max-connection-idle-time-millis", 900000L);
                    boolean disableCookies = config.getBoolean("disable-cookies", true);
                    String clientKeystore = config.get("client-keystore");
                    String clientKeystorePassword = config.get("client-keystore-password");
                    String clientPrivateKeyPassword = config.get("client-key-password");
                    String[] proxyMappings = config.getArray("proxy-mappings");
                    boolean disableTrustManager = config.getBoolean("disable-trust-manager", false);
                    
                    HttpClientBuilder builder = new HttpClientBuilder();

                    builder.socketTimeout(socketTimeout, TimeUnit.MILLISECONDS)
                            .establishConnectionTimeout(establishConnectionTimeout, TimeUnit.MILLISECONDS)
                            .maxPooledPerRoute(maxPooledPerRoute)
                            .connectionPoolSize(connectionPoolSize)
                            .connectionTTL(connectionTTL, TimeUnit.MILLISECONDS)
                            .maxConnectionIdleTime(maxConnectionIdleTime, TimeUnit.MILLISECONDS)
                            .disableCookies(disableCookies)
                            .proxyMappings(ProxyMappings.valueOf(proxyMappings));

                    TruststoreProvider truststoreProvider = session.getProvider(TruststoreProvider.class);
                    boolean disableTruststoreProvider = truststoreProvider == null || truststoreProvider.getTruststore() == null;
                    
                    if (disableTruststoreProvider) {
                    	logger.warn("TruststoreProvider is disabled");
                    } else {
                        builder.hostnameVerification(HttpClientBuilder.HostnameVerificationPolicy.valueOf(truststoreProvider.getPolicy().name()));
                        try {
                            builder.trustStore(truststoreProvider.getTruststore());
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to load truststore", e);
                        }
                    }

                    if (disableTrustManager) {
                    	logger.warn("TrustManager is disabled");
                    	builder.disableTrustManager();
                    }
                    
                    if (clientKeystore != null) {
                        clientKeystore = EnvUtil.replace(clientKeystore);
                        try {
                            KeyStore clientCertKeystore = KeystoreUtil.loadKeyStore(clientKeystore, clientKeystorePassword);
                            builder.keyStore(clientCertKeystore, clientPrivateKeyPassword);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to load keystore", e);
                        }
                    }
                    httpClient = builder.build();
                }
            }
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }



}
