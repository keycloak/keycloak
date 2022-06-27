package org.keycloak.config;

import java.util.ArrayList;
import java.util.List;

public class MetricsOptions {

    public static final Option METRICS_ENABLED = new OptionBuilder<>("metrics-enabled", Boolean.class)
            .category(OptionCategory.METRICS)
            .description("If the server should expose metrics. If enabled, metrics are available at the '/metrics' endpoint.")
            .buildTime(true)
            .defaultValue(Boolean.FALSE)
            .expectedValues(Boolean.TRUE, Boolean.FALSE)
            .build();

    public static final List<Option<?>> ALL_OPTIONS = new ArrayList<>();

    static {
         ALL_OPTIONS.add(METRICS_ENABLED);
    }
}
