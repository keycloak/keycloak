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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.BaseUnits;
import org.jboss.logging.Logger;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerTransaction;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.stream.Collectors;

public class MicrometerMetricsEventListener implements EventListenerProvider {

    private static final Logger logger = Logger.getLogger(MicrometerMetricsEventListener.class);

    private static final String EMPTY_IDP = "";
    private static final String REALM_TAG = "realm";
    private static final String IDP_TAG = "idp";
    private static final String CLIENT_ID_TAG = "client.id";
    private static final String ERROR_TAG = "error";
    private static final String EVENT_TAG = "event";
    // TODO better description
    private static final String DESCRIPTION_OF_EVENT_METER = "The total number of Keycloak events";
    private static final String KEYLOAK_METER_NAME = "keycloak";
    // TODO better name for simple
    private static final String SIMPLE_EVENT_METER_NAME = KEYLOAK_METER_NAME + ".simple";

    private static final Map<EventType, String> EVENT_TYPE_TO_TAG_VALUE =
            Arrays.stream(EventType.values())
                    .collect(Collectors.toMap(e -> e, MicrometerMetricsEventListenerFactory::format));

    private final EventListenerTransaction tx =
            new EventListenerTransaction(null, this::countEvent);

    private final EnumSet<EventType> includedEvents;
    private final EnumSet<EventType> eventsWithAdditionalTags;

    public MicrometerMetricsEventListener(KeycloakSession session,
                                          EnumSet<EventType> includedEvents,
                                          EnumSet<EventType> eventsWithAdditionalTags) {
        session.getTransactionManager().enlistAfterCompletion(tx);
        this.includedEvents = includedEvents;
        this.eventsWithAdditionalTags = eventsWithAdditionalTags;
    }

    @Override
    public void onEvent(Event event) {
        if (includedEvents.contains(event.getType())) {
            tx.addEvent(event);
        }
    }

    private void countEvent(Event event) {
        logger.debugf("Received user event of type %s in realm %s",
                event.getType().name(), event.getRealmName());
        if (eventsWithAdditionalTags.contains(event.getType())) {
            countRealmEventIdpClientIdErrorTagsFromEvent(event);
        } else {
            countRealmEventTagsFromEvent(event);
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // do nothing
    }

    private void countRealmEventTagsFromEvent(final Event event) {
        Counter.builder(SIMPLE_EVENT_METER_NAME)
                .description(DESCRIPTION_OF_EVENT_METER)
                .tags(Tags.of(Tag.of(REALM_TAG, nullToEmpty(event.getRealmName())),
                        Tag.of(EVENT_TAG, EVENT_TYPE_TO_TAG_VALUE.get(event.getType()))))
                .baseUnit(BaseUnits.EVENTS)
                .register(Metrics.globalRegistry)
                .increment();
    }

    private void countRealmEventIdpClientIdErrorTagsFromEvent(final Event event) {
        Counter.builder(KEYLOAK_METER_NAME)
                .description(DESCRIPTION_OF_EVENT_METER)
                .tags(Tags.of(Tag.of(REALM_TAG, nullToEmpty(event.getRealmName())),
                        Tag.of(EVENT_TAG, EVENT_TYPE_TO_TAG_VALUE.get(event.getType())),
                        Tag.of(IDP_TAG, getIdentityProvider(event)),
                        Tag.of(CLIENT_ID_TAG, getErrorClientId(event)),
                        Tag.of(ERROR_TAG, nullToEmpty(event.getError()))))
                .baseUnit(BaseUnits.EVENTS)
                .register(Metrics.globalRegistry)
                .increment();
    }

    private String getIdentityProvider(Event event) {
        String identityProvider = null;
        if (event.getDetails() != null) {
            identityProvider = event.getDetails().get(Details.IDENTITY_PROVIDER);
        }
        if (identityProvider == null) {
            identityProvider = EMPTY_IDP;
        }
        return identityProvider;
    }


    private String getErrorClientId(Event event) {
        return nullToEmpty(Errors.CLIENT_NOT_FOUND.equals(event.getError())
                ? Errors.CLIENT_NOT_FOUND : event.getClientId());
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    @Override
    public void close() {
        // unused
    }
}
