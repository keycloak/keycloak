package org.keycloak.quarkus.runtime.configuration.mappers;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;

import org.keycloak.config.ConfigSupportLevel;
import org.keycloak.config.OptionCategory;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.configuration.ConfigArgsConfigSource;
import org.keycloak.quarkus.runtime.configuration.DisabledMappersInterceptor;
import org.keycloak.quarkus.runtime.configuration.PersistedConfigSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import static org.keycloak.quarkus.runtime.Environment.isRebuildCheck;

public final class PropertyMappers {

    public static String VALUE_MASK = "*******";
    private static final MappersConfig MAPPERS = new MappersConfig();

    private PropertyMappers(){}

    static {
        MAPPERS.addAll(CachingPropertyMappers.getClusteringPropertyMappers());
        MAPPERS.addAll(DatabasePropertyMappers.getDatabasePropertyMappers());
        MAPPERS.addAll(HostnamePropertyMappers.getHostnamePropertyMappers());
        MAPPERS.addAll(HttpPropertyMappers.getHttpPropertyMappers());
        MAPPERS.addAll(HealthPropertyMappers.getHealthPropertyMappers());
        MAPPERS.addAll(ConfigKeystorePropertyMappers.getConfigKeystorePropertyMappers());
        MAPPERS.addAll(MetricsPropertyMappers.getMetricsPropertyMappers());
        MAPPERS.addAll(ProxyPropertyMappers.getProxyPropertyMappers());
        MAPPERS.addAll(VaultPropertyMappers.getVaultPropertyMappers());
        MAPPERS.addAll(FeaturePropertyMappers.getMappers());
        MAPPERS.addAll(LoggingPropertyMappers.getMappers());
        MAPPERS.addAll(TransactionPropertyMappers.getTransactionPropertyMappers());
        MAPPERS.addAll(ClassLoaderPropertyMappers.getMappers());
        MAPPERS.addAll(SecurityPropertyMappers.getMappers());
        MAPPERS.addAll(ExportPropertyMappers.getMappers());
        MAPPERS.addAll(ImportPropertyMappers.getMappers());
        MAPPERS.addAll(TruststorePropertyMappers.getMappers());
    }

    public static ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {
        PropertyMapper<?> mapper = MAPPERS.getOrDefault(name, PropertyMapper.IDENTITY);
        return mapper.getConfigValue(name, context);
    }

    public static boolean isBuildTimeProperty(String name) {
        if (isFeaturesBuildTimeProperty(name) || isSpiBuildTimeProperty(name)) {
            return true;
        }

        PropertyMapper<?> mapper = MAPPERS.get(name);
        boolean isBuildTimeProperty = mapper == null ? false : mapper.isBuildTime();

        return isBuildTimeProperty
                && !"kc.version".equals(name)
                && !ConfigArgsConfigSource.CLI_ARGS.equals(name)
                && !"kc.home.dir".equals(name)
                && !"kc.config.file".equals(name)
                && !Environment.PROFILE.equals(name)
                && !"kc.show.config".equals(name)
                && !"kc.show.config.runtime".equals(name)
                && !"kc.config-file".equals(name);
    }

    private static boolean isSpiBuildTimeProperty(String name) {
        return name.startsWith("kc.spi") && (name.endsWith("provider") || name.endsWith("enabled"));
    }

    private static boolean isFeaturesBuildTimeProperty(String name) {
        return name.startsWith("kc.features");
    }

    public static Map<OptionCategory, List<PropertyMapper<?>>> getRuntimeMappers() {
        return MAPPERS.getRuntimeMappers();
    }

    public static Map<OptionCategory, List<PropertyMapper<?>>> getBuildTimeMappers() {
        return MAPPERS.getBuildTimeMappers();
    }

    public static Map<String, PropertyMapper<?>> getDisabledMappers() {
        final var disabledMappers = new HashMap<>(getDisabledBuildTimeMappers());
        disabledMappers.putAll(getDisabledRuntimeMappers());
        return disabledMappers;
    }

    public static Map<String, PropertyMapper<?>> getDisabledRuntimeMappers() {
        return MAPPERS.getDisabledRuntimeMappers();
    }

    public static Map<String, PropertyMapper<?>> getDisabledBuildTimeMappers() {
        return MAPPERS.getDisabledBuildTimeMappers();
    }

    /**
     * Removes all disabled mappers from the runtime/buildtime mappers
     */
    public static void sanitizeDisabledMappers() {
        MAPPERS.sanitizeDisabledMappers();
    }

    public static String formatValue(String property, String value) {
        property = removeProfilePrefixIfNeeded(property);
        PropertyMapper<?> mapper = getMapper(property);

        if (mapper != null && mapper.isMask()) {
            return VALUE_MASK;
        }

        return value;
    }

    private static String removeProfilePrefixIfNeeded(String property) {
        if(property.startsWith("%")) {
            String profilePrefix = property.substring(0, property.indexOf(".") +1);
            property = property.split(profilePrefix)[1];
        }
        return property;
    }

    public static PropertyMapper<?> getMapper(String property) {
        if (property.startsWith("%")) {
            return MAPPERS.get(property.substring(property.indexOf('.') + 1));
        }
        return MAPPERS.get(property);
    }

