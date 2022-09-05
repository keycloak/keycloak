package org.keycloak.quarkus.runtime.configuration.mappers;

import org.keycloak.config.HealthOptions;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;


final class HealthPropertyMappers {

    private HealthPropertyMappers(){}

    public static PropertyMapper[] getHealthPropertyMappers() {
        return new PropertyMapper[] {
                fromOption(HealthOptions.HEALTH_ENABLED)
                        .to("quarkus.health.extensions.enabled")
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .build()
        };
    }

}
