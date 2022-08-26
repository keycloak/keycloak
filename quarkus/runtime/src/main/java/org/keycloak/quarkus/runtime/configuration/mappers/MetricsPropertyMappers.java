package org.keycloak.quarkus.runtime.configuration.mappers;

import org.keycloak.config.MetricsOptions;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;


final class MetricsPropertyMappers {

    private MetricsPropertyMappers(){}

    public static PropertyMapper[] getMetricsPropertyMappers() {
        return new PropertyMapper[] {
                fromOption(MetricsOptions.METRICS_ENABLED)
                        .to("quarkus.smallrye-metrics.extensions.enabled")
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .build()
        };
    }

}