    public static Collection<PropertyMapper<?>> getMappers() {
        return MAPPERS.values();
    }

    public static boolean isSupported(PropertyMapper<?> mapper) {
        return mapper.getCategory().getSupportLevel().equals(ConfigSupportLevel.SUPPORTED);
    }

    public static Optional<PropertyMapper<?>> getDisabledMapper(String property) {
        if (property == null) return Optional.empty();

        PropertyMapper<?> mapper = getDisabledBuildTimeMappers().get(property);
        if (mapper == null) {
            mapper = getDisabledRuntimeMappers().get(property);
        }
        return Optional.ofNullable(mapper);
    }

    public static boolean isDisabledMapper(String property) {
        final Predicate<String> isDisabledMapper = (p) -> getDisabledMapper(p).isPresent();

        if (property.startsWith("%")) {
            return isDisabledMapper.test(property.substring(property.indexOf('.') + 1));
        }
        return isDisabledMapper.test(property);
    }

    private static class MappersConfig extends HashMap<String, PropertyMapper<?>> {

        private final Map<OptionCategory, List<PropertyMapper<?>>> buildTimeMappers = new EnumMap<>(OptionCategory.class);
        private final Map<OptionCategory, List<PropertyMapper<?>>> runtimeTimeMappers = new EnumMap<>(OptionCategory.class);

        private final Map<String, PropertyMapper<?>> disabledBuildTimeMappers = new HashMap<>();
        private final Map<String, PropertyMapper<?>> disabledRuntimeMappers = new HashMap<>();

        public void addAll(PropertyMapper<?>[] mappers, BooleanSupplier isEnabled, String enabledWhen) {
            Arrays.stream(mappers).forEach(mapper -> {
                mapper.setEnabled(isEnabled);
                mapper.setEnabledWhen(enabledWhen);
            });

            addAll(mappers);
        }

        public void addAll(PropertyMapper<?>[] mappers) {
            for (PropertyMapper<?> mapper : mappers) {
                addMapper(mapper);

                if (mapper.isBuildTime()) {
                    addMapperByStage(mapper, buildTimeMappers);
                } else {
                    addMapperByStage(mapper, runtimeTimeMappers);
                }
            }
        }

        private static void addMapperByStage(PropertyMapper<?> mapper, Map<OptionCategory, List<PropertyMapper<?>>> mappers) {
            mappers.computeIfAbsent(mapper.getCategory(), c -> new ArrayList<>()).add(mapper);
        }

        @Override
        public PropertyMapper<?> put(String key, PropertyMapper<?> value) {
            if (containsKey(key)) {
                throw new IllegalArgumentException("Duplicated mapper for key [" + key + "]");
            }
            return super.put(key, value);
        }

        public void addMapper(PropertyMapper<?> mapper) {
            addMapper(mapper, super::put);
        }

        public void removeMapper(PropertyMapper<?> mapper) {
            remove(mapper.getTo());
            remove(mapper.getFrom());
            remove(mapper.getCliFormat());
            remove(mapper.getEnvVarFormat());
        }

        public void sanitizeDisabledMappers() {
            DisabledMappersInterceptor.runWithDisabled(() -> { // We need to have the whole configuration available

                // Initialize profile in order to check state of features. Disable Persisted CS for re-augmentation
                if (isRebuildCheck()) {
                    PersistedConfigSource.getInstance().runWithDisabled(Environment::getCurrentOrCreateFeatureProfile);
                } else {
                    Environment.getCurrentOrCreateFeatureProfile();
                }

                sanitizeMappers(buildTimeMappers, disabledBuildTimeMappers);
                sanitizeMappers(runtimeTimeMappers, disabledRuntimeMappers);
            });
        }

        public Map<OptionCategory, List<PropertyMapper<?>>> getRuntimeMappers() {
            return runtimeTimeMappers;
        }

        public Map<OptionCategory, List<PropertyMapper<?>>> getBuildTimeMappers() {
            return buildTimeMappers;
        }

        public Map<String, PropertyMapper<?>> getDisabledBuildTimeMappers() {
            return disabledBuildTimeMappers;
        }

        public Map<String, PropertyMapper<?>> getDisabledRuntimeMappers() {
            return disabledRuntimeMappers;
        }

        private static void sanitizeMappers(Map<OptionCategory, List<PropertyMapper<?>>> mappers,
                                            Map<String, PropertyMapper<?>> disabledMappers) {
            mappers.forEach((category, propertyMappers) ->
                    propertyMappers.removeIf(pm -> {
                        final boolean shouldRemove = !pm.isEnabled();
                        if (shouldRemove) {
                            MAPPERS.removeMapper(pm);
                            addMapper(pm, disabledMappers::put);
                        }
                        return shouldRemove;
                    }));
        }

        private static void addMapper(PropertyMapper<?> mapper, BiConsumer<String, PropertyMapper<?>> adder) {
            adder.accept(mapper.getTo(), mapper);
            adder.accept(mapper.getFrom(), mapper);
            adder.accept(mapper.getCliFormat(), mapper);
            adder.accept(mapper.getEnvVarFormat(), mapper);
        }
    }
}
