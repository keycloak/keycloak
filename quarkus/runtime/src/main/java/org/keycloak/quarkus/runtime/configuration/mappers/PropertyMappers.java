package org.keycloak.quarkus.runtime.configuration.mappers;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;

import org.keycloak.config.ConfigSupportLevel;
import org.keycloak.config.OptionCategory;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.configuration.ConfigArgsConfigSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public final class PropertyMappers {

    public static String VALUE_MASK = "*******";
    static final MappersConfig MAPPERS = new MappersConfig();

    private PropertyMappers(){}

    static {
        MAPPERS.addAll(CachingPropertyMappers.getClusteringPropertyMappers());
        MAPPERS.addAll(DatabasePropertyMappers.getDatabasePropertyMappers());
        MAPPERS.addAll(HostnamePropertyMappers.getHostnamePropertyMappers());
        MAPPERS.addAll(HttpPropertyMappers.getHttpPropertyMappers());
        MAPPERS.addAll(HealthPropertyMappers.getHealthPropertyMappers());
        MAPPERS.addAll(MetricsPropertyMappers.getMetricsPropertyMappers());
        MAPPERS.addAll(ProxyPropertyMappers.getProxyPropertyMappers());
        MAPPERS.addAll(VaultPropertyMappers.getVaultPropertyMappers());
        MAPPERS.addAll(FeaturePropertyMappers.getMappers());
        MAPPERS.addAll(LoggingPropertyMappers.getMappers());
        MAPPERS.addAll(TransactionPropertyMappers.getTransactionPropertyMappers());
        MAPPERS.addAll(StoragePropertyMappers.getMappers());
    }

    public static ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {
        PropertyMapper mapper = MAPPERS.getOrDefault(name, PropertyMapper.IDENTITY);
        ConfigValue configValue = mapper.getConfigValue(name, context);

        if (configValue == null) {
            Optional<String> prefixedMapper = getPrefixedMapper(name);

            if (prefixedMapper.isPresent()) {
                return MAPPERS.get(prefixedMapper.get()).getConfigValue(name, context);
            }
        } else {
            configValue.withName(mapper.getTo());
        }

        return configValue;
    }

    public static boolean isBuildTimeProperty(String name) {
        if (isFeaturesBuildTimeProperty(name) || isSpiBuildTimeProperty(name)) {
            return true;
        }

        PropertyMapper mapper = MAPPERS.get(name);
        boolean isBuildTimeProperty = mapper == null ? false : mapper.isBuildTime();

        if (mapper == null && !isBuildTimeProperty) {
            Optional<String> prefixedMapper = PropertyMappers.getPrefixedMapper(name);

            if (prefixedMapper.isPresent()) {
                isBuildTimeProperty = MAPPERS.get(prefixedMapper.get()).isBuildTime();
            }
        }

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

    public static Map<OptionCategory, List<PropertyMapper>> getRuntimeMappers() {
        return MAPPERS.getRuntimeMappers();
    }

    public static Map<OptionCategory, List<PropertyMapper>> getBuildTimeMappers() {
        return MAPPERS.getBuildTimeMappers();
    }

    public static String formatValue(String property, String value) {
        property = removeProfilePrefixIfNeeded(property);
        PropertyMapper mapper = getMapper(property);

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

    public static PropertyMapper getMapper(String property) {
        if (property.startsWith("%")) {
            return MAPPERS.get(property.substring(property.indexOf('.') + 1));
        }
        return MAPPERS.get(property);
    }

    public static Collection<PropertyMapper> getMappers() {
        return MAPPERS.values();
    }

    public static boolean isSupported(PropertyMapper mapper) {
        return mapper.getCategory().getSupportLevel().equals(ConfigSupportLevel.SUPPORTED);
    }

    private static Optional<String> getPrefixedMapper(String name) {
        return MAPPERS.entrySet().stream().filter(
                new Predicate<Map.Entry<String, PropertyMapper>>() {
                    @Override
                    public boolean test(Map.Entry<String, PropertyMapper> entry) {
                        String key = entry.getKey();

                        if (!key.endsWith(".")) {
                            return false;
                        }

                        // checks both to and from mapping
                        return name.startsWith(key) || name.startsWith(entry.getValue().getFrom());
                    }
                })
                .map(Map.Entry::getKey)
                .findAny();
    }

    private static class MappersConfig extends HashMap<String, PropertyMapper> {

        private Map<OptionCategory, List<PropertyMapper>> buildTimeMappers = new EnumMap<>(OptionCategory.class);
        private Map<OptionCategory, List<PropertyMapper>> runtimeTimeMappers = new EnumMap<>(OptionCategory.class);

        public void addAll(PropertyMapper[] mappers) {
            for (PropertyMapper mapper : mappers) {
                super.put(mapper.getTo(), mapper);
                super.put(mapper.getFrom(), mapper);
                super.put(mapper.getCliFormat(), mapper);
                super.put(mapper.getEnvVarFormat(), mapper);

                if (mapper.isBuildTime()) {
                    addMapperByStage(mapper, buildTimeMappers);
                } else {
                    addMapperByStage(mapper, runtimeTimeMappers);
                }
            }
        }

        private void addMapperByStage(PropertyMapper mapper, Map<OptionCategory, List<PropertyMapper>> mappers) {
            mappers.computeIfAbsent(mapper.getCategory(),
                    new Function<OptionCategory, List<PropertyMapper>>() {
                        @Override
                        public List<PropertyMapper> apply(OptionCategory c) {
                            return new ArrayList<>();
                        }
                    }).add(mapper);
        }

        @Override
        public PropertyMapper put(String key, PropertyMapper value) {
            if (containsKey(key)) {
                throw new IllegalArgumentException("Duplicated mapper for key [" + key + "]");
            }
            return super.put(key, value);
        }

        public Map<OptionCategory, List<PropertyMapper>> getRuntimeMappers() {
            return runtimeTimeMappers;
        }

        public Map<OptionCategory, List<PropertyMapper>> getBuildTimeMappers() {
            return buildTimeMappers;
        }
    }

}
