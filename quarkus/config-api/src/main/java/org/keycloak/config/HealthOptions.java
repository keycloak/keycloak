package org.keycloak.config;

import java.util.ArrayList;
import java.util.List;

public class HealthOptions {

    public static final Option HEALTH_ENABLED = new OptionBuilder<>("health-enabled", Boolean.class)
            .category(OptionCategory.HEALTH)
            .description("If the server should expose health check endpoints. If enabled, health checks are available at the '/health', '/health/ready' and '/health/live' endpoints.")
            .defaultValue(Boolean.FALSE)
            .buildTime(true)
            .build();

    public static final List<Option<?>> ALL_OPTIONS = new ArrayList<>();

    static {
        ALL_OPTIONS.add(HEALTH_ENABLED);
    }
}
