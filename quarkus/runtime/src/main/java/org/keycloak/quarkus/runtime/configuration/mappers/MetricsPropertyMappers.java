package org.keycloak.quarkus.runtime.configuration.mappers;

import org.keycloak.config.MetricsOptions;

import static org.keycloak.quarkus.runtime.configuration.Configuration.isTrue;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;


final class MetricsPropertyMappers implements PropertyMapperGrouping {

    public static final String METRICS_ENABLED_MSG = "metrics are enabled";

    @Override
    public PropertyMapper<?>[] getPropertyMappers() {
        return new PropertyMapper[] {
                fromOption(MetricsOptions.METRICS_ENABLED)
                        .to("quarkus.micrometer.enabled")
                        .build()
        };
    }

    public static boolean metricsEnabled() {
        return isTrue(MetricsOptions.METRICS_ENABLED);
    }
}
