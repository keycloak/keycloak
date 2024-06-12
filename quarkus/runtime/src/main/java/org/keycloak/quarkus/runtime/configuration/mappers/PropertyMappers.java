package org.keycloak.quarkus.runtime.configuration.mappers;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;

import jakarta.ws.rs.core.MultivaluedHashMap;
import org.jboss.logging.Logger;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.config.ConfigSupportLevel;
import org.keycloak.config.OptionCategory;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.cli.command.AbstractCommand;
import org.keycloak.quarkus.runtime.cli.command.Build;
import org.keycloak.quarkus.runtime.cli.command.ShowConfig;
import org.keycloak.quarkus.runtime.configuration.ConfigArgsConfigSource;
import org.keycloak.quarkus.runtime.configuration.DisabledMappersInterceptor;
import org.keycloak.quarkus.runtime.configuration.PersistedConfigSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.keycloak.quarkus.runtime.Environment.isParsedCommand;
import static org.keycloak.quarkus.runtime.Environment.isRebuild;
import static org.keycloak.quarkus.runtime.Environment.isRebuildCheck;
import static org.keycloak.quarkus.runtime.configuration.KeycloakConfigSourceProvider.isKeyStoreConfigSource;

public final class PropertyMappers {

    public static String VALUE_MASK = "*******";
    private static final MappersConfig MAPPERS = new MappersConfig();
    private static final Logger log = Logger.getLogger(PropertyMappers.class);

    private PropertyMappers(){}

    static {
        MAPPERS.addAll(CachingPropertyMappers.getClusteringPropertyMappers());
        MAPPERS.addAll(DatabasePropertyMappers.getDatabasePropertyMappers());
        MAPPERS.addAll(HostnameV2PropertyMappers.getHostnamePropertyMappers());
        MAPPERS.addAll(HostnameV1PropertyMappers.getHostnamePropertyMappers());
        MAPPERS.addAll(HttpPropertyMappers.getHttpPropertyMappers());
        MAPPERS.addAll(HealthPropertyMappers.getHealthPropertyMappers());
        MAPPERS.addAll(ConfigKeystorePropertyMappers.getConfigKeystorePropertyMappers());
        MAPPERS.addAll(ManagementPropertyMappers.getManagementPropertyMappers());
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
        return getMapperOrDefault(name, PropertyMapper.IDENTITY).getConfigValue(name, context);
    }

