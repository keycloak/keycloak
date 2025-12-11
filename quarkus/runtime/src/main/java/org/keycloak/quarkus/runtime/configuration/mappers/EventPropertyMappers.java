package org.keycloak.quarkus.runtime.configuration.mappers;

import java.util.List;

import org.keycloak.common.Profile;

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
                fromOption(USER_EVENT_METRICS_EVENTS)
                        .to("kc.spi-events-listener--micrometer-user-event-metrics--events")
                        .paramLabel("events")
                        .isEnabled(EventPropertyMappers::userEventsMetricsTags, "user event metrics are enabled")
                        .build()
        );
    }

    private static boolean userEventsMetricsEnabled() {
        return metricsEnabled() && Profile.isFeatureEnabled(Profile.Feature.USER_EVENT_METRICS);
    }

    private static boolean userEventsMetricsTags() {
        return isTrue(USER_EVENT_METRICS_ENABLED);
    }

}
