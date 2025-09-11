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
import org.apache.http.impl.client.AbstractResponseHandler;
import org.apache.http.impl.client.BasicResponseHandler;
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
    private volatile CloseableHttpClient retriableHttpClient;
    private Config.Scope config;

    private BasicResponseHandler stringResponseHandler;

    private final InputStreamResponseHandler inputStreamResponseHandler = new InputStreamResponseHandler();
    private long maxConsumedResponseSize;

    // Retry configuration
    private RetryConfig defaultRetryConfig;

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
            public CloseableHttpClient getRetriableHttpClient() {
                return retriableHttpClient;
            }

            @Override
            public CloseableHttpClient getRetriableHttpClient(RetryConfig retryConfig) {
                // If using default config, return the cached client
                if (retryConfig == null ||
                    (defaultRetryConfig.getMaxRetries() == retryConfig.getMaxRetries() &&
                     defaultRetryConfig.isRetryOnIOException() == retryConfig.isRetryOnIOException())) {
                    return retriableHttpClient;
                }

                // Otherwise create a new client with the custom config
                return DefaultHttpClientFactory.this.createRetriableHttpClient(session, retryConfig);
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
            if (retriableHttpClient != null && retriableHttpClient != httpClient) {
                retriableHttpClient.close();
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

        // Initialize default retry configuration
        int maxRetries = config.getInt("http-client.default-max-retries", 0); // No retries by default
        boolean retryOnIOException = config.getBoolean("http-client.default-retry-on-io-exception", true);
        long initialBackoffMillis = config.getLong("http-client.default-initial-backoff-millis", 1000L);

        // Get backoff multiplier as a string and convert to double (Config.Scope
        // doesn't have getDouble)
        String backoffMultiplierStr = config.get("http-client.default-backoff-multiplier", "2.0");
        double backoffMultiplier = Double.parseDouble(backoffMultiplierStr);

        // Get jitter settings
        boolean useJitter = config.getBoolean("http-client.default-use-jitter", true);
        String jitterFactorStr = config.get("http-client.default-jitter-factor", "0.5");
        double jitterFactor = Double.parseDouble(jitterFactorStr);

        int connectionTimeoutMillis = config.getInt("http-client.default-connection-timeout-millis", 10000);
        int socketTimeoutMillis = config.getInt("http-client.default-socket-timeout-millis", 10000);

        defaultRetryConfig = new RetryConfig.Builder()
                .maxRetries(maxRetries)
                .retryOnIOException(retryOnIOException)
                .initialBackoffMillis(initialBackoffMillis)
                .backoffMultiplier(backoffMultiplier)
                .useJitter(useJitter)
                .jitterFactor(jitterFactor)
                .connectionTimeoutMillis(connectionTimeoutMillis)
                .socketTimeoutMillis(socketTimeoutMillis)
                .build();
    }

    private void lazyInit(KeycloakSession session) {
        if (httpClient == null) {
            synchronized(this) {
                if (httpClient == null) {
                    // Create the default HTTP client with no retries
                    httpClient = createHttpClientWithoutRetries(session);

                    // Initialize the default retriable client
                    if (defaultRetryConfig.getMaxRetries() > 0) {
                        // Create a retriable client with the default configuration
                        retriableHttpClient = createRetriableHttpClient(session, defaultRetryConfig);
                    } else {
                        // If retries are disabled by default, use the regular client
                        retriableHttpClient = httpClient;
                    }
                }
            }
        }
    }

    /**
     * Creates a retriable HTTP client with the specified retry configuration
     */
    private CloseableHttpClient createRetriableHttpClient(KeycloakSession session, RetryConfig retryConfig) {
        // If retries are disabled, just return the default client
        if (retryConfig == null || retryConfig.getMaxRetries() <= 0) {
            return httpClient;
        }

        // Create HTTP client builder
        HttpClientBuilder builder = newHttpClientBuilder();

        // Configure basic settings
        long socketTimeout = retryConfig.getSocketTimeoutMillis();
        long establishConnectionTimeout = retryConfig.getConnectionTimeoutMillis();
        int maxPooledPerRoute = config.getInt("max-pooled-per-route", 64);
        int connectionPoolSize = config.getInt("connection-pool-size", 128);
        long connectionTTL = config.getLong("connection-ttl-millis", -1L);
        long maxConnectionIdleTime = config.getLong("max-connection-idle-time-millis", 900000L);
        boolean disableCookies = config.getBoolean("disable-cookies", true);
        boolean expectContinueEnabled = getBooleanConfigWithSysPropFallback("expect-continue-enabled", false);
        boolean reuseConnections = getBooleanConfigWithSysPropFallback("reuse-connections", true);

        // Configure builder
        builder.socketTimeout(socketTimeout, TimeUnit.MILLISECONDS)
                .establishConnectionTimeout(establishConnectionTimeout, TimeUnit.MILLISECONDS)
                .maxPooledPerRoute(maxPooledPerRoute)
                .connectionPoolSize(connectionPoolSize)
                .connectionTTL(connectionTTL, TimeUnit.MILLISECONDS)
                .maxConnectionIdleTime(maxConnectionIdleTime, TimeUnit.MILLISECONDS)
                .disableCookies(disableCookies)
                .proxyMappings(configureProxySettings())
                .expectContinueEnabled(expectContinueEnabled)
                .reuseConnections(reuseConnections);

        // Configure security settings
        configureSecuritySettings(session, builder);

        // Configure retry handler
        builder.getApacheHttpClientBuilder().setRetryHandler(
                new org.apache.http.impl.client.DefaultHttpRequestRetryHandler(
                        retryConfig.getMaxRetries(), retryConfig.isRetryOnIOException()) {
                    @Override
                    public boolean retryRequest(IOException exception, int executionCount,
                            org.apache.http.protocol.HttpContext context) {
                        boolean shouldRetry = super.retryRequest(exception, executionCount, context);
                        if (shouldRetry) {
                            try {
                                // Calculate backoff with jitter
                                long baseDelay = retryConfig.getInitialBackoffMillis() *
                                        (long)Math.pow(retryConfig.getBackoffMultiplier(), executionCount - 1);
                                long delay = baseDelay;
                                if (retryConfig.isUseJitter()) {
                                    // Add +/- 50% jitter
                                    double jitter = 1.0 - retryConfig.getJitterFactor() +
                                            (Math.random() * retryConfig.getJitterFactor() * 2.0);
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

        return builder.build();
    }

    /**
     * Creates an HTTP client without retry functionality
     */
    private CloseableHttpClient createHttpClientWithoutRetries(KeycloakSession session) {
        // Create HTTP client builder
        HttpClientBuilder builder = newHttpClientBuilder();

        // Configure basic settings
        long socketTimeout = config.getLong("socket-timeout-millis", 5000L);
        long establishConnectionTimeout = config.getLong("establish-connection-timeout-millis", -1L);
        int maxPooledPerRoute = config.getInt("max-pooled-per-route", 64);
        int connectionPoolSize = config.getInt("connection-pool-size", 128);
        long connectionTTL = config.getLong("connection-ttl-millis", -1L);
        long maxConnectionIdleTime = config.getLong("max-connection-idle-time-millis", 900000L);
        boolean disableCookies = config.getBoolean("disable-cookies", true);
        boolean expectContinueEnabled = getBooleanConfigWithSysPropFallback("expect-continue-enabled", false);
        boolean reuseConnections = getBooleanConfigWithSysPropFallback("reuse-connections", true);

        // Configure builder
        builder.socketTimeout(socketTimeout, TimeUnit.MILLISECONDS)
                .establishConnectionTimeout(establishConnectionTimeout, TimeUnit.MILLISECONDS)
                .maxPooledPerRoute(maxPooledPerRoute)
                .connectionPoolSize(connectionPoolSize)
                .connectionTTL(connectionTTL, TimeUnit.MILLISECONDS)
                .maxConnectionIdleTime(maxConnectionIdleTime, TimeUnit.MILLISECONDS)
                .disableCookies(disableCookies)
                .proxyMappings(configureProxySettings())
                .expectContinueEnabled(expectContinueEnabled)
                .reuseConnections(reuseConnections);

        // Configure security settings
        configureSecuritySettings(session, builder);

        // Build the client
        return builder.build();
    }

    /**
     * Configures security settings for an HTTP client builder.
     *
     * @param session The Keycloak session
     * @param builder The HTTP client builder to configure
     */
    private void configureSecuritySettings(KeycloakSession session, HttpClientBuilder builder) {
        // Configure TrustStore
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

        // Configure TrustManager
        boolean disableTrustManager = config.getBoolean("disable-trust-manager", false);
        if (disableTrustManager) {
            logger.warn("TrustManager is disabled");
            builder.disableTrustManager();
        }

        // Configure KeyStore
        String clientKeystore = config.get("client-keystore");
        if (clientKeystore != null) {
            clientKeystore = EnvUtil.replace(clientKeystore);
            String clientKeystorePassword = config.get("client-keystore-password");
            String clientPrivateKeyPassword = config.get("client-key-password");
            try {
                KeyStore clientCertKeystore = KeystoreUtil.loadKeyStore(clientKeystore, clientKeystorePassword);
                builder.keyStore(clientCertKeystore, clientPrivateKeyPassword);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load keystore", e);
            }
        }
    }

    /**
     * Configures proxy settings for HTTP clients.
     *
     * @return ProxyMappings configured based on settings or environment variables
     */
    private ProxyMappings configureProxySettings() {
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
        return proxyMappings;
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
                .name("http-client.default-max-retries")
                .type("int")
                .helpText("Maximum number of retry attempts for HTTP requests.")
                .defaultValue(3)
                .add()
                .property()
                .name("http-client.default-retry-on-io-exception")
                .type("boolean")
                .helpText("Whether to retry HTTP requests on IO exceptions.")
                .defaultValue(true)
                .add()
                .property()
                .name("http-client.default-initial-backoff-millis")
                .type("long")
                .helpText("Initial backoff time in milliseconds before the first retry attempt.")
                .defaultValue(1000L)
                .add()
                .property()
                .name("http-client.default-backoff-multiplier")
                .type("string")
                .helpText(
                        "Multiplier for exponential backoff between retry attempts. For example, with an initial backoff of 1000ms and a multiplier of 2.0, the retry delays would be: 1000ms, 2000ms, 4000ms, etc.")
                .defaultValue("2.0")
                .add()
                .property()
                .name("http-client.default-use-jitter")
                .type("boolean")
                .helpText(
                        "Whether to apply jitter to backoff times to prevent synchronized retry storms when multiple clients are retrying at the same time.")
                .defaultValue(true)
                .add()
                .property()
                .name("http-client.default-jitter-factor")
                .type("string")
                .helpText(
                        "Jitter factor to apply to backoff times. A value of 0.5 means the actual backoff time will be between 50% and 150% of the calculated exponential backoff time.")
                .defaultValue("0.5")
                .add()
                .property()
                .name("http-client.default-connection-timeout-millis")
                .type("int")
                .helpText("Connection timeout in milliseconds for retriable HTTP clients.")
                .defaultValue(10000)
                .add()
                .property()
                .name("http-client.default-socket-timeout-millis")
                .type("int")
                .helpText("Socket timeout in milliseconds for retriable HTTP clients.")
                .defaultValue(10000)
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
