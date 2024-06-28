package org.keycloak.quarkus.runtime.configuration.mappers;

import io.smallrye.config.ConfigSourceInterceptorContext;

import org.keycloak.common.crypto.FipsMode;
import org.keycloak.config.HttpOptions;
import org.keycloak.config.SecurityOptions;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.Messages;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import java.io.File;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.BiFunction;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

public final class HttpPropertyMappers {
    private static final int MIN_MAX_THREADS = 50;
    private static final String QUARKUS_HTTPS_CERT_FILES = "quarkus.http.ssl.certificate.files";
    private static final String QUARKUS_HTTPS_CERT_KEY_FILES = "quarkus.http.ssl.certificate.key-files";

    private HttpPropertyMappers(){}

    public static PropertyMapper<?>[] getHttpPropertyMappers() {
        return new PropertyMapper[] {
                fromOption(HttpOptions.HTTP_ENABLED)
                        .to("quarkus.http.insecure-requests")
                        .transformer(HttpPropertyMappers::getHttpEnabledTransformer)
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .build(),
                fromOption(HttpOptions.HTTP_SERVER_ENABLED)
                        .to("quarkus.http.host-enabled")
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
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
                fromOption(HttpOptions.HTTPS_CERTIFICATE_FILE)
                        .to(QUARKUS_HTTPS_CERT_FILES)
                        .transformer(HttpPropertyMappers.validatePath(QUARKUS_HTTPS_CERT_FILES))
                        .paramLabel("file")
                        .build(),
                fromOption(HttpOptions.HTTPS_CERTIFICATE_KEY_FILE)
                        .to(QUARKUS_HTTPS_CERT_KEY_FILES)
                        .transformer(HttpPropertyMappers.validatePath(QUARKUS_HTTPS_CERT_KEY_FILES))
                        .paramLabel("file")
                        .build(),
                fromOption(HttpOptions.HTTPS_KEY_STORE_FILE
                        .withRuntimeSpecificDefault(getDefaultKeystorePathValue()))
                        .to("quarkus.http.ssl.certificate.key-store-file")
                        .paramLabel("file")
                        .build(),
                fromOption(HttpOptions.HTTPS_KEY_STORE_PASSWORD)
                        .to("quarkus.http.ssl.certificate.key-store-password")
                        .paramLabel("password")
                        .isMasked(true)
                        .build(),
                fromOption(HttpOptions.HTTPS_KEY_STORE_TYPE)
                        .to("quarkus.http.ssl.certificate.key-store-file-type")
                        .mapFrom(SecurityOptions.FIPS_MODE.getKey())
                        .transformer(HttpPropertyMappers::resolveKeyStoreType)
                        .paramLabel("type")
                        .build(),
                fromOption(HttpOptions.HTTPS_TRUST_STORE_FILE)
                        .to("quarkus.http.ssl.certificate.trust-store-file")
                        .paramLabel("file")
                        .build(),
                fromOption(HttpOptions.HTTPS_TRUST_STORE_PASSWORD)
                        .to("quarkus.http.ssl.certificate.trust-store-password")
                        .paramLabel("password")
                        .isMasked(true)
                        .build(),
                fromOption(HttpOptions.HTTPS_TRUST_STORE_TYPE)
                        .to("quarkus.http.ssl.certificate.trust-store-file-type")
                        .mapFrom(SecurityOptions.FIPS_MODE.getKey())
                        .transformer(HttpPropertyMappers::resolveKeyStoreType)
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
                        .build()
        };
    }

    public static void validateConfig() {
        boolean enabled = isHttpEnabled(Configuration.getOptionalKcValue(HttpOptions.HTTP_ENABLED.getKey()));

        if (!enabled) {
            Optional<String> value = Configuration.getOptionalKcValue(HttpOptions.HTTPS_CERTIFICATE_FILE.getKey());

            if (value.isEmpty()) {
                value = Configuration.getOptionalValue("quarkus.http.ssl.certificate.key-store-file");
            }

            if (value.isEmpty()) {
                throw new PropertyException(Messages.httpsConfigurationNotSet());
            }
        }
    }

    private static BiFunction<Optional<String>, ConfigSourceInterceptorContext, Optional<String>> validatePath(String key) {
        return (value, context) -> Environment.isWindows() ? value.filter(v -> v.equals(context.proceed(key).getValue())).map(p -> p.replace("\\", "/")) : value;
    }

    private static Optional<String> getHttpEnabledTransformer(Optional<String> value, ConfigSourceInterceptorContext context) {
        return of(isHttpEnabled(value) ? "enabled" : "disabled");
    }

    private static boolean isHttpEnabled(Optional<String> value) {
        boolean enabled = Boolean.parseBoolean(value.get());
        Optional<String> proxy = Configuration.getOptionalKcValue("proxy");

        if (Environment.isDevMode() || Environment.isImportExportMode()
                || ("edge".equalsIgnoreCase(proxy.orElse("")))) {
            enabled = true;
        }
        return enabled;
    }

    private static File getDefaultKeystorePathValue() {
        String homeDir = Environment.getHomeDir();

        if (homeDir != null) {
            File file = Paths.get(homeDir, "conf", "server.keystore").toFile();

            if (file.exists()) {
                return file;
            }
        }

        return null;
    }

    private static Optional<String> resolveKeyStoreType(Optional<String> value,
            ConfigSourceInterceptorContext configSourceInterceptorContext) {
        if (value.isPresent()) {
            try {
                if (FipsMode.valueOfOption(value.get()).equals(FipsMode.STRICT)) {
                    return of("BCFKS");
                }
                return empty();
            } catch (IllegalArgumentException ignore) {
            }
        }
        return value;
    }

    private static Optional<String> resolveMaxThreads(Optional<String> value,
            ConfigSourceInterceptorContext configSourceInterceptorContext) {
        if (value.isEmpty()) {
            return of(String.valueOf(Math.max(MIN_MAX_THREADS, 4 * Runtime.getRuntime().availableProcessors())));
        }
        return value;
    }
}

