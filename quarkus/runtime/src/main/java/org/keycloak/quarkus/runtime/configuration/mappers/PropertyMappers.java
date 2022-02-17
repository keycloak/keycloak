package org.keycloak.quarkus.runtime.configuration.mappers;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.configuration.ConfigArgsConfigSource;
import org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class PropertyMappers {

    public static String VALUE_MASK = "*******";
    static final MappersConfig MAPPERS = new MappersConfig();

    private PropertyMappers(){}

    static {
        MAPPERS.addAll(ClusteringPropertyMappers.getClusteringPropertyMappers());
        MAPPERS.addAll(DatabasePropertyMappers.getDatabasePropertyMappers());
        MAPPERS.addAll(HostnamePropertyMappers.getHostnamePropertyMappers());
        MAPPERS.addAll(HttpPropertyMappers.getHttpPropertyMappers());
        MAPPERS.addAll(MetricsPropertyMappers.getMetricsPropertyMappers());
        MAPPERS.addAll(ProxyPropertyMappers.getProxyPropertyMappers());
        MAPPERS.addAll(VaultPropertyMappers.getVaultPropertyMappers());
        MAPPERS.addAll(FeaturePropertyMappers.getMappers());
        MAPPERS.addAll(LoggingPropertyMappers.getMappers());
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
        if (isFeaturesBuildTimeProperty(name) || isSpiBuildTimeProperty(name) || name.startsWith(MicroProfileConfigProvider.NS_QUARKUS_PREFIX)) {
            return true;
        }

        if (!name.startsWith(MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX)) {
            return false;
        }

        boolean isBuildTimeProperty = MAPPERS.entrySet().stream()
                .anyMatch(new Predicate<Map.Entry<String, PropertyMapper>>() {
                    @Override
                    public boolean test(Map.Entry<String, PropertyMapper> entry) {
                        PropertyMapper mapper = entry.getValue();
                        return (mapper.getFrom().equals(name) || mapper.getTo().equals(name)) && mapper.isBuildTime();
                    }
                });

        if (!isBuildTimeProperty) {
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

    public static List<PropertyMapper> getRuntimeMappers() {
        return MAPPERS.values().stream()
                .filter(entry -> !entry.isBuildTime()).collect(Collectors.toList());
    }

    public static List<PropertyMapper> getBuildTimeMappers() {
        return MAPPERS.values().stream()
                .filter(PropertyMapper::isBuildTime).collect(Collectors.toList());
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

        public void addAll(PropertyMapper[] mappers) {
            for (PropertyMapper mapper : mappers) {
                super.put(mapper.getTo(), mapper);
                super.put(mapper.getFrom(), mapper);
                super.put(mapper.getCliFormat(), mapper);
                super.put(mapper.getEnvVarFormat(), mapper);
            }
        }

        @Override
        public PropertyMapper put(String key, PropertyMapper value) {
            if (containsKey(key)) {
                throw new IllegalArgumentException("Duplicated mapper for key [" + key + "]");
            }
            return super.put(key, value);
        }
    }
}
