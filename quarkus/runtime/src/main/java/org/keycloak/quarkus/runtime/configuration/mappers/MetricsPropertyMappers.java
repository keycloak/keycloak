package org.keycloak.quarkus.runtime.configuration.mappers;

import java.util.Arrays;


final class MetricsPropertyMappers {

    private MetricsPropertyMappers(){}

    public static PropertyMapper[] getMetricsPropertyMappers() {
        return new PropertyMapper[] {
                builder().from("metrics-enabled")
                        .to("quarkus.datasource.metrics.enabled")
                        .isBuildTimeProperty(true)
                        .defaultValue(Boolean.FALSE.toString())
                        .description("If the server should expose metrics and healthcheck. If enabled, metrics are available at the '/metrics' endpoint and healthcheck at the '/health' endpoint.")
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .expectedValues(Arrays.asList(Boolean.TRUE.toString(), Boolean.FALSE.toString()))
                        .build()
        };
    }

    private static PropertyMapper.Builder builder() {
        return PropertyMapper.builder(ConfigCategory.METRICS);
    }
}
