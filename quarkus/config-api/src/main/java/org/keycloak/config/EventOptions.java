package org.keycloak.config;

public class EventOptions {

    public static final Option<Boolean> USER_EVENT_METRICS_ENABLED = new OptionBuilder<>("user-event-metrics-enabled", Boolean.class)
            .category(OptionCategory.EVENTS)
            .description("Create metrics based on user events.")
            .buildTime(false)
            .defaultValue(Boolean.FALSE)
            .build();

    public static final Option<String> USER_EVENT_METRICS_TAGS = new OptionBuilder<>("user-event-metrics-tags", String.class)
            .category(OptionCategory.EVENTS)
            .description("Comma-separated list of tags to be collected for event metrics. Reduce the tags to avoid a high metrics cardinality if you use a lot of client, realms or IDPs.")
            .buildTime(false)
            .defaultValue("realm,clientId,idp")
            .build();

    public static final Option<String> USER_EVENT_METRICS_EVENTS = new OptionBuilder<>("user-event-metrics-events", String.class)
            .category(OptionCategory.EVENTS)
            .description("Comma-separated list of events to be collected for event metrics. Reduce the number of metrics. If empty or not set, all events create a metric.")
            .buildTime(false)
            .build();

}


