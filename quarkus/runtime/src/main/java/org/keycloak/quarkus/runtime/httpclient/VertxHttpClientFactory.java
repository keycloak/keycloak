package org.keycloak.quarkus.runtime.httpclient;

import java.io.ByteArrayOutputStream;
import java.security.KeyStore;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.common.enums.HostnameVerificationPolicy;
import org.keycloak.common.util.EnvUtil;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.connections.httpclient.HttpClientFactory;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.connections.httpclient.ProxyMappings;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.truststore.TruststoreProvider;

import io.quarkus.arc.Arc;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.net.KeyStoreOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.jboss.logging.Logger;

import static org.keycloak.utils.StringUtil.isBlank;

public class VertxHttpClientFactory implements HttpClientFactory, EnvironmentDependentProviderFactory {

    private static final Logger logger = Logger.getLogger(VertxHttpClientFactory.class);

    public static final String PROVIDER_ID = "vertx";

    private volatile WebClient webClient;
    private volatile HttpClient httpClient;
    private Config.Scope config;
    private long maxConsumedResponseSize;
    private long socketTimeoutMs;
    private int maxRetries;
    private long initialBackoffMillis;
    private double backoffMultiplier;
    private long requestTimeoutMs;
    private boolean useJitter;
    private double jitterFactor;
    private ProxyMappings proxyMappings;

    @Override
    public HttpClientProvider create(KeycloakSession session) {
        lazyInit(session);
        return new VertxHttpClientProvider(webClient, httpClient, maxConsumedResponseSize, socketTimeoutMs,
                requestTimeoutMs, maxRetries, initialBackoffMillis, backoffMultiplier, useJitter, jitterFactor, proxyMappings);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void init(Config.Scope config) {
        // Shared properties (timeouts, pool, etc.) read from "default" scope so users
        // don't reconfigure when switching v1→v2. Same pattern as OTelHttpClientFactory.
        this.config = Config.scope("connectionsHttpClient", "default");
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        maxConsumedResponseSize = config.getLong("max-consumed-response-size",
                HttpClientProvider.DEFAULT_MAX_CONSUMED_RESPONSE_SIZE);
        socketTimeoutMs = config.getLong("socket-timeout-millis", 5000L);
        requestTimeoutMs = config.getLong("request-timeout-millis", 30000L);

        maxRetries = config.getInt("max-retries", 0);
        initialBackoffMillis = config.getLong("initial-backoff-millis", 1000L);
        backoffMultiplier = Double.parseDouble(config.get("backoff-multiplier", "2.0"));
        useJitter = config.getBoolean("use-jitter", true);
        jitterFactor = Double.parseDouble(config.get("jitter-factor", "0.5"));

    }

    @Override
    public void close() {
        try {
            if (webClient != null) {
                webClient.close();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.HTTP_CLIENT_V2);
    }

    @Override
    public int order() {
        return 100;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create().build();
    }

    private void lazyInit(KeycloakSession session) {
        if (webClient == null) {
            synchronized (this) {
                if (webClient == null) {
                    Vertx vertx = Arc.requireContainer().instance(Vertx.class).get();
                    WebClientOptions options = buildOptions(session);
                    httpClient = vertx.createHttpClient(options);
                    webClient = WebClient.wrap(httpClient, options);
                    logger.info("Vert.x HTTP client initialized (HTTP_CLIENT_V2)");
                }
            }
        }
    }

    private WebClientOptions buildOptions(KeycloakSession session) {
        WebClientOptions options = new WebClientOptions();

        options.setMaxPoolSize(config.getInt("max-pooled-per-route", 64));

        long connectTimeout = config.getLong("establish-connection-timeout-millis", -1L);
        if (connectTimeout > 0) {
            options.setConnectTimeout((int) connectTimeout);
        }
        long socketTimeout = config.getLong("socket-timeout-millis", 5000L);
        if (socketTimeout > 0) {
            options.setIdleTimeout((int) socketTimeout);
            options.setIdleTimeoutUnit(TimeUnit.MILLISECONDS);
        }

        long maxIdleTime = config.getLong("max-connection-idle-time-millis", 900000L);
        if (maxIdleTime > 0) {
            options.setKeepAliveTimeout((int) (maxIdleTime / 1000));
        }

        options.setKeepAlive(config.getBoolean("reuse-connections", true));
        options.setFollowRedirects(config.getBoolean("allow-redirects", false));
        options.setDecompressionSupported(true);

        configureTls(session, options);
        configureProxy();

        return options;
    }

    private void configureTls(KeycloakSession session, WebClientOptions options) {
        boolean disableTrustManager = config.getBoolean("disable-trust-manager", false);
        if (disableTrustManager) {
            logger.warn("TrustManager is disabled — all certificates will be trusted");
            options.setTrustAll(true);
            options.setVerifyHost(false);
        } else {
            TruststoreProvider truststoreProvider = session.getProvider(TruststoreProvider.class);
            if (truststoreProvider == null || truststoreProvider.getTruststore() == null) {
                logger.warn("TruststoreProvider is disabled");
            } else {
                HostnameVerificationPolicy policy = truststoreProvider.getPolicy();
                options.setVerifyHost(policy != HostnameVerificationPolicy.ANY);
                options.setTrustOptions(keystoreToOptions(truststoreProvider.getTruststore(), null));
                options.setSsl(true);
            }
        }

        String clientKeystore = config.get("client-keystore");
        if (clientKeystore != null) {
            clientKeystore = EnvUtil.replace(clientKeystore);
            String clientKeystorePassword = config.get("client-keystore-password");
            try {
                KeyStore ks = KeystoreUtil.loadKeyStore(clientKeystore, clientKeystorePassword);
                options.setKeyCertOptions(keystoreToOptions(ks, config.get("client-key-password", clientKeystorePassword)));
                options.setSsl(true);
                logger.debug("Client keystore configured for mutual TLS");
            } catch (Exception e) {
                throw new RuntimeException("Failed to load client keystore: " + clientKeystore, e);
            }
        }
    }

    private KeyStoreOptions keystoreToOptions(KeyStore keyStore, String password) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            char[] pw = password != null ? password.toCharArray() : new char[0];
            keyStore.store(baos, pw);
            return new KeyStoreOptions()
                    .setType(keyStore.getType())
                    .setValue(Buffer.buffer(baos.toByteArray()))
                    .setPassword(password != null ? password : "");
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize keystore", e);
        }
    }

    private void configureProxy() {
        ProxyMappings mappings = ProxyMappings.valueOf(config.getArray("proxy-mappings"));
        if (mappings == null || mappings.isEmpty()) {
            logger.debug("Trying to use proxy mapping from env vars");
            String httpProxy = getEnvVarValue("https_proxy");
            if (isBlank(httpProxy)) {
                httpProxy = getEnvVarValue("http_proxy");
            }
            String noProxy = getEnvVarValue("no_proxy");

            if (!isBlank(httpProxy)) {
                mappings = ProxyMappings.withFixedProxyMapping(httpProxy, noProxy);
            }
        }

        if (mappings != null && !mappings.isEmpty()) {
            this.proxyMappings = mappings;
            logger.debug("Proxy mappings configured — per-request proxy routing enabled");
        }
    }

    private String getEnvVarValue(String name) {
        String value = System.getenv(name.toLowerCase());
        if (isBlank(value)) {
            value = System.getenv(name.toUpperCase());
        }
        return value;
    }
}
