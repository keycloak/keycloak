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

import io.netty.handler.ssl.OpenSsl;
import io.quarkus.arc.Arc;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.OpenSSLEngineOptions;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.jboss.logging.Logger;

import static org.keycloak.utils.StringUtil.isBlank;

public class VertxHttpClientFactory implements HttpClientFactory, EnvironmentDependentProviderFactory {

    private static final Logger logger = Logger.getLogger(VertxHttpClientFactory.class);

    public static final String PROVIDER_ID = "vertx";

    private volatile WebClient webClient;
    private Config.Scope config;
    private long maxConsumedResponseSize;
    private int maxRetries;
    private long initialBackoffMillis;
    private double backoffMultiplier;
    private boolean useJitter;
    private double jitterFactor;

    @Override
    public HttpClientProvider create(KeycloakSession session) {
        lazyInit(session);
        return new VertxHttpClientProvider(webClient, maxConsumedResponseSize, maxRetries,
                initialBackoffMillis, backoffMultiplier, useJitter, jitterFactor);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void init(Config.Scope config) {
        // Quarkus routes config differently; share the "default" scope like OTelHttpClientFactory
        this.config = Config.scope("connectionsHttpClient", "default");
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        maxConsumedResponseSize = config.getLong("max-consumed-response-size",
                HttpClientProvider.DEFAULT_MAX_CONSUMED_RESPONSE_SIZE);

        maxRetries = config.getInt("max-retries", 0);
        initialBackoffMillis = config.getLong("initial-backoff-millis", 1000L);
        backoffMultiplier = Double.parseDouble(config.get("backoff-multiplier", "2.0"));
        useJitter = config.getBoolean("use-jitter", true);
        jitterFactor = Double.parseDouble(config.get("jitter-factor", "0.5"));

        checkOpenSslPresence();
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
                .helpText("Maximum time to establish connection.")
                .defaultValue(-1L)
                .add()
                .property()
                .name("max-pooled-per-route")
                .type("int")
                .helpText("Maximum connections per host.")
                .defaultValue(64)
                .add()
                .property()
                .name("max-connection-idle-time-millis")
                .type("long")
                .helpText("Maximum idle time for pooled connections.")
                .defaultValue(900000L)
                .add()
                .property()
                .name("disable-trust-manager")
                .type("boolean")
                .helpText("Disable trust verification (INSECURE).")
                .defaultValue(false)
                .add()
                .property()
                .name("openssl-required")
                .type("string")
                .helpText("OpenSSL presence policy when HTTP_CLIENT_V2 is enabled: 'warn' (default), 'fail', or 'none'.")
                .defaultValue("warn")
                .add()
                .property()
                .name("max-retries")
                .type("int")
                .helpText("Maximum number of retry attempts for outgoing HTTP requests. Set to 0 to disable retries (default).")
                .defaultValue(0)
                .add()
                .build();
    }

    private void lazyInit(KeycloakSession session) {
        if (webClient == null) {
            synchronized (this) {
                if (webClient == null) {
                    Vertx vertx = Arc.container().instance(Vertx.class).get();
                    WebClientOptions options = buildOptions(session);
                    webClient = WebClient.create(vertx, options);
                    logger.info("Vert.x HTTP client initialized (HTTP_CLIENT_V2)");
                }
            }
        }
    }

    private void checkOpenSslPresence() {
        String policy = config.get("openssl-required", "warn");
        if (!"fail".equals(policy) && !"warn".equals(policy) && !"none".equals(policy)) {
            throw new RuntimeException("Invalid openssl-required value: '" + policy
                    + "'. Valid values: 'fail', 'warn', 'none'.");
        }
        if (!OpenSsl.isAvailable()) {
            if ("fail".equals(policy)) {
                throw new RuntimeException(
                        "HTTP_CLIENT_V2 requires OpenSSL but it is not available. "
                        + "Install OpenSSL and netty-tcnative, or set openssl-required=warn.");
            } else if ("warn".equals(policy)) {
                logger.warn("OpenSSL is not available — PQC enforcement is disabled. TLS will use JSSE (Java SSL).");
            }
        } else {
            logger.infof("OpenSSL detected: %s", OpenSsl.versionString());
        }
    }

    private WebClientOptions buildOptions(KeycloakSession session) {
        WebClientOptions options = new WebClientOptions();
        if (OpenSsl.isAvailable()) {
            options.setSslEngineOptions(new OpenSSLEngineOptions());
        }

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
        configureProxy(options);

        return options;
    }

    private void configureTls(KeycloakSession session, WebClientOptions options) {
        boolean disableTrustManager = config.getBoolean("disable-trust-manager", false);
        if (disableTrustManager) {
            logger.warn("TrustManager is disabled — all certificates will be trusted");
            options.setTrustAll(true);
            options.setVerifyHost(false);
            return;
        }

        TruststoreProvider truststoreProvider = session.getProvider(TruststoreProvider.class);
        if (truststoreProvider == null || truststoreProvider.getTruststore() == null) {
            logger.warn("TruststoreProvider is disabled");
        } else {
            HostnameVerificationPolicy policy = truststoreProvider.getPolicy();
            options.setVerifyHost(policy != HostnameVerificationPolicy.ANY);
            options.setTrustOptions(keystoreToJksOptions(truststoreProvider.getTruststore(), null));
            options.setSsl(true);
        }

        String clientKeystore = config.get("client-keystore");
        if (clientKeystore != null) {
            clientKeystore = EnvUtil.replace(clientKeystore);
            String clientKeystorePassword = config.get("client-keystore-password");
            try {
                KeyStore ks = KeystoreUtil.loadKeyStore(clientKeystore, clientKeystorePassword);
                options.setKeyCertOptions(keystoreToJksOptions(ks, config.get("client-key-password", clientKeystorePassword)));
                options.setSsl(true);
                logger.debug("Client keystore configured for mutual TLS");
            } catch (Exception e) {
                throw new RuntimeException("Failed to load client keystore: " + clientKeystore, e);
            }
        }
    }

    private JksOptions keystoreToJksOptions(KeyStore keyStore, String password) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            char[] pw = password != null ? password.toCharArray() : new char[0];
            keyStore.store(baos, pw);
            return new JksOptions()
                    .setValue(Buffer.buffer(baos.toByteArray()))
                    .setPassword(password != null ? password : "");
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize keystore", e);
        }
    }

    private void configureProxy(WebClientOptions options) {
        String noProxy = null;
        ProxyMappings proxyMappings = ProxyMappings.valueOf(config.getArray("proxy-mappings"));
        if (proxyMappings == null || proxyMappings.isEmpty()) {
            logger.debug("Trying to use proxy mapping from env vars");
            String httpProxy = getEnvVarValue("https_proxy");
            if (isBlank(httpProxy)) {
                httpProxy = getEnvVarValue("http_proxy");
            }
            noProxy = getEnvVarValue("no_proxy");

            if (!isBlank(httpProxy)) {
                proxyMappings = ProxyMappings.withFixedProxyMapping(httpProxy, noProxy);
            }
        }

        if (proxyMappings == null || proxyMappings.isEmpty()) {
            return;
        }

        // Vert.x WebClient only supports a single global proxy — use the catch-all entry
        ProxyMappings.ProxyMapping wildcard = proxyMappings.getProxyFor("this-host-should-match-wildcard-only.test");
        if (wildcard == null || wildcard.getProxyHost() == null) {
            logger.warn("proxy-mappings configured but no wildcard (.*) entry found. "
                    + "Vert.x HTTP client only supports a single global proxy; per-host routing is not available.");
            return;
        }

        ProxyOptions proxyOptions = new ProxyOptions()
                .setType(ProxyType.HTTP)
                .setHost(wildcard.getProxyHost().getHostName())
                .setPort(wildcard.getProxyHost().getPort());
        if (wildcard.getProxyCredentials() != null) {
            proxyOptions.setUsername(wildcard.getProxyCredentials().getUserName())
                    .setPassword(wildcard.getProxyCredentials().getPassword());
        }
        options.setProxyOptions(proxyOptions);

        // Apply no_proxy exclusions via Vert.x addNonProxyHost (glob: * → .*)
        if (!isBlank(noProxy)) {
            for (String host : noProxy.split(",")) {
                host = host.trim();
                if (!host.isEmpty()) {
                    // Vert.x uses glob matching; prefix with *. for suffix matching like no_proxy expects
                    options.addNonProxyHost(host);
                    options.addNonProxyHost("*." + host);
                }
            }
        }

        logger.infof("Proxy configured: %s:%d", wildcard.getProxyHost().getHostName(),
                wildcard.getProxyHost().getPort());
    }

    private String getEnvVarValue(String name) {
        String value = System.getenv(name.toLowerCase());
        if (isBlank(value)) {
            value = System.getenv(name.toUpperCase());
        }
        return value;
    }
}
