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

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.keycloak.Config;
import org.keycloak.common.util.EnvUtil;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.truststore.TruststoreProvider;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.AbstractResponseHandler;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;

import static org.keycloak.utils.StringUtil.isBlank;

/**
 * The default {@link HttpClientFactory} for {@link HttpClientProvider HttpClientProvider's} used by Keycloak for outbound HTTP calls.
 * <p>
 * Example for Quarkus configuration:
 * <p>
 * {@code
 * spi-connections-http-client-default-connection-pool-size=10
 * }
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultHttpClientFactory implements HttpClientFactory {

    private static final Logger logger = Logger.getLogger(DefaultHttpClientFactory.class);
    private static final String configScope = "keycloak.connectionsHttpClient.default.";

    private static final String HTTPS_PROXY = "https_proxy";
    private static final String HTTP_PROXY = "http_proxy";
    private static final String NO_PROXY = "no_proxy";
    public static final String MAX_CONSUMED_RESPONSE_SIZE = "max-consumed-response-size";

    private volatile CloseableHttpClient httpClient;
    private Config.Scope config;

    private BasicResponseHandler stringResponseHandler;

    private final InputStreamResponseHandler inputStreamResponseHandler = new InputStreamResponseHandler();
    private long maxConsumedResponseSize;

    private static class InputStreamResponseHandler extends AbstractResponseHandler<InputStream> {

        public InputStream handleEntity(HttpEntity entity) throws IOException {
            return entity.getContent();
        }

        public InputStream handleResponse(HttpResponse response) throws IOException {
            return super.handleResponse(response);
        }
    }

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
            public String getString(String uri) throws IOException {
                HttpGet request = new HttpGet(uri);
                HttpResponse response = httpClient.execute(request);
                String body = stringResponseHandler.handleResponse(response);
                if (body == null) {
                    throw new IOException("No content returned from HTTP call");
                }
                return body;
            }

            @Override
            public InputStream getInputStream(String uri) throws IOException {
                HttpGet request = new HttpGet(uri);
                HttpResponse response = httpClient.execute(request);
                InputStream body = inputStreamResponseHandler.handleResponse(response);
                if (body == null) {
                    throw new IOException("No content returned from HTTP call");
                }
                return body;
            }

            @Override
            public long getMaxConsumedResponseSize() {
                return maxConsumedResponseSize;
            }
        };
    }

    @Override
    public void close() {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
        } catch (IOException ignored) {

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
                    long maxConnectionIdleTime = config.getLong("max-connection-idle-time-millis", 900000L);
                    boolean disableCookies = config.getBoolean("disable-cookies", true);
                    String clientKeystore = config.get("client-keystore");
                    String clientKeystorePassword = config.get("client-keystore-password");
                    String clientPrivateKeyPassword = config.get("client-key-password");
                    boolean disableTrustManager = config.getBoolean("disable-trust-manager", false);

                    boolean expectContinueEnabled = getBooleanConfigWithSysPropFallback("expect-continue-enabled", false);
                    boolean reuseConnections = getBooleanConfigWithSysPropFallback("reuse-connections", true);

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

                    HttpClientBuilder builder = newHttpClientBuilder();

                    builder.socketTimeout(socketTimeout, TimeUnit.MILLISECONDS)
                            .establishConnectionTimeout(establishConnectionTimeout, TimeUnit.MILLISECONDS)
                            .maxPooledPerRoute(maxPooledPerRoute)
                            .connectionPoolSize(connectionPoolSize)
                            .connectionTTL(connectionTTL, TimeUnit.MILLISECONDS)
                            .maxConnectionIdleTime(maxConnectionIdleTime, TimeUnit.MILLISECONDS)
                            .disableCookies(disableCookies)
                            .proxyMappings(proxyMappings)
                            .expectContinueEnabled(expectContinueEnabled)
                            .reuseConnections(reuseConnections);

                    TruststoreProvider truststoreProvider = session.getProvider(TruststoreProvider.class);
                    boolean disableTruststoreProvider = truststoreProvider == null || truststoreProvider.getTruststore() == null;

                    if (disableTruststoreProvider) {
                    	logger.warn("TruststoreProvider is disabled");
                    } else {
                        builder.hostnameVerification(truststoreProvider.getPolicy());
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

                    // Configure retry behavior
                    configureRetries(builder);

                    httpClient = builder.build();
                }
            }
        }
    }

    /**
     * Configures retry behavior for the HTTP client builder.
     * Applies server-wide retry configuration if enabled.
     *
     * @param builder The HTTP client builder to configure
     */
    private void configureRetries(HttpClientBuilder builder) {
        int maxRetries = config.getInt("max-retries", 0);
        if (maxRetries <= 0) {
            return; // Retries disabled
        }
        // Always enable request-sent retries for common requests (e.g., GET, POST)
        long initialBackoffMillis = config.getLong("initial-backoff-millis", 1000L);
        String backoffMultiplierStr = config.get("backoff-multiplier", "2.0");
        double backoffMultiplier = Double.parseDouble(backoffMultiplierStr);
        boolean useJitter = config.getBoolean("use-jitter", true);
        String jitterFactorStr = config.get("jitter-factor", "0.5");
        double jitterFactor = Double.parseDouble(jitterFactorStr);

        builder.getApacheHttpClientBuilder().setRetryHandler(
                new org.apache.http.impl.client.DefaultHttpRequestRetryHandler(maxRetries, true) {
                    @Override
                    public boolean retryRequest(IOException exception, int executionCount,
                            org.apache.http.protocol.HttpContext context) {
                        boolean shouldRetry = super.retryRequest(exception, executionCount, context);
                        if (shouldRetry) {
                            try {
                                long baseDelay = initialBackoffMillis *
                                        (long)Math.pow(backoffMultiplier, executionCount - 1);
                                long delay = baseDelay;
                                if (useJitter) {
                                    double jitter = 1.0 - jitterFactor +
                                            (Math.random() * jitterFactor * 2.0);
                                    delay = (long)(baseDelay * jitter);
                                }
                                Thread.sleep(delay);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                        return shouldRetry;
                    }
                });
    }

    protected HttpClientBuilder newHttpClientBuilder() {
        return new HttpClientBuilder();
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        maxConsumedResponseSize = config.getLong(MAX_CONSUMED_RESPONSE_SIZE, HttpClientProvider.DEFAULT_MAX_CONSUMED_RESPONSE_SIZE);
        stringResponseHandler = new SafeBasicResponseHandler(maxConsumedResponseSize);
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
                .property()
                .name(MAX_CONSUMED_RESPONSE_SIZE)
                .type("long")
                .helpText("Maximum size of a response consumed by the client (to prevent denial of service)")
                .defaultValue(HttpClientProvider.DEFAULT_MAX_CONSUMED_RESPONSE_SIZE)
                .add()
                .property()
                .name("max-retries")
                .type("int")
                .helpText("Maximum number of retry attempts for all outgoing HTTP requests. Set to 0 to disable retries (default).")
                .defaultValue(0)
                .add()
                .property()
                .name("initial-backoff-millis")
                .type("long")
                .helpText("Initial backoff time in milliseconds before the first retry attempt.")
                .defaultValue(1000L)
                .add()
                .property()
                .name("backoff-multiplier")
                .type("string")
                .helpText(
                        "Multiplier for exponential backoff between retry attempts. For example, with an initial backoff of 1000ms and a multiplier of 2.0, the retry delays would be: 1000ms, 2000ms, 4000ms, etc.")
                .defaultValue("2.0")
                .add()
                .property()
                .name("use-jitter")
                .type("boolean")
                .helpText(
                        "Whether to apply jitter to backoff times to prevent synchronized retry storms when multiple clients are retrying at the same time.")
                .defaultValue(true)
                .add()
                .property()
                .name("jitter-factor")
                .type("string")
                .helpText(
                        "Jitter factor to apply to backoff times. A value of 0.5 means the actual backoff time will be between 50% and 150% of the calculated exponential backoff time.")
                .defaultValue("0.5")
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

    // For testing purposes
    public Config.Scope getConfig() {
        return config;
    }
}
