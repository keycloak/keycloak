package org.keycloak.config;

public class HealthOptions {

    public static final Option<Boolean> HEALTH_ENABLED = new OptionBuilder<>("health-enabled", Boolean.class)
            .category(OptionCategory.HEALTH)
            .description("If the server should expose health check endpoints. If enabled, health checks are available at the '/health', '/health/ready', '/health/live', and '/health/started' endpoints.")
            .defaultValue(Boolean.FALSE)
            .buildTime(true)
            .build();

    public static final Option<Boolean> HEALTH_ON_MAIN = new OptionBuilder<>("health-on-main", Boolean.class)
            .category(OptionCategory.HEALTH)
            .description("If the server should expose health checks om amain. If enabled, health checks are available at the '/health/ready' endpoint on the main interface.")
            .defaultValue(Boolean.FALSE)
            .buildTime(true)
            .build();

}
