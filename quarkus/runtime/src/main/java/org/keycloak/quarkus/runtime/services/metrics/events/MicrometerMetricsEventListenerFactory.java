/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.quarkus.runtime.services.metrics.events;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.config.MetricsOptions;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class MicrometerMetricsEventListenerFactory implements EventListenerProviderFactory {

    private static final Logger logger = Logger.getLogger(MicrometerMetricsEventListenerFactory.class);
    private static final EventListenerProvider NO_OP_LISTENER = new NoOpEventListenerProvider();
    private static final String ID = "micrometer-metrics";
    private static final String EXCLUDED_EVENTS_OPTION = "excluded-events";
    private static final String ADMIN_EVENTS_ENABLED_OPTION = "admin-events-enabled";

    private final EnumSet<EventType> includedEvents = EnumSet.allOf(EventType.class);
    private boolean metricsEnabled;
    private boolean adminEventsEnabled;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        if (metricsEnabled) {
            return new MicrometerMetricsEventListener(session, includedEvents, adminEventsEnabled);
        } else {
            return NO_OP_LISTENER;
        }
    }

    @Override
    public void init(Config.Scope config) {
        String[] excluded = config.getArray(EXCLUDED_EVENTS_OPTION);
        if (excluded != null) {
            for (String e : excluded) {
                includedEvents.remove(EventType.valueOf(e.toUpperCase()));
            }
        }
        adminEventsEnabled = config.getBoolean(ADMIN_EVENTS_ENABLED_OPTION, true);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        metricsEnabled = Configuration.isTrue(MetricsOptions.METRICS_ENABLED);
        if (!metricsEnabled) {
            logger.warn("Invalid '" + ID + "' EventListenerProvider configuration. Available only when metrics are enabled. Using NoOpEventListener.");
        }
    }

    @Override
    public void close() {
        // nothing to do
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        String[] supportedEvents = Arrays.stream(EventType.values())
                .map(EventType::name)
                .map(String::toLowerCase)
                .sorted(Comparator.naturalOrder())
                .toArray(String[]::new);
        return ProviderConfigurationBuilder.create()
                .property()
                .name(EXCLUDED_EVENTS_OPTION)
                .type(ProviderConfigProperty.STRING_TYPE)
                .helpText("A comma-separated list of events that should not be collected as a metric.")
                .options(supportedEvents)
                .add()
                .property()
                .name(ADMIN_EVENTS_ENABLED_OPTION)
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .helpText("AdminEvents should be collected as a metric.")
                .defaultValue(true)
                .add()
                .build();
    }

    @Override
    public String getId() {
        return ID;
    }

    private static class NoOpEventListenerProvider implements EventListenerProvider {
        @Override
        public void onEvent(Event event) {
           // do nothing
        }

        @Override
        public void onEvent(AdminEvent event, boolean includeRepresentation) {
           // do nothing
        }

        @Override
        public void close() {
            // do nothing
        }
    }
}
