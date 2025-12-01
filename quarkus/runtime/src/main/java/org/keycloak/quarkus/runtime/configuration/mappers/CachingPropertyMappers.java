package org.keycloak.quarkus.runtime.configuration.mappers;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import org.keycloak.common.Profile;
import org.keycloak.config.CachingOptions;
import org.keycloak.config.Option;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.utils.StringUtil;

import com.google.common.base.CaseFormat;
import io.smallrye.config.ConfigSourceInterceptorContext;

import static org.keycloak.quarkus.runtime.configuration.Configuration.getOptionalKcValue;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

final class CachingPropertyMappers implements PropertyMapperGrouping {

    private static final String REMOTE_HOST_SET = "remote host is set";
    private static final String MULTI_SITE_OR_EMBEDDED_REMOTE_FEATURE_SET = "feature '%s' or '%s' is set".formatted(Profile.Feature.MULTI_SITE.getKey(), Profile.Feature.CLUSTERLESS.getKey());
    private static final String MULTI_SITE_FEATURE_SET = "feature '%s' or '%s' is set".formatted(Profile.Feature.MULTI_SITE.getKey(), Profile.Feature.CLUSTERLESS.getKey());

    private static final String CACHE_STACK_SET_TO_ISPN = "'cache' type is set to '" + CachingOptions.Mechanism.ispn.name() + "'";

