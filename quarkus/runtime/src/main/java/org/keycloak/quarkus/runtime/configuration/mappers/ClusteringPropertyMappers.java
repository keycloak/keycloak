package org.keycloak.quarkus.runtime.configuration.mappers;

import org.keycloak.config.ClusteringOptions;
import org.keycloak.quarkus.runtime.Environment;

import io.smallrye.config.ConfigSourceInterceptorContext;

import static java.util.Optional.of;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

import java.util.Optional;

final class ClusteringPropertyMappers {

    private ClusteringPropertyMappers() {
    }

    public static PropertyMapper[] getClusteringPropertyMappers() {
        return new PropertyMapper[] {
                fromOption(ClusteringOptions.CACHE)
                        .paramLabel("type")
                        .build(),
                fromOption(ClusteringOptions.CACHE_STACK)
                        .to("kc.spi-connections-infinispan-quarkus-stack")
                        .paramLabel("stack")
                        .build(),
                fromOption(ClusteringOptions.CACHE_CONFIG_FILE)
                        .mapFrom("cache")
                        .to("kc.spi-connections-infinispan-quarkus-config-file")
                        .transformer(ClusteringPropertyMappers::resolveConfigFile)
                        .paramLabel("file")
                        .build()
        };
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
            pathPrefix = homeDir + "/conf/";
        }

        return of(pathPrefix + value.get());
    }
}
