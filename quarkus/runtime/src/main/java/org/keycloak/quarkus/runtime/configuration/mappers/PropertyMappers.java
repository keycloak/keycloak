package org.keycloak.quarkus.runtime.configuration.mappers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.MultivaluedHashMap;

import org.keycloak.common.Profile;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.config.ConfigSupportLevel;
import org.keycloak.config.Option;
import org.keycloak.config.OptionCategory;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.cli.Picocli;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.cli.command.AbstractCommand;
import org.keycloak.quarkus.runtime.configuration.DisabledMappersInterceptor;
import org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider;
import org.keycloak.quarkus.runtime.configuration.NestedPropertyMappingInterceptor;
import org.keycloak.quarkus.runtime.configuration.PersistedConfigSource;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.Expressions;
import org.jboss.logging.Logger;

import static org.keycloak.quarkus.runtime.Environment.isRebuild;
import static org.keycloak.quarkus.runtime.Environment.isRebuildCheck;
import static org.keycloak.quarkus.runtime.configuration.KeycloakConfigSourceProvider.isKeyStoreConfigSource;

public final class PropertyMappers {

    public static final String KC_SPI_PREFIX = "kc.spi";
    public static String VALUE_MASK = "*******";
    private static MappersConfig MAPPERS;
    private static final Logger log = Logger.getLogger(PropertyMappers.class);
    private final static List<PropertyMapperGrouping> GROUPINGS;
    static {
        GROUPINGS = List.of(new CachingPropertyMappers(), new DatabasePropertyMappers(),
                new ConfigKeystorePropertyMappers(), new EventPropertyMappers(), new ClassLoaderPropertyMappers(),
                new ExportPropertyMappers(), new BootstrapAdminPropertyMappers(), new HostnameV2PropertyMappers(),
                new HttpPropertyMappers(), new HttpAccessLogPropertyMappers(), new HealthPropertyMappers(),
                new FeaturePropertyMappers(), new ImportPropertyMappers(), new ManagementPropertyMappers(),
                new MetricsPropertyMappers(), new OpenApiPropertyMappers(), new LoggingPropertyMappers(), new ProxyPropertyMappers(),
                new VaultPropertyMappers(), new TracingPropertyMappers(), new TransactionPropertyMappers(),
                new SecurityPropertyMappers(), new TruststorePropertyMappers());
    }

    public static List<PropertyMapperGrouping> getPropertyMapperGroupings() {
        return GROUPINGS;
    }

    private PropertyMappers(){}

    static {
        reset();
    }

    public static void reset() {
        MAPPERS = new MappersConfig();
        GROUPINGS.forEach(g -> MAPPERS.addAll(g.getPropertyMappers()));
    }

    public static ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {
        PropertyMapper<?> mapper = getMapper(name);

        // During re-aug do not resolve server runtime properties and avoid they included by quarkus in the default value config source.
        //
        // The special handling of log properties is because some logging runtime properties are requested during build time
        // and we need to resolve them. That should be fine as they are generally not considered security sensitive.
        // If however expressions are not enabled that means quarkus is specifically looking for runtime defaults, and we should not provide a value
        // See https://github.com/quarkusio/quarkus/pull/42157
        if (isRebuild() && isKeycloakRuntime(name, mapper)
                && (NestedPropertyMappingInterceptor.getResolvingRoot().or(() -> Optional.of(name))
                        .filter(n -> n.startsWith("quarkus.log.") || n.startsWith("quarkus.console.")).isEmpty()
                        || !Expressions.isEnabled())) {
            return ConfigValue.builder().withName(name).build();
        }

        if (mapper == null) {
            return context.proceed(name);
        }
        return mapper.forKey(name).getConfigValue(name, context);
    }

    public static boolean isSpiBuildTimeProperty(String name) {
        // we can't require the new property formant until we're ok with a breaking change
        //return name.startsWith(KC_SPI_PREFIX) && (name.endsWith("--provider") || name.endsWith("--enabled") || name.endsWith("--provider-default"));
        return name.startsWith(KC_SPI_PREFIX) && (name.endsWith("-provider") || name.endsWith("-enabled") || name.endsWith("-provider-default"));
    }

    public static boolean isMaybeSpiBuildTimeProperty(String name) {
        return isSpiBuildTimeProperty(name) && !name.contains("--");
    }

