package org.keycloak.config;

public class HealthOptions {

    public static final Option<Boolean> HEALTH_ENABLED = new OptionBuilder<>("health-enabled", Boolean.class)
            .category(OptionCategory.HEALTH)
            .description("If the server should expose health check endpoints. If enabled, health checks are available at the '/health', '/health/ready' and '/health/live' endpoints.")
            .defaultValue(Boolean.FALSE)
            .buildTime(true)
            .build();

    public static final Option<Boolean> HEALTH_CLASSIC_PROBES_ENABLED = new OptionBuilder<>("health-classic-probes-enabled", Boolean.class)
            .category(OptionCategory.HEALTH)
            .description("If enabled, use the original Quarkus blocking handlers for '/health/ready' and '/health/live' endpoints.")
            .defaultValue(Boolean.FALSE)
            .buildTime(true)
            .hidden()
            .build();

}
