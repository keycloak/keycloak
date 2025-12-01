package org.keycloak.quarkus.runtime.configuration.mappers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.keycloak.common.Profile;
import org.keycloak.common.crypto.FipsMode;
import org.keycloak.config.HttpOptions;
import org.keycloak.config.SecurityOptions;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.Messages;
import org.keycloak.quarkus.runtime.cli.ExecutionExceptionHandler;
import org.keycloak.quarkus.runtime.cli.Picocli;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.cli.command.AbstractCommand;

import io.quarkus.runtime.util.ClassPathUtils;
import io.quarkus.vertx.http.runtime.options.TlsUtils;
import io.smallrye.config.ConfigSourceInterceptorContext;

import static org.keycloak.quarkus.runtime.configuration.Configuration.getOptionalKcValue;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getOptionalValue;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromFeature;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

public final class HttpPropertyMappers implements PropertyMapperGrouping {
    private static final int MIN_MAX_THREADS = 50;
    private static final String QUARKUS_HTTPS_CERT_FILES = "quarkus.http.ssl.certificate.files";
    private static final String QUARKUS_HTTPS_CERT_KEY_FILES = "quarkus.http.ssl.certificate.key-files";
    private static final String QUARKUS_HTTPS_KEY_STORE_FILE = "quarkus.http.ssl.certificate.key-store-file";
    private static final String QUARKUS_HTTPS_TRUST_STORE_FILE = "quarkus.http.ssl.certificate.trust-store-file";
    public static final String QUARKUS_HTTPS_TRUST_STORE_FILE_TYPE = "quarkus.http.ssl.certificate.trust-store-file-type";
    private static final String QUARKUS_HTTPS_KEY_STORE_FILE_TYPE = "quarkus.http.ssl.certificate.key-store-file-type";

    // Transform runtime exceptions obtained from Quarkus to ours with a relevant message
    private static void setCustomExceptionTransformer() {
        ExecutionExceptionHandler.addExceptionTransformer(TlsUtils.class, exception -> {
            if (exception instanceof IOException ioe) {
                return new PropertyException("Failed to load 'https-*' material: " + ioe.getClass().getSimpleName() + " " + ioe.getMessage(), ioe);
            } else if (exception instanceof IllegalArgumentException iae) {
                if (iae.getMessage().contains(QUARKUS_HTTPS_TRUST_STORE_FILE_TYPE)) {
                    return new PropertyException("Unable to determine 'https-trust-store-type' automatically. " +
                            "Adjust the file extension or specify the property.", iae);
                } else if (iae.getMessage().contains(QUARKUS_HTTPS_KEY_STORE_FILE_TYPE)) {
                    return new PropertyException("Unable to determine 'https-key-store-type' automatically. " +
                            "Adjust the file extension or specify the property.", iae);
                } else {
                    return new PropertyException(iae.getMessage(), iae);
                }
            }
            return exception;
        });
    }

