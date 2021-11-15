package org.keycloak.quarkus.runtime.configuration.mappers;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class PropertyMappers {

    static final Map<String, PropertyMapper> MAPPERS = new HashMap<>();

    private PropertyMappers(){}

    static {
        addMappers(ClusteringPropertyMappers.getClusteringPropertyMappers());
        addMappers(DatabasePropertyMappers.getDatabasePropertyMappers());
        addMappers(HostnamePropertyMappers.getHostnamePropertyMappers());
        addMappers(HttpPropertyMappers.getHttpPropertyMappers());
        addMappers(MetricsPropertyMappers.getMetricsPropertyMappers());
        addMappers(ProxyPropertyMappers.getProxyPropertyMappers());
        addMappers(VaultPropertyMappers.getVaultPropertyMappers());
    }

    private static void addMappers(PropertyMapper[] mappers) {
        for (PropertyMapper mapper : mappers) {
            MAPPERS.put(mapper.getTo(), mapper);
        }
    }

    public static ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {
        PropertyMapper mapper = MAPPERS.getOrDefault(name, PropertyMapper.IDENTITY);
        ConfigValue configValue = mapper
                .getOrDefault(name, context, context.proceed(name));

        if (configValue == null) {
            Optional<String> prefixedMapper = getPrefixedMapper(name);

            if (prefixedMapper.isPresent()) {
                return MAPPERS.get(prefixedMapper.get()).getOrDefault(name, context, configValue);
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
                && !Environment.CLI_ARGS.equals(name)
                && !"kc.home.dir".equals(name)
                && !"kc.config.file".equals(name)
                && !Environment.PROFILE.equals(name)
                && !"kc.show.config".equals(name)
                && !"kc.show.config.runtime".equals(name)
                && !toCLIFormat("kc.config.file").equals(name);
    }

    private static boolean isSpiBuildTimeProperty(String name) {
        return name.startsWith("kc.spi") && (name.endsWith("provider") || name.endsWith("enabled"));
    }

    private static boolean isFeaturesBuildTimeProperty(String name) {
        return name.startsWith("kc.features");
    }

    public static String toCLIFormat(String name) {
        if (name.indexOf('.') == -1) {
            return name;
        }
        return MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX
                .concat(name.substring(3, name.lastIndexOf('.') + 1)
                        .replaceAll("\\.", "-") + name.substring(name.lastIndexOf('.') + 1));
    }

    public static List<PropertyMapper> getRuntimeMappers() {
        return MAPPERS.values().stream()
                .filter(entry -> !entry.isBuildTime()).collect(Collectors.toList());
    }

    public static List<PropertyMapper> getBuildTimeMappers() {
        return MAPPERS.values().stream()
                .filter(PropertyMapper::isBuildTime).collect(Collectors.toList());
    }

    public static String canonicalFormat(String name) {
        return name.replaceAll("-", "\\.");
    }

    public static String formatValue(String property, String value) {
        PropertyMapper mapper = getMapper(property);

        if (mapper != null && mapper.isMask()) {
            return "*******";
        }

        return value;
    }

    public static PropertyMapper getMapper(String property) {
        PropertyMapper mapper = MAPPERS.get(property);

        if (mapper != null) {
            return mapper;
        }

        return MAPPERS.values().stream().filter(new Predicate<PropertyMapper>() {
            @Override
            public boolean test(PropertyMapper propertyMapper) {
                return property.equals(propertyMapper.getFrom()) || property.equals(propertyMapper.getTo());
            }
        }).findFirst().orElse(null);
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
}
