package org.keycloak.quarkus.runtime.configuration.mappers;

import org.keycloak.config.MetricsOptions;

import static org.keycloak.quarkus.runtime.configuration.Configuration.isTrue;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;


final class MetricsPropertyMappers {

    public static final String METRICS_ENABLED_MSG = "metrics are enabled";

    private MetricsPropertyMappers(){}

    public static PropertyMapper<?>[] getMetricsPropertyMappers() {
        return new PropertyMapper[] {
                fromOption(MetricsOptions.METRICS_ENABLED)
                        .to("quarkus.micrometer.enabled")
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .build()
        };
    }

    public static boolean metricsEnabled() {
        return isTrue(MetricsOptions.METRICS_ENABLED);
    }
}
