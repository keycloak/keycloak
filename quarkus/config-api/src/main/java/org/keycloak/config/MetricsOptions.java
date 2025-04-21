package org.keycloak.config;

public class MetricsOptions {

    public static final Option<Boolean> METRICS_ENABLED = new OptionBuilder<>("metrics-enabled", Boolean.class)
            .category(OptionCategory.METRICS)
            .description("If the server should expose metrics. If enabled, metrics are available at the '/metrics' endpoint.")
            .buildTime(true)
            .defaultValue(Boolean.FALSE)
            .build();

    public static final Option<Boolean> PASSWORD_VALIDATION_COUNTER_ENABLED = new OptionBuilder<>("metrics-password-validation-counter-enabled", Boolean.class)
            .category(OptionCategory.METRICS)
            .description("If the server should publish counter metric for number of password validations performed.")
            .defaultValue(Boolean.TRUE)
            .hidden() // This is intended to be enabled all the time when global metrics are enabled, therefore this option is hidden
            .build();
}
