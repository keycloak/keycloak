package org.keycloak.quarkus.runtime.configuration.mappers;

import java.util.Arrays;
import java.util.function.BiFunction;

import org.keycloak.quarkus.runtime.Environment;

import io.smallrye.config.ConfigSourceInterceptorContext;

final class ClusteringPropertyMappers {

    private ClusteringPropertyMappers() {
    }

    public static PropertyMapper[] getClusteringPropertyMappers() {
        return new PropertyMapper[] {
                builder().from("cache")
                        .defaultValue("ispn")
                        .description("Defines the cache mechanism for high-availability. "
                                + "By default, a 'ispn' cache is used to create a cluster between multiple server nodes. "
                                + "A 'local' cache disables clustering and is intended for development and testing purposes.")
                        .paramLabel("type")
                        .isBuildTimeProperty(true)
                        .expectedValues("local", "ispn")
                        .build(),
                builder().from("cache-stack")
                        .to("kc.spi-connections-infinispan-quarkus-stack")
                        .description("Define the default stack to use for cluster communication and node discovery. This option only takes effect "
                                + "if 'cache' is set to 'ispn'. Default: udp.")
                        .paramLabel("stack")
                        .isBuildTimeProperty(true)
                        .expectedValues(Arrays.asList("tcp", "udp", "kubernetes", "ec2", "azure", "google"))
                        .build(),
                builder().from("cache-config-file")
                        .mapFrom("cache")
                        .to("kc.spi-connections-infinispan-quarkus-config-file")
                        .description("Defines the file from which cache configuration should be loaded from.")
                        .transformer(new BiFunction<String, ConfigSourceInterceptorContext, String>() {
                            @Override
                            public String apply(String value, ConfigSourceInterceptorContext context) {
                                if ("local".equals(value)) {
                                    return "cache-local.xml";
                                } else if ("ispn".equals(value)) {
                                    return "cache-ispn.xml";
                                }

                                String pathPrefix;
                                String homeDir = Environment.getHomeDir();

                                if (homeDir == null) {
                                    pathPrefix = "";
                                } else {
                                    pathPrefix = homeDir + "/conf/";
                                }

                                return pathPrefix + value;
                            }
                        })
                        .paramLabel("file")
                        .isBuildTimeProperty(true)
                        .build()
        };
    }

    private static PropertyMapper.Builder builder() {
        return PropertyMapper.builder(ConfigCategory.CLUSTERING);
    }
}
