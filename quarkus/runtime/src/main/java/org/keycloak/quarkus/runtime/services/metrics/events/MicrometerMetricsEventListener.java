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
import org.keycloak.events.EventListenerTransaction;
import org.keycloak.events.EventType;
import org.keycloak.events.GlobalEventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;

import java.util.Locale;

public class MicrometerMetricsEventListener implements GlobalEventListenerProvider {

    private static final Logger logger = Logger.getLogger(MicrometerMetricsEventListener.class);

    private static final String REALM_TAG = "realm";
    private static final String IDP_TAG = "idp";
    private static final String CLIENT_ID_TAG = "client.id";
    private static final String ERROR_TAG = "error";
    private static final String EVENT_TAG = "event";
    private static final String DESCRIPTION_OF_EVENT_METER = "Keycloak user events";
    private static final String KEYCLOAK_METER_NAME_PREFIX = "keycloak_";
    private static final String USER_EVENTS_METER_NAME = KEYCLOAK_METER_NAME_PREFIX + "user";

    private final boolean withIdp, withRealm, withClientId;

    private final EventListenerTransaction tx =
            new EventListenerTransaction(null, this::countEvent);

    public MicrometerMetricsEventListener(KeycloakSession session, boolean withIdp, boolean withRealm, boolean withClientId) {
        this.withIdp = withIdp;
        this.withRealm = withRealm;
        this.withClientId = withClientId;
        session.getTransactionManager().enlistAfterCompletion(tx);
    }

    @Override
    public void onEvent(Event event) {
        tx.addEvent(event);
    }

    private void countEvent(Event event) {
        logger.debugf("Received user event of type %s in realm %s",
                event.getType().name(), event.getRealmName());

        Counter.Builder counterBuilder = Counter.builder(USER_EVENTS_METER_NAME)
                .description(DESCRIPTION_OF_EVENT_METER)
                .tags(Tags.of(Tag.of(EVENT_TAG, format(event.getType())),
                        Tag.of(ERROR_TAG, getError(event))))
                .baseUnit(BaseUnits.EVENTS);

        if (withRealm) {
            counterBuilder.tag(REALM_TAG, nullToEmpty(event.getRealmName()));
        }

        if (withIdp) {
            counterBuilder.tag(IDP_TAG, getIdentityProvider(event));
        }

        if (withClientId) {
            counterBuilder.tag(CLIENT_ID_TAG, getClientId(event));
        }

        counterBuilder.register(Metrics.globalRegistry)
                .increment();
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // do nothing for now
    }

    private String getIdentityProvider(Event event) {
        String identityProvider = null;
        if (event.getDetails() != null) {
            identityProvider = event.getDetails().get(Details.IDENTITY_PROVIDER);
        }
        return nullToEmpty(identityProvider);
    }


    private String getClientId(Event event) {
        // Don't use the clientId as a tag value of the event CLIENT_NOT_FOUND as it would lead to a metrics cardinality explosion
        return nullToEmpty(Errors.CLIENT_NOT_FOUND.equals(event.getError())
                ? "unknown" : event.getClientId());
    }

    private String getError(Event event) {
        String error = event.getError();
        if (error == null && event.getType().name().endsWith("_ERROR")) {
            error = "unknown";
        }
        return nullToEmpty(error);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }


    public static String format(EventType type) {
        // Remove the error suffix so that all events have the same tag.
        // In dashboards, we can distinguish errors from non-errors by looking at the error tag.
        String name = type.name();
        if (name.endsWith("_ERROR")) {
            name = name.substring(0, name.length() - "_ERROR".length());
        }
        return name.toLowerCase(Locale.ROOT);
    }

    @Override
    public void close() {
        // unused
    }
}
