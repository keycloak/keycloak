package org.keycloak.config;

public class MetricsOptions {

    public static final Option<Boolean> METRICS_ENABLED = new OptionBuilder<>("metrics-enabled", Boolean.class)
            .category(OptionCategory.METRICS)
            .description("If the server should expose metrics. If enabled, metrics are available at the '/metrics' endpoint.")
            .buildTime(true)
            .defaultValue(Boolean.FALSE)
            .build();

}
