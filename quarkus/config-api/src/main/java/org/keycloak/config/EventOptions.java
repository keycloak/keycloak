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
            .description("Comma-separated list of events to be collected for user event metrics. Reduce the number of metrics. If empty or not set, all events create a metric.")
            .buildTime(false)
            .build();

}