    @Override
    public List<PropertyMapper<?>> getPropertyMappers() {
        List<PropertyMapper<?>> staticMappers = List.of(
                fromOption(CachingOptions.CACHE)
                        .paramLabel("type")
                        .build(),
                fromOption(CachingOptions.CACHE_STACK)
                        .isEnabled(CachingPropertyMappers::cacheSetToInfinispan, CACHE_STACK_SET_TO_ISPN)
                        .to("kc.spi-cache--embedded--default-stack")
                        .paramLabel("stack")
                        .build(),
                fromOption(CachingOptions.CACHE_CONFIG_FILE)
                        .mapFrom(CachingOptions.CACHE, (value, context) -> {
                            if (CachingOptions.Mechanism.local.name().equals(value)) {
                                return "cache-local.xml";
                            } else if (CachingOptions.Mechanism.ispn.name().equals(value)) {
                                return resolveConfigFile("cache-ispn.xml", null);
                            } else {
                                return null;
                            }
                        })
                        .to("kc.spi-cache-embedded--default--config-file")
                        .transformer(CachingPropertyMappers::resolveConfigFile)
                        .validator(s -> {
                            if (!Files.exists(Paths.get(resolveConfigFile(s, null)))) {
                                throw new PropertyException("Cache config file '%s' does not exist in the conf directory".formatted(s));
                            }
                        })
                        .paramLabel("file")
                        .build(),
                fromOption(CachingOptions.CACHE_CONFIG_MUTATE)
                        .to("kc.spi-cache-embedded--default--config-mutate")
                        .build(),
                fromOption(CachingOptions.CACHE_EMBEDDED_MTLS_ENABLED)
                        .to("kc.spi-jgroups-mtls--default--enabled")
                        .isEnabled(CachingPropertyMappers::getDefaultMtlsEnabled, "a TCP based cache-stack is used")
                        .build(),
                fromOption(CachingOptions.CACHE_EMBEDDED_MTLS_KEYSTORE.withRuntimeSpecificDefault(getConfPathValue("cache-mtls-keystore.p12")))
                        .paramLabel("file")
                        .to("kc.spi-jgroups-mtls--default--keystore-file")
                        .isEnabled(() -> Configuration.isTrue(CachingOptions.CACHE_EMBEDDED_MTLS_ENABLED), "property '%s' is enabled".formatted(CachingOptions.CACHE_EMBEDDED_MTLS_ENABLED.getKey()))
                        .validator(value -> checkValidKeystore(value, CachingOptions.CACHE_EMBEDDED_MTLS_KEYSTORE, CachingOptions.CACHE_EMBEDDED_MTLS_KEYSTORE_PASSWORD))
                        .build(),
                fromOption(CachingOptions.CACHE_EMBEDDED_MTLS_KEYSTORE_PASSWORD)
                        .paramLabel("password")
                        .isMasked(true)
                        .to("kc.spi-jgroups-mtls--default--keystore-password")
                        .isEnabled(() -> Configuration.isTrue(CachingOptions.CACHE_EMBEDDED_MTLS_ENABLED), "property '%s' is enabled".formatted(CachingOptions.CACHE_EMBEDDED_MTLS_ENABLED.getKey()))
                        .validator(value -> checkOptionPresent(CachingOptions.CACHE_EMBEDDED_MTLS_KEYSTORE_PASSWORD, CachingOptions.CACHE_EMBEDDED_MTLS_KEYSTORE))
                        .build(),
                fromOption(CachingOptions.CACHE_EMBEDDED_MTLS_TRUSTSTORE.withRuntimeSpecificDefault(getConfPathValue("cache-mtls-truststore.p12")))
                        .paramLabel("file")
                        .to("kc.spi-jgroups-mtls--default--truststore-file")
                        .isEnabled(() -> Configuration.isTrue(CachingOptions.CACHE_EMBEDDED_MTLS_ENABLED), "property '%s' is enabled".formatted(CachingOptions.CACHE_EMBEDDED_MTLS_ENABLED.getKey()))
                        .validator(value -> checkValidKeystore(value, CachingOptions.CACHE_EMBEDDED_MTLS_TRUSTSTORE, CachingOptions.CACHE_EMBEDDED_MTLS_TRUSTSTORE_PASSWORD))
                        .build(),
                fromOption(CachingOptions.CACHE_EMBEDDED_MTLS_TRUSTSTORE_PASSWORD)
                        .paramLabel("password")
                        .isMasked(true)
                        .to("kc.spi-jgroups-mtls--default--truststore-password")
                        .isEnabled(() -> Configuration.isTrue(CachingOptions.CACHE_EMBEDDED_MTLS_ENABLED), "property '%s' is enabled".formatted(CachingOptions.CACHE_EMBEDDED_MTLS_ENABLED.getKey()))
                        .validator(value -> checkOptionPresent(CachingOptions.CACHE_EMBEDDED_MTLS_TRUSTSTORE_PASSWORD, CachingOptions.CACHE_EMBEDDED_MTLS_TRUSTSTORE))
                        .build(),
                fromOption(CachingOptions.CACHE_EMBEDDED_MTLS_ROTATION)
                        .paramLabel("days")
                        .to("kc.spi-jgroups-mtls--default--rotation")
                        .isEnabled(() -> Configuration.isTrue(CachingOptions.CACHE_EMBEDDED_MTLS_ENABLED), "property '%s' is enabled".formatted(CachingOptions.CACHE_EMBEDDED_MTLS_ENABLED.getKey()))
                        .validator(CachingPropertyMappers::validateCertificateRotationIsPositive)
                        .build(),
                fromOption(CachingOptions.CACHE_EMBEDDED_NETWORK_BIND_ADDRESS)
                        .paramLabel("address")
                        .to("kc.spi-cache-embedded--default--network-bind-address")
                        .isEnabled(CachingPropertyMappers::cacheSetToInfinispan, "Infinispan clustered embedded is enabled")
                        .build(),
                fromOption(CachingOptions.CACHE_EMBEDDED_NETWORK_BIND_PORT)
                       .paramLabel("port")
                       .to("kc.spi-cache-embedded--default--network-bind-port")
                       .isEnabled(CachingPropertyMappers::cacheSetToInfinispan, "Infinispan clustered embedded is enabled")
                       .build(),
                fromOption(CachingOptions.CACHE_EMBEDDED_NETWORK_EXTERNAL_ADDRESS)
                       .paramLabel("address")
                       .to("kc.spi-cache-embedded--default--network-external-address")
                       .isEnabled(CachingPropertyMappers::cacheSetToInfinispan, "Infinispan clustered embedded is enabled")
                       .build(),
                fromOption(CachingOptions.CACHE_EMBEDDED_NETWORK_EXTERNAL_PORT)
                       .paramLabel("port")
                       .isEnabled(CachingPropertyMappers::cacheSetToInfinispan, "Infinispan clustered embedded is enabled")
                       .to("kc.spi-cache-embedded--default--network-external-port")
                       .build(),
                fromOption(CachingOptions.CACHE_REMOTE_HOST)
                        .paramLabel("hostname")
                        .to("kc.spi-cache-remote--default--hostname")
                        .addValidateEnabled(CachingPropertyMappers::isRemoteCacheHostEnabled, MULTI_SITE_OR_EMBEDDED_REMOTE_FEATURE_SET)
                        .isRequired(InfinispanUtils::isRemoteInfinispan, MULTI_SITE_FEATURE_SET)
                        .build(),
                fromOption(CachingOptions.CACHE_REMOTE_PORT)
                        .isEnabled(CachingPropertyMappers::remoteHostSet, CachingPropertyMappers.REMOTE_HOST_SET)
                        .to("kc.spi-cache-remote--default--port")
                        .paramLabel("port")
                        .build(),
                fromOption(CachingOptions.CACHE_REMOTE_TLS_ENABLED)
                        .isEnabled(CachingPropertyMappers::remoteHostSet, CachingPropertyMappers.REMOTE_HOST_SET)
                        .to("kc.spi-cache-remote--default--tls-enabled")
                        .build(),
                fromOption(CachingOptions.CACHE_REMOTE_USERNAME)
                        .isEnabled(CachingPropertyMappers::remoteHostSet, CachingPropertyMappers.REMOTE_HOST_SET)
                        .to("kc.spi-cache-remote--default--username")
                        .validator((value) -> validateCachingOptionIsPresent(CachingOptions.CACHE_REMOTE_USERNAME, CachingOptions.CACHE_REMOTE_PASSWORD))
                        .paramLabel("username")
                        .build(),
                fromOption(CachingOptions.CACHE_REMOTE_PASSWORD)
                        .isEnabled(CachingPropertyMappers::remoteHostSet, CachingPropertyMappers.REMOTE_HOST_SET)
                        .to("kc.spi-cache-remote--default--password")
                        .validator((value) -> validateCachingOptionIsPresent(CachingOptions.CACHE_REMOTE_PASSWORD, CachingOptions.CACHE_REMOTE_USERNAME))
                        .paramLabel("password")
                        .isMasked(true)
                        .build(),
                fromOption(CachingOptions.CACHE_METRICS_HISTOGRAMS_ENABLED)
                        .isEnabled(MetricsPropertyMappers::metricsEnabled, MetricsPropertyMappers.METRICS_ENABLED_MSG)
                        .to("kc.spi-cache-embedded--default--metrics-histograms-enabled")
                        .build(),
                fromOption(CachingOptions.CACHE_REMOTE_BACKUP_SITES)
                        .isEnabled(CachingPropertyMappers::remoteHostSet, CachingPropertyMappers.REMOTE_HOST_SET)
                        .to("kc.spi-cache-remote--default--backup-sites")
                        .paramLabel("sites")
                        .build()
        );

        int numMappers = staticMappers.size() + CachingOptions.LOCAL_MAX_COUNT_CACHES.length + CachingOptions.CLUSTERED_MAX_COUNT_CACHES.length;
        List<PropertyMapper<?>> mappers = new ArrayList<>(numMappers);
        mappers.addAll(staticMappers);

        for (String cache : CachingOptions.LOCAL_MAX_COUNT_CACHES) {
            mappers.add(maxCountOpt(cache, () -> true, ""));
        }

        for (String cache : CachingOptions.CLUSTERED_MAX_COUNT_CACHES) {
            mappers.add(maxCountOpt(cache, InfinispanUtils::isEmbeddedInfinispan, "embedded Infinispan clusters configured"));
        }

        return mappers;
    }

