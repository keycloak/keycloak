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

import java.util.HashSet;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.BaseUnits;
import org.bouncycastle.util.Strings;

public class MicrometerUserEventMetricsEventListenerProviderFactory implements EventListenerProviderFactory, EnvironmentDependentProviderFactory {

    private static final String ID = "micrometer-user-event-metrics";
    private static final String TAGS_OPTION = "tags";
    private static final String EVENTS_OPTION = "events";
    private static final String DESCRIPTION_OF_EVENT_METER = "Keycloak user events";
    // Micrometer naming convention that separates lowercase words with a . (dot) character.
    private static final String KEYCLOAK_METER_NAME_PREFIX = "keycloak.";
    private static final String USER_EVENTS_METER_NAME = KEYCLOAK_METER_NAME_PREFIX + "user";

    private boolean withIdp, withRealm, withClientId;

    private HashSet<String> events;
    private Meter.MeterProvider<Counter> meterProvider;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new MicrometerUserEventMetricsEventListenerProvider(session, withIdp, withRealm, withClientId, events, meterProvider);
    }

    @Override
    public void init(Config.Scope config) {
        meterProvider = Counter.builder(USER_EVENTS_METER_NAME)
                .description(DESCRIPTION_OF_EVENT_METER)
                .baseUnit(BaseUnits.EVENTS).withRegistry(Metrics.globalRegistry);

        String tagsConfig = config.get(TAGS_OPTION);
        if (tagsConfig != null) {
            for (String s : Strings.split(tagsConfig, ',')) {
                switch (s.trim()) {
                    case "idp" -> withIdp = true;
                    case "realm" -> withRealm = true;
                    case "clientId" -> withClientId = true;
                    default -> throw new IllegalArgumentException("Unknown tag for collecting user event metrics: '" + s + "'");
                }
            }
        }
        String eventsConfig = config.get(EVENTS_OPTION);
        if (eventsConfig != null && !eventsConfig.trim().isEmpty()) {
            events = new HashSet<>();
            for (String s : Strings.split(eventsConfig, ',')) {
                events.add(s.trim());
            }
        }

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
        // nothing to do
    }

    @Override
    public String getId() {
        return ID;
    }


    @Override
    public boolean isGlobal() {
        return true;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        Boolean enabled = config.getBoolean("enabled");
        return enabled != null && enabled;
    }

}
