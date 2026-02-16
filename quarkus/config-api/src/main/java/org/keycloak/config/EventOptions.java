package org.keycloak.config;

import java.util.List;

public class EventOptions {

    public static final Option<Boolean> USER_EVENT_METRICS_ENABLED = new OptionBuilder<>("event-metrics-user-enabled", Boolean.class)
            .category(OptionCategory.EVENTS)
            .description("Create metrics based on user events.")
            .buildTime(true)
            .defaultValue(Boolean.FALSE)
            .build();

    public static final Option<List<String>> USER_EVENT_METRICS_TAGS = OptionBuilder.listOptionBuilder("event-metrics-user-tags", String.class)
            .category(OptionCategory.EVENTS)
            .description("Comma-separated list of tags to be collected for user event metrics. By default only 'realm' is enabled to avoid a high metrics cardinality.")
            .buildTime(false)
            .expectedValues(List.of("realm", "idp", "clientId"))
            .defaultValue(List.of("realm"))
            .build();

    public static final Option<List<String>> USER_EVENT_METRICS_EVENTS = OptionBuilder.listOptionBuilder("event-metrics-user-events", String.class)
            .category(OptionCategory.EVENTS)
            .description("Comma-separated list of events to be collected for user event metrics. This option can be used to reduce the number of metrics created as by default all user events create a metric.")
            .buildTime(false)
            .deprecatedMetadata(DeprecatedMetadata.deprecateValues("Use `remove_credential` instead of `remove_totp`, and `update_credential` instead of `update_totp` and `update_password`.", "remove_totp", "update_totp", "update_password"))
            .build();

}
