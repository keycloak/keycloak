package org.keycloak.quarkus.runtime.configuration.mappers;

import java.util.Arrays;


final class HealthPropertyMappers {

    private HealthPropertyMappers(){}

    public static PropertyMapper[] getHealthPropertyMappers() {
        return new PropertyMapper[] {
                builder().from("health-enabled")
                        .to("quarkus.datasource.health.enabled")
                        .isBuildTimeProperty(true)
                        .defaultValue(Boolean.FALSE.toString())
                        .description("If the server should expose health check endpoints. If enabled, health checks are available at the '/health', '/health/ready' and '/health/live' endpoints.")
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .expectedValues(Arrays.asList(Boolean.TRUE.toString(), Boolean.FALSE.toString()))
                        .build()
        };
    }

    private static PropertyMapper.Builder builder() {
        return PropertyMapper.builder(ConfigCategory.HEALTH);
    }
}