    public static boolean isBuildTimeProperty(String name) {
        if (isFeaturesBuildTimeProperty(name) || isSpiBuildTimeProperty(name)) {
            return true;
        }

        final PropertyMapper<?> mapper = getMapperOrDefault(name, null);
        boolean isBuildTimeProperty = mapper == null ? false : mapper.isBuildTime();

        return isBuildTimeProperty
                && !"kc.version".equals(name)
                && !ConfigArgsConfigSource.CLI_ARGS.equals(name)
                && !"kc.home.dir".equals(name)
                && !"kc.config.file".equals(name)
                && !org.keycloak.common.util.Environment.PROFILE.equals(name)
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

    public static String maskValue(String property, String value) {
        return maskValue(property, value, null);
    }

    public static String maskValue(String property, String value, String configSourceName) {
        property = removeProfilePrefixIfNeeded(property);
        PropertyMapper<?> mapper = getMapper(property);

        if ((configSourceName != null && isKeyStoreConfigSource(configSourceName) || (mapper != null && mapper.isMask()))) {
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

    private static PropertyMapper<?> getMapperOrDefault(String property, PropertyMapper<?> defaultMapper, OptionCategory category) {
        property = removeProfilePrefixIfNeeded(property);
        final var mappers = new ArrayList<>(MAPPERS.getOrDefault(property, Collections.emptyList()));
        if (category != null) {
            mappers.removeIf(m -> !m.getCategory().equals(category));
        }

        return switch (mappers.size()) {
            case 0 -> defaultMapper;
            case 1 -> mappers.get(0);
            default -> {
                log.debugf("Duplicated mappers for key '%s'. Used the first found.", property);
                yield mappers.get(0);
            }
        };
    }

    private static PropertyMapper<?> getMapperOrDefault(String property, PropertyMapper<?> defaultMapper) {
        return getMapperOrDefault(property, defaultMapper, null);
    }

    public static PropertyMapper<?> getMapper(String property, OptionCategory category) {
        return getMapperOrDefault(property, null, category);
    }

    public static PropertyMapper<?> getMapper(String property) {
        return getMapper(property, null);
    }

    public static Set<PropertyMapper<?>> getMappers() {
        return MAPPERS.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
    }

    public static boolean isSupported(PropertyMapper<?> mapper) {
        ConfigSupportLevel supportLevel = mapper.getCategory().getSupportLevel();
        return supportLevel.equals(ConfigSupportLevel.SUPPORTED) || supportLevel.equals(ConfigSupportLevel.DEPRECATED);
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
        final Predicate<String> isDisabledMapper = (p) -> getDisabledMapper(p).isPresent() && getMapper(p) == null;

        if (property.startsWith("%")) {
            return isDisabledMapper.test(property.substring(property.indexOf('.') + 1));
        }
        return isDisabledMapper.test(property);
    }

    private static Set<PropertyMapper<?>> filterDeniedCategories(List<PropertyMapper<?>> mappers) {
        final var allowedCategories = Environment.getParsedCommand()
                .map(AbstractCommand::getOptionCategories)
                .map(EnumSet::copyOf)
                .orElseGet(() -> EnumSet.allOf(OptionCategory.class));

        return mappers.stream().filter(f -> allowedCategories.contains(f.getCategory())).collect(Collectors.toSet());
    }

    private static class MappersConfig extends MultivaluedHashMap<String, PropertyMapper<?>> {

        private final Map<OptionCategory, List<PropertyMapper<?>>> buildTimeMappers = new EnumMap<>(OptionCategory.class);
        private final Map<OptionCategory, List<PropertyMapper<?>>> runtimeTimeMappers = new EnumMap<>(OptionCategory.class);

        private final Map<String, PropertyMapper<?>> disabledBuildTimeMappers = new HashMap<>();
        private final Map<String, PropertyMapper<?>> disabledRuntimeMappers = new HashMap<>();

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

        public void addMapper(PropertyMapper<?> mapper) {
            handleMapper(mapper, this::add);
        }

        public void removeMapper(PropertyMapper<?> mapper) {
            handleMapper(mapper, this::remove);
        }

        private void remove(String key, PropertyMapper<?> mapper) {
            List<PropertyMapper<?>> list = get(key);
            if (CollectionUtil.isNotEmpty(list)) {
                list.remove(mapper);
            }
        }

        public void sanitizeDisabledMappers() {
            if (Environment.getParsedCommand().isEmpty()) return; // do not sanitize when no command is present

            DisabledMappersInterceptor.runWithDisabled(() -> { // We need to have the whole configuration available

                // Initialize profile in order to check state of features. Disable Persisted CS for re-augmentation
                if (isRebuildCheck()) {
                    PersistedConfigSource.getInstance().runWithDisabled(Environment::getCurrentOrCreateFeatureProfile);
                } else {
                    Environment.getCurrentOrCreateFeatureProfile();
                }

                sanitizeMappers(buildTimeMappers, disabledBuildTimeMappers);
                sanitizeMappers(runtimeTimeMappers, disabledRuntimeMappers);

                assertDuplicatedMappers();
            });
        }

        private void assertDuplicatedMappers() {
            final var duplicatedMappers = entrySet().stream()
                    .filter(e -> CollectionUtil.isNotEmpty(e.getValue()))
                    .filter(e -> e.getValue().size() > 1)
                    .toList();

            final var isBuildPhase = isRebuild() || isRebuildCheck() || isParsedCommand(Build.NAME);
            final var allowedForCommand = isParsedCommand(ShowConfig.NAME);

            if (!duplicatedMappers.isEmpty()) {
                duplicatedMappers.forEach(f -> {
                    final var filteredMappers = filterDeniedCategories(f.getValue());

                    if (filteredMappers.size() > 1) {
                        final var areBuildTimeMappers = filteredMappers.stream().anyMatch(PropertyMapper::isBuildTime);

                        // thrown in runtime, or in build time, when some mapper is marked as buildTime + not allowed to have duplicates for specific command
                        final var shouldBeThrown = !allowedForCommand && (!isBuildPhase || areBuildTimeMappers);
                        if (shouldBeThrown) {
                            throw new PropertyException(String.format("Duplicated mapper for key '%s'.", f.getKey()));
                        }
                    }
                });
            }
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
                            handleMapper(pm, disabledMappers::put);
                        }
                        return shouldRemove;
                    }));
        }

        private static void handleMapper(PropertyMapper<?> mapper, BiConsumer<String, PropertyMapper<?>> operation) {
            operation.accept(mapper.getFrom(), mapper);
            if (!mapper.getFrom().equals(mapper.getTo())) {
                operation.accept(mapper.getTo(), mapper);
            }
            operation.accept(mapper.getCliFormat(), mapper);
            operation.accept(mapper.getEnvVarFormat(), mapper);
        }
    }
}
