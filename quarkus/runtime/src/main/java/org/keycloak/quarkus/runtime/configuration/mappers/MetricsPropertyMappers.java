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
                        .build(),
                fromOption(MetricsOptions.PASSWORD_VALIDATION_COUNTER_ENABLED)
                        .to("kc.spi-credential--keycloak-password--metrics-enabled")
                        .isEnabled(MetricsPropertyMappers::metricsEnabled, "metrics are enabled")
                        .build(),
                fromOption(MetricsOptions.INFINISPAN_METRICS_ENABLED)
                        .mapFrom(MetricsOptions.METRICS_ENABLED)
                        .to("kc.spi-cache-embedded--default--metrics-enabled")
                        .isEnabled(MetricsPropertyMappers::metricsEnabled, "metrics are enabled")
                        .build()
        };
    }

    public static boolean metricsEnabled() {
        return isTrue(MetricsOptions.METRICS_ENABLED);
    }
}