    @Override
    public List<PropertyMapper<?>> getPropertyMappers() {
        setCustomExceptionTransformer();
        return List.of(
                fromOption(HttpOptions.HTTP_ENABLED)
                        .to("quarkus.http.insecure-requests")
                        .transformer(HttpPropertyMappers::getHttpEnabledTransformer)
                        .build(),
                fromOption(HttpOptions.HTTP_HOST)
                        .to("quarkus.http.host")
                        .paramLabel("host")
                        .build(),
                fromOption(HttpOptions.HTTP_RELATIVE_PATH)
                        .to("quarkus.http.root-path")
                        .paramLabel("path")
                        .build(),
                fromOption(HttpOptions.HTTP_PORT)
                        .to("quarkus.http.port")
                        .paramLabel("port")
                        .build(),
                fromOption(HttpOptions.HTTPS_PORT)
                        .to("quarkus.http.ssl-port")
                        .paramLabel("port")
                        .build(),
                fromOption(HttpOptions.HTTPS_CLIENT_AUTH)
                        .to("quarkus.http.ssl.client-auth")
                        .paramLabel("auth")
                        .build(),
                fromOption(HttpOptions.HTTPS_CIPHER_SUITES)
                        .to("quarkus.http.ssl.cipher-suites")
                        .paramLabel("ciphers")
                        .build(),
                fromOption(HttpOptions.HTTPS_PROTOCOLS)
                        .to("quarkus.http.ssl.protocols")
                        .paramLabel("protocols")
                        .build(),
                fromOption(HttpOptions.HTTPS_CERTIFICATES_RELOAD_PERIOD)
                        .to("quarkus.http.ssl.certificate.reload-period")
                        .transformer(HttpPropertyMappers::transformNegativeReloadPeriod)
                        .paramLabel("reload period")
                        .build(),
                fromOption(HttpOptions.HTTPS_CERTIFICATE_FILE)
                        .to(QUARKUS_HTTPS_CERT_FILES)
                        .transformer(HttpPropertyMappers::transformPath)
                        .paramLabel("file")
                        .build(),
                fromOption(HttpOptions.HTTPS_CERTIFICATE_KEY_FILE)
                        .to(QUARKUS_HTTPS_CERT_KEY_FILES)
                        .transformer(HttpPropertyMappers::transformPath)
                        .paramLabel("file")
                        .build(),
                fromOption(HttpOptions.HTTPS_KEY_STORE_FILE
                        .withRuntimeSpecificDefault(getDefaultKeystorePathValue()))
                        .to(QUARKUS_HTTPS_KEY_STORE_FILE)
                        .transformer(HttpPropertyMappers::transformPath)
                        .paramLabel("file")
                        .build(),
                fromOption(HttpOptions.HTTPS_KEY_STORE_PASSWORD)
                        .to("quarkus.http.ssl.certificate.key-store-password")
                        .paramLabel("password")
                        .isMasked(true)
                        .build(),
                fromOption(HttpOptions.HTTPS_KEY_STORE_TYPE)
                        .mapFrom(SecurityOptions.FIPS_MODE, HttpPropertyMappers::resolveKeyStoreType)
                        .to(QUARKUS_HTTPS_KEY_STORE_FILE_TYPE)
                        .paramLabel("type")
                        .build(),
                fromOption(HttpOptions.HTTPS_TRUST_STORE_FILE)
                        .to(QUARKUS_HTTPS_TRUST_STORE_FILE)
                        .transformer(HttpPropertyMappers::transformPath)
                        .paramLabel("file")
                        .build(),
                fromOption(HttpOptions.HTTPS_TRUST_STORE_PASSWORD)
                        .to("quarkus.http.ssl.certificate.trust-store-password")
                        .paramLabel("password")
                        .isMasked(true)
                        .build(),
                fromOption(HttpOptions.HTTPS_TRUST_STORE_TYPE)
                        .mapFrom(SecurityOptions.FIPS_MODE, HttpPropertyMappers::resolveKeyStoreType)
                        .to(QUARKUS_HTTPS_TRUST_STORE_FILE_TYPE)
                        .paramLabel("type")
                        .build(),
                fromOption(HttpOptions.HTTP_MAX_QUEUED_REQUESTS)
                        .to("quarkus.thread-pool.queue-size")
                        .paramLabel("requests")
                        .build(),
                fromOption(HttpOptions.HTTP_POOL_MAX_THREADS)
                        .to("quarkus.thread-pool.max-threads")
                        .transformer(HttpPropertyMappers::resolveMaxThreads)
                        .paramLabel("threads")
                        .build(),
                fromOption(HttpOptions.HTTP_METRICS_HISTOGRAMS_ENABLED)
                        .isEnabled(MetricsPropertyMappers::metricsEnabled, MetricsPropertyMappers.METRICS_ENABLED_MSG)
                        .build(),
                fromOption(HttpOptions.HTTP_METRICS_SLOS)
                        .isEnabled(MetricsPropertyMappers::metricsEnabled, MetricsPropertyMappers.METRICS_ENABLED_MSG)
                        .paramLabel("list of buckets")
                        .build(),
                fromFeature(Profile.Feature.HTTP_OPTIMIZED_SERIALIZERS)
                        .to("quarkus.rest.jackson.optimization.enable-reflection-free-serializers")
                        .build(),
                fromOption(HttpOptions.HTTP_ACCEPT_NON_NORMALIZED_PATHS)
                        .build()
        );
    }

    @Override
    public void validateConfig(Picocli picocli) {
        if (picocli.getParsedCommand().filter(AbstractCommand::isServing).isPresent()) {
            boolean enabled = isHttpEnabled(getOptionalKcValue(HttpOptions.HTTP_ENABLED.getKey()).orElse(null));
            if (!enabled && !isHttpsEnabled()) {
                throw new PropertyException(Messages.httpsConfigurationNotSet());
            }
        }
    }

    public static boolean isHttpsEnabled() {
        Optional<String> certFile = getOptionalValue(QUARKUS_HTTPS_CERT_FILES);
        Optional<String> keystoreFile = getOptionalValue(QUARKUS_HTTPS_KEY_STORE_FILE);
        return certFile.isPresent() || keystoreFile.isPresent();
    }

    private static String transformPath(String value, ConfigSourceInterceptorContext context) {
        return value == null ? value : ClassPathUtils.toResourceName(Path.of(value));
    }

    private static String getHttpEnabledTransformer(String value, ConfigSourceInterceptorContext context) {
        return isHttpEnabled(value) ? "enabled" : "disabled";
    }

    static String transformNegativeReloadPeriod(String value, ConfigSourceInterceptorContext context) {
        // -1 means no reload
        return "-1".equals(value) ? null : value;
    }

    private static boolean isHttpEnabled(String value) {
        if (Environment.isDevMode() || Environment.isNonServerMode()) {
            return true;
        }
        return Boolean.parseBoolean(value);
    }

    private static File getDefaultKeystorePathValue() {
        return Environment.getHomeDir().map(f -> Paths.get(f, "conf", "server.keystore").toFile()).filter(File::exists)
                .orElse(null);
    }

    private static String resolveKeyStoreType(String value,
            ConfigSourceInterceptorContext configSourceInterceptorContext) {
        if (FipsMode.STRICT.toString().equals(value)) {
            return "BCFKS";
        }
        return null;
    }

    private static String resolveMaxThreads(String value,
            ConfigSourceInterceptorContext configSourceInterceptorContext) {
        if (value == null) {
            return String.valueOf(Math.max(MIN_MAX_THREADS, 4 * Runtime.getRuntime().availableProcessors()));
        }
        return value;
    }
}
