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
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.truststore.TruststoreProvider;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;

import static org.keycloak.utils.StringUtil.isBlank;

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
    private static final String configScope = "keycloak.connectionsHttpClient.default.";

    private static final String HTTPS_PROXY = "https_proxy";
    private static final String HTTP_PROXY = "http_proxy";
    private static final String NO_PROXY = "no_proxy";

    private volatile CloseableHttpClient httpClient;
    private Config.Scope config;

    @Override
    public HttpClientProvider create(KeycloakSession session) {
        lazyInit(session);

        return new HttpClientProvider() {
            @Override
            public CloseableHttpClient getHttpClient() {
                return httpClient;
            }

            @Override
            public void close() {

            }

            @Override
            public int postText(String uri, String text) throws IOException {
                HttpPost request = new HttpPost(uri);
                request.setEntity(EntityBuilder.create().setText(text).setContentType(ContentType.TEXT_PLAIN).build());
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    try {
                        return response.getStatusLine().getStatusCode();
                    } finally {
                        EntityUtils.consumeQuietly(response.getEntity());
                    }
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                    throw t;
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
                    long socketTimeout = config.getLong("socket-timeout-millis", 5000L);
                    long establishConnectionTimeout = config.getLong("establish-connection-timeout-millis", -1L);
                    int maxPooledPerRoute = config.getInt("max-pooled-per-route", 64);
                    int connectionPoolSize = config.getInt("connection-pool-size", 128);
                    long connectionTTL = config.getLong("connection-ttl-millis", -1L);
                    boolean reuseConnections = config.getBoolean("reuse-connections", true);
                    long maxConnectionIdleTime = config.getLong("max-connection-idle-time-millis", 900000L);
                    boolean disableCookies = config.getBoolean("disable-cookies", true);
                    String clientKeystore = config.get("client-keystore");
                    String clientKeystorePassword = config.get("client-keystore-password");
                    String clientPrivateKeyPassword = config.get("client-key-password");
                    boolean disableTrustManager = config.getBoolean("disable-trust-manager", false);

                    boolean expectContinueEnabled = getBooleanConfigWithSysPropFallback("expect-continue-enabled", false);
                    boolean resuseConnections = getBooleanConfigWithSysPropFallback("reuse-connections", true);

                    // optionally configure proxy mappings
                    // direct SPI config (e.g. via standalone.xml) takes precedence over env vars
                    // lower case env vars take precedence over upper case env vars
                    ProxyMappings proxyMappings = ProxyMappings.valueOf(config.getArray("proxy-mappings"));
                    if (proxyMappings == null || proxyMappings.isEmpty()) {
                        logger.debug("Trying to use proxy mapping from env vars");
                        String httpProxy = getEnvVarValue(HTTPS_PROXY);
                        if (isBlank(httpProxy)) {
                            httpProxy = getEnvVarValue(HTTP_PROXY);
                        }
                        String noProxy = getEnvVarValue(NO_PROXY);

                        logger.debugf("httpProxy: %s, noProxy: %s", httpProxy, noProxy);
                        proxyMappings = ProxyMappings.withFixedProxyMapping(httpProxy, noProxy);
                    }

                    HttpClientBuilder builder = new HttpClientBuilder();

                    builder.socketTimeout(socketTimeout, TimeUnit.MILLISECONDS)
                            .establishConnectionTimeout(establishConnectionTimeout, TimeUnit.MILLISECONDS)
                            .maxPooledPerRoute(maxPooledPerRoute)
                            .connectionPoolSize(connectionPoolSize)
                            .reuseConnections(reuseConnections)
                            .connectionTTL(connectionTTL, TimeUnit.MILLISECONDS)
                            .maxConnectionIdleTime(maxConnectionIdleTime, TimeUnit.MILLISECONDS)
                            .disableCookies(disableCookies)
                            .proxyMappings(proxyMappings)
                            .expectContinueEnabled(expectContinueEnabled)
                            .reuseConnections(resuseConnections);

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

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name("socket-timeout-millis")
                .type("long")
                .helpText("Socket inactivity timeout.")
                .defaultValue(5000L)
                .add()
                .property()
                .name("establish-connection-timeout-millis")
                .type("long")
                .helpText("When trying to make an initial socket connection, what is the timeout?")
                .defaultValue(-1L)
                .add()
                .property()
                .name("max-pooled-per-route")
                .type("int")
                .helpText("Assigns maximum connection per route value.")
                .defaultValue(64)
                .add()
                .property()
                .name("connection-pool-size")
                .type("int")
                .helpText("Assigns maximum total connection value.")
                .add()
                .property()
                .name("connection-ttl-millis")
                .type("long")
                .helpText("Sets maximum time, in milliseconds, to live for persistent connections.")
                .defaultValue(-1L)
                .add()
                .property()
                .name("reuse-connections")
                .type("boolean")
                .helpText("If connections should be reused.")
                .defaultValue(true)
                .add()
                .property()
                .name("max-connection-idle-time-millis")
                .type("long")
                .helpText("Sets the time, in milliseconds, for evicting idle connections from the pool.")
                .defaultValue(900000)
                .add()
                .property()
                .name("disable-cookies")
                .type("boolean")
                .helpText("Disables state (cookie) management.")
                .defaultValue(true)
                .add()
                .property()
                .name("client-keystore")
                .type("string")
                .helpText("The file path of the key store from where the key material is going to be read from to set-up TLS connections.")
                .add()
                .property()
                .name("client-keystore-password")
                .type("string")
                .helpText("The key store password.")
                .add()
                .property()
                .name("client-key-password")
                .type("string")
                .helpText("The key password.")
                .defaultValue(-1L)
                .add()
                .property()
                .name("disable-trust-manager")
                .type("boolean")
                .helpText("Disable trust management and hostname verification. NOTE this is a security hole, so only set this option if you cannot or do not want to verify the identity of the host you are communicating with.")
                .defaultValue(false)
                .add()
                .property()
                .name("proxy-mappings")
                .type("string")
                .helpText("Denotes the combination of a regex based hostname pattern and a proxy-uri in the form of hostnamePattern;proxyUri.")
                .add()
                .build();
    }

    private boolean getBooleanConfigWithSysPropFallback(String key, boolean defaultValue) {
        Boolean value = config.getBoolean(key);
        if (value == null) {
            String s = System.getProperty(configScope + key);
            if (s != null) {
                value = Boolean.parseBoolean(s);
            }
        }
        return value != null ? value : defaultValue;
    }

    private String getEnvVarValue(String name) {
        String value = System.getenv(name.toLowerCase());
        if (isBlank(value)) {
            value = System.getenv(name.toUpperCase());
        }
        return value;
    }

}
