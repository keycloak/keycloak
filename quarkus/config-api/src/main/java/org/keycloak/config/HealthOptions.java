package org.keycloak.config;

public class HealthOptions {

    public static final Option<Boolean> HEALTH_ENABLED = new OptionBuilder<>("health-enabled", Boolean.class)
            .category(OptionCategory.HEALTH)
            .description("If the server should expose health check endpoints. If enabled, health checks are available at the '/health', '/health/ready' and '/health/live' endpoints.")
            .defaultValue(Boolean.FALSE)
            .buildTime(true)
            .build();

}
