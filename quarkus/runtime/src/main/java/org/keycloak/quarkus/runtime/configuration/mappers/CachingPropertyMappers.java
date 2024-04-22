package org.keycloak.quarkus.runtime.configuration.mappers;

import org.keycloak.config.CachingOptions;
import org.keycloak.quarkus.runtime.Environment;

import io.smallrye.config.ConfigSourceInterceptorContext;

import static java.util.Optional.of;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getOptionalKcValue;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

import java.io.File;
import java.nio.file.Paths;
import java.util.Optional;

final class CachingPropertyMappers {

    private static final String REMOTE_HOST_SET = "remote host is set";

    private CachingPropertyMappers() {
    }

    public static PropertyMapper<?>[] getClusteringPropertyMappers() {
        return new PropertyMapper[] {
                fromOption(CachingOptions.CACHE)
                        .paramLabel("type")
                        .build(),
                fromOption(CachingOptions.CACHE_STACK)
                        .to("kc.spi-connections-infinispan-quarkus-stack")
                        .paramLabel("stack")
                        .build(),
                fromOption(CachingOptions.CACHE_CONFIG_FILE)
                        .mapFrom("cache")
                        .to("kc.spi-connections-infinispan-quarkus-config-file")
                        .transformer(CachingPropertyMappers::resolveConfigFile)
                        .paramLabel("file")
                        .build(),
                fromOption(CachingOptions.CACHE_EMBEDDED_MTLS_ENABLED)
                        .build(),
                fromOption(CachingOptions.CACHE_EMBEDDED_MTLS_KEYSTORE.withRuntimeSpecificDefault(getDefaultKeystorePathValue()))
                        .paramLabel("file")
                        .build(),
                fromOption(CachingOptions.CACHE_EMBEDDED_MTLS_KEYSTORE_PASSWORD)
                        .paramLabel("password")
                        .isMasked(true)
                        .build(),
                fromOption(CachingOptions.CACHE_EMBEDDED_MTLS_TRUSTSTORE.withRuntimeSpecificDefault(getDefaultTruststorePathValue()))
                        .paramLabel("file")
                        .build(),
                fromOption(CachingOptions.CACHE_EMBEDDED_MTLS_TRUSTSTORE_PASSWORD)
                        .paramLabel("password")
                        .isMasked(true)
                        .build(),
                fromOption(CachingOptions.CACHE_REMOTE_HOST)
                        .paramLabel("hostname")
                        .build(),
                fromOption(CachingOptions.CACHE_REMOTE_PORT)
                        .isEnabled(CachingPropertyMappers::remoteHostSet, CachingPropertyMappers.REMOTE_HOST_SET)
                        .paramLabel("port")
                        .build(),
                fromOption(CachingOptions.CACHE_REMOTE_TLS_ENABLED)
                        .isEnabled(CachingPropertyMappers::remoteHostSet, CachingPropertyMappers.REMOTE_HOST_SET)
                        .build(),
                fromOption(CachingOptions.CACHE_REMOTE_USERNAME)
                        .isEnabled(CachingPropertyMappers::remoteHostSet, CachingPropertyMappers.REMOTE_HOST_SET)
                        .paramLabel("username")
                        .build(),
                fromOption(CachingOptions.CACHE_REMOTE_PASSWORD)
                        .isEnabled(CachingPropertyMappers::remoteHostSet, CachingPropertyMappers.REMOTE_HOST_SET)
                        .paramLabel("password")
                        .isMasked(true)
                        .build(),

                fromOption(CachingOptions.CACHE_METRICS_HISTOGRAMS_ENABLED)
                        .isEnabled(MetricsPropertyMappers::metricsEnabled, MetricsPropertyMappers.METRICS_ENABLED_MSG)
                        .build(),

        };
    }

    private static boolean remoteHostSet() {
        return getOptionalKcValue(CachingOptions.CACHE_REMOTE_HOST_PROPERTY).isPresent();
    }

    private static Optional<String> resolveConfigFile(Optional<String> value, ConfigSourceInterceptorContext context) {
        if ("local".equals(value.get())) {
            return of("cache-local.xml");
        } else if ("ispn".equals(value.get())) {
            return of("cache-ispn.xml");
        }

        String pathPrefix;
        String homeDir = Environment.getHomeDir();

        if (homeDir == null) {
            pathPrefix = "";
        } else {
            pathPrefix = homeDir + File.separator + "conf" + File.separator;
        }

        return of(pathPrefix + value.get());
    }

    private static String getDefaultKeystorePathValue() {
        String homeDir = Environment.getHomeDir();

        if (homeDir != null) {
            File file = Paths.get(homeDir, "conf", "cache-mtls-keystore.p12").toFile();

            if (file.exists()) {
                return file.getAbsolutePath();
            }
        }

        return null;
    }

    private static String getDefaultTruststorePathValue() {
        String homeDir = Environment.getHomeDir();

        if (homeDir != null) {
            File file = Paths.get(homeDir, "conf", "cache-mtls-truststore.p12").toFile();

            if (file.exists()) {
                return file.getAbsolutePath();
            }
        }

        return null;
    }
}