    private static boolean getDefaultMtlsEnabled() {
        if (!cacheSetToInfinispan()) {
            return false;
        }
        Optional<String> cacheStackOptional = getOptionalKcValue(CachingOptions.CACHE_STACK);
        if (cacheStackOptional.isEmpty()) {
            return true;
        }
        String cacheStack = cacheStackOptional.get();
        return !(cacheStack.equals("udp") || cacheStack.equals("jdbc-ping-udp"));
    }

    private static boolean remoteHostSet() {
        return getOptionalKcValue(CachingOptions.CACHE_REMOTE_HOST_PROPERTY).isPresent();
    }

    public static boolean cacheSetToInfinispan() {
        if (InfinispanUtils.isRemoteInfinispan()) {
            return false;
        }

        Optional<String> cache = getOptionalKcValue(CachingOptions.CACHE);
        if (cache.isEmpty() && !Environment.isDevMode()) {
            return true;
        }
        return cache.isPresent() && cache.get().equals(CachingOptions.Mechanism.ispn.name());
    }

    private static String resolveConfigFile(String value, ConfigSourceInterceptorContext context) {
        return Environment.getHomeDir().map(f -> Paths.get(f, "conf", value).toString()).orElse(null);
    }

    private static String getConfPathValue(String file) {
        return Environment.getHomeDir().map(f -> Paths.get(f, "conf", file).toFile()).filter(File::exists)
                .map(File::getAbsolutePath).orElse(null);
    }

    private static PropertyMapper<?> maxCountOpt(String cacheName, BooleanSupplier isEnabled, String enabledWhen) {
        return fromOption(CachingOptions.maxCountOption(cacheName))
                .isEnabled(isEnabled, enabledWhen)
                .paramLabel("max-count")
                .to("kc.spi-cache-embedded--default--%s-max-count".formatted(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, cacheName)))
                .build();
    }

    private static boolean isRemoteCacheHostEnabled() {
        return InfinispanUtils.isRemoteInfinispan();
    }

    private static void validateCachingOptionIsPresent(Option<?> optionSet, Option<?> optionRequired) {
        if (getOptionalKcValue(optionRequired).isEmpty()) {
            throw new PropertyException("The option '%s' is required when '%s' is set.".formatted(optionRequired.getKey(), optionSet.getKey()));
        }
    }

    private static void checkValidKeystore(String store, Option<String> option, Option<String> requiredOption) {
        checkOptionPresent(option, requiredOption);
        if (!new File(store).exists()) {
            throw new IllegalArgumentException("The '%s' file '%s' does not exist.".formatted(option.getKey(), store));
        }
    }

    private static void checkOptionPresent(Option<String> option, Option<String> requiredOption) {
        if (getOptionalKcValue(requiredOption).isPresent()) {
            return;
        }
        throw new PropertyException("The option '%s' requires '%s' to be enabled.".formatted(option.getKey(), requiredOption.getKey()));
    }

    private static void validateCertificateRotationIsPositive(String value) {
        value = value.trim();
        if (StringUtil.isBlank(value)) {
            throw new PropertyException("Option '%s' must not be empty.".formatted(CachingOptions.CACHE_EMBEDDED_MTLS_ROTATION.getKey()));
        }
        try {
            if (Integer.parseInt(value) <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException unused) {
            throw new PropertyException("JGroups MTLS certificate rotation in '%s' option must positive.".formatted(CachingOptions.CACHE_EMBEDDED_MTLS_ROTATION.getKey()));
        }
    }
}
