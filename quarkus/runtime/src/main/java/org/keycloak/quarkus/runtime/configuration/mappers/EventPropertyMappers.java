package org.keycloak.quarkus.runtime.configuration.mappers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.keycloak.common.Profile;
import org.keycloak.events.EventType;

import static org.keycloak.config.EventOptions.USER_EVENT_METRICS_ENABLED;
import static org.keycloak.config.EventOptions.USER_EVENT_METRICS_EVENTS;
import static org.keycloak.config.EventOptions.USER_EVENT_METRICS_TAGS;
import static org.keycloak.quarkus.runtime.configuration.Configuration.isTrue;
import static org.keycloak.quarkus.runtime.configuration.mappers.MetricsPropertyMappers.METRICS_ENABLED_MSG;
import static org.keycloak.quarkus.runtime.configuration.mappers.MetricsPropertyMappers.metricsEnabled;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;


final class EventPropertyMappers implements PropertyMapperGrouping {

    @Override
    public List<PropertyMapper<?>> getPropertyMappers() {
        return List.of(
                fromOption(USER_EVENT_METRICS_ENABLED)
                        .to("kc.spi-events-listener--micrometer-user-event-metrics--enabled")
                        .isEnabled(EventPropertyMappers::userEventsMetricsEnabled, METRICS_ENABLED_MSG + " and feature " + Profile.Feature.USER_EVENT_METRICS.getKey() + " is enabled")
                        .build(),
                fromOption(USER_EVENT_METRICS_TAGS)
                        .to("kc.spi-events-listener--micrometer-user-event-metrics--tags")
                        .paramLabel("tags")
                        .isEnabled(EventPropertyMappers::userEventsMetricsTags, "user event metrics are enabled")
                        .build(),
                fromOption(USER_EVENT_METRICS_EVENTS.toBuilder().expectedValues(expectedUserMetricEvents()).build())
                        .to("kc.spi-events-listener--micrometer-user-event-metrics--events")
                        .paramLabel("events")
                        .isEnabled(EventPropertyMappers::userEventsMetricsTags, "user event metrics are enabled")
                        .build()
        );
    }

    private static List<String> expectedUserMetricEvents() {
        List<String> values = new ArrayList<>();
        for (EventType event : EventType.values()) {
            if (event.name().endsWith("_ERROR")) {
                continue;
            }
            if (event == EventType.VALIDATE_ACCESS_TOKEN) {
                // event is deprecated and no longer used in the code base
                continue;
            }
            String value = event.name().toLowerCase();
            values.add(value);
        }
        Collections.sort(values);
        return values;
    }

    private static boolean userEventsMetricsEnabled() {
        return metricsEnabled() && Profile.isFeatureEnabled(Profile.Feature.USER_EVENT_METRICS);
    }

    private static boolean userEventsMetricsTags() {
        return isTrue(USER_EVENT_METRICS_ENABLED);
    }

}
