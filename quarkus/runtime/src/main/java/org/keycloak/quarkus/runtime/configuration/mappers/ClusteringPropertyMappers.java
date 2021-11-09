package org.keycloak.quarkus.runtime.configuration.mappers;

import java.util.Arrays;

final class ClusteringPropertyMappers {

    private ClusteringPropertyMappers() {
    }

    public static PropertyMapper[] getClusteringPropertyMappers() {
        return new PropertyMapper[] {
                builder().from("cluster")
                        .to("kc.spi.connections-infinispan.quarkus.config-file")
                        .defaultValue("default")
                        .transformer((value, context) -> "cluster-" + value + ".xml")
                        .description("Specifies clustering configuration. The specified value points to the infinispan " +
                                "configuration file prefixed with the 'cluster-` inside the distribution configuration directory. " +
                                "Value 'local' effectively disables clustering and use infinispan caches in the local mode. " +
                                "Value 'default' enables clustering for infinispan caches.")
                        .paramLabel("mode")
                        .isBuildTimeProperty(true)
                        .build(),
                builder().from("cluster-stack")
                        .to("kc.spi.connections-infinispan.quarkus.stack")
                        .description("Define the default stack to use for cluster communication and node discovery.")
                        .defaultValue("udp")
                        .paramLabel("stack")
                        .isBuildTimeProperty(true)
                        .expectedValues(Arrays.asList("tcp", "udp", "kubernetes", "ec2", "azure", "google"))
                        .build()
        };
    }

    private static PropertyMapper.Builder builder() {
        return PropertyMapper.builder(ConfigCategory.CLUSTERING);
    }
}