    private static boolean isKeycloakRuntime(String name, PropertyMapper<?> mapper) {
        if (mapper == null) {
            return name.startsWith(MicroProfileConfigProvider.NS_KEYCLOAK) && !isSpiBuildTimeProperty(name);
        }
        return mapper.isRunTime();
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
    public static void sanitizeDisabledMappers(AbstractCommand command) {
        MAPPERS.sanitizeDisabledMappers(command);
    }

    public static String maskValue(String value, PropertyMapper<?> mapper) {
        return maskValue(value, null, mapper);
    }

    public static String maskValue(String value, String configSourceName, PropertyMapper<?> mapper) {
        if ((configSourceName != null && isKeyStoreConfigSource(configSourceName) || (mapper != null && mapper.isMask()))) {
            return VALUE_MASK;
        }

        return value;
    }

    public static PropertyMapper<?> getMapper(String property, OptionCategory category) {
        final var mappers = new ArrayList<>(MAPPERS.getOrDefault(property, Collections.emptyList()));
        if (category != null) {
            mappers.removeIf(m -> !m.getCategory().equals(category));
        }

        return switch (mappers.size()) {
            case 0 -> null;
            case 1 -> mappers.get(0);
            default -> {
                log.tracef("Duplicated mappers for key '%s'. Used the first found.", property);
                yield mappers.get(0);
            }
        };
    }

    public static PropertyMapper<?> getMapper(String property) {
        return getMapper(property, null);
    }

    public static PropertyMapper<?> getMapperByCliKey(String cliKey) {
        return getKcKeyFromCliKey(cliKey).map(PropertyMappers::getMapper).orElse(null);
    }

    public static Optional<String> getKcKeyFromCliKey(String cliKey) {
        if (!cliKey.startsWith(Picocli.ARG_PREFIX)) {
            return Optional.empty();
        }
        return Optional.ofNullable(
                MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX + cliKey.substring(Picocli.ARG_PREFIX.length()));
    }

    /**
     * @return a mutable copy of all known mappers
     */
    public static Set<PropertyMapper<?>> getMappers() {
        return MAPPERS.values().stream().flatMap(Collection::stream).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static Set<WildcardPropertyMapper<?>> getWildcardMappers() {
        return MAPPERS.getWildcardMappers();
    }

    public static WildcardPropertyMapper<?> getWildcardMappedFrom(Option<?> from) {
        return MAPPERS.wildcardConfig.wildcardMapFrom.get(from.getKey());
    }

    public static boolean isSupported(PropertyMapper<?> mapper) {
        ConfigSupportLevel supportLevel = mapper.getCategory().getSupportLevel();
        return supportLevel.equals(ConfigSupportLevel.SUPPORTED) || supportLevel.equals(ConfigSupportLevel.DEPRECATED);
    }

    public static Optional<PropertyMapper<?>> getDisabledMapper(String property) {
        if (property == null) {
            return Optional.empty();
        }

        PropertyMapper<?> mapper = getDisabledBuildTimeMappers().get(property);
        if (mapper == null) {
            mapper = getDisabledRuntimeMappers().get(property);
        }
        return Optional.ofNullable(mapper);
    }

    public static boolean isDisabledMapper(String property) {
        return getDisabledMapper(property).isPresent() && getMapper(property) == null;
    }

    private static class MappersConfig extends MultivaluedHashMap<String, PropertyMapper<?>> {

        private final Map<OptionCategory, List<PropertyMapper<?>>> buildTimeMappers = new EnumMap<>(OptionCategory.class);
        private final Map<OptionCategory, List<PropertyMapper<?>>> runtimeTimeMappers = new EnumMap<>(OptionCategory.class);

        private final Map<String, PropertyMapper<?>> disabledBuildTimeMappers = new HashMap<>();
        private final Map<String, PropertyMapper<?>> disabledRuntimeMappers = new HashMap<>();

        private final WildcardMappersConfig wildcardConfig = new WildcardMappersConfig();

        public void addAll(List<? extends PropertyMapper<?>> mappers) {
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
            if (mapper.hasWildcard()) {
                wildcardConfig.addMapper((WildcardPropertyMapper<?>) mapper);
            } else {
                handleMapper(mapper, this::add);
            }
        }

        public void removeMapper(PropertyMapper<?> mapper) {
            if (mapper.hasWildcard()) {
                wildcardConfig.removeMapper((WildcardPropertyMapper<?>) mapper);
            } else {
                handleMapper(mapper, this::remove);
            }
        }

        private void remove(String key, PropertyMapper<?> mapper) {
            List<PropertyMapper<?>> list = super.get(key);
            if (CollectionUtil.isNotEmpty(list)) {
                list.remove(mapper);
                if (list.isEmpty()) {
                    super.remove(key);
                }
            }
        }

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public List<PropertyMapper<?>> get(Object key) {
            // First check the base mappings
            String strKey = (String) key;

            List ret = super.get(key);
            if (ret != null) {
                return ret;
            }

            // TODO: we may want to introduce a prefix tree here as we add more wildcardMappers
            // for now we'll just limit ourselves to searching wildcards when we see a quarkus or
            // keycloak key
            ret = wildcardConfig.get(strKey);
            return !ret.isEmpty() ? ret : null;
        }

        @Override
        public List<PropertyMapper<?>> remove(Object mapper) {
            return super.remove(mapper);
        }

        public Set<WildcardPropertyMapper<?>> getWildcardMappers() {
            return Collections.unmodifiableSet(wildcardConfig.wildcardMappers);
        }

        public void sanitizeDisabledMappers(AbstractCommand command) {
            DisabledMappersInterceptor.runWithDisabled(() -> { // We need to have the whole configuration available

                // Initialize profile in order to check state of features. Disable Persisted CS for re-augmentation
                if (isRebuildCheck()) {
                    PersistedConfigSource.getInstance().runWithDisabled(Environment::getCurrentOrCreateFeatureProfile);
                } else {
                    Environment.getCurrentOrCreateFeatureProfile();
                    if (!command.shouldStart()) {
                        // this will use the deferred logger, which means it may not be seen in some circumstances
                        Profile.getInstance().logUnsupportedFeatures();
                    }
                }

                sanitizeMappers(buildTimeMappers, disabledBuildTimeMappers, command);
                sanitizeMappers(runtimeTimeMappers, disabledRuntimeMappers, command);

                entrySet().stream().filter(e -> e.getValue().size() > 1).findFirst().ifPresent(e -> {
                    throw new PropertyException(String.format("Duplicated mapper for key '%s'.", e.getKey()));
                });
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
                                            Map<String, PropertyMapper<?>> disabledMappers, AbstractCommand command) {
            mappers.forEach((category, propertyMappers) ->
                    propertyMappers.removeIf(pm -> {
                        final boolean shouldRemove = !pm.isEnabled(command);
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
                String to = mapper.getTo();
                operation.accept(to, mapper);
                if (to.startsWith(KC_SPI_PREFIX)) {
                    if (!mapper.getTo().contains("--")) {
                        throw new IllegalStateException("Mapper should use the new form of the SPI option with the `--` separator: " + to);
                    }
                    String legacyTo = mapper.getTo().replace("--", "-");
                    operation.accept(legacyTo, mapper);
                }
            }
        }
    }

    /**
     * Helper class for handling Mappers config for wildcards
     */
    private static class WildcardMappersConfig {
        private final Set<WildcardPropertyMapper<?>> wildcardMappers = new HashSet<>();
        private final Map<String, WildcardPropertyMapper<?>> wildcardMapFrom = new HashMap<>();

        public void addMapper(WildcardPropertyMapper<?> mapper) {
            if (mapper.getMapFrom() != null) {
                wildcardMapFrom.put(mapper.getMapFrom(), mapper);
            }
            wildcardMappers.add(mapper);
        }

        public void removeMapper(WildcardPropertyMapper<?> mapper) {
            wildcardMappers.remove(mapper);
            if (mapper.getFrom() != null) {
                wildcardMapFrom.remove(mapper.getMapFrom());
            }
        }

        public List<WildcardPropertyMapper<?>> get(String key) {
            // TODO: we may want to introduce a prefix tree here as we add more wildcardMappers
            // for now we'll just limit ourselves to searching wildcards when we see a quarkus or
            // keycloak key
            if (key.startsWith(MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX) || key.startsWith(MicroProfileConfigProvider.NS_QUARKUS_PREFIX)) {
                return wildcardMappers.stream()
                        .filter(m -> m.matchesWildcardOptionName(key))
                        .toList();
            }
            return Collections.emptyList();
        }
    }
}
