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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import org.jboss.logging.Logger;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerTransaction;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.stream.Collectors;

public class MicrometerMetricsEventListener implements EventListenerProvider {

    private static final Logger logger = Logger.getLogger(MicrometerMetricsEventListener.class);

    private static final EnumSet<EventType> NON_GENERIC_EVENT_TYPES =
            EnumSet.of(EventType.LOGIN, EventType.LOGIN_ERROR,
                    EventType.CLIENT_LOGIN, EventType.CLIENT_LOGIN_ERROR,
                    EventType.REGISTER, EventType.REGISTER_ERROR,
                    EventType.REFRESH_TOKEN, EventType.REFRESH_TOKEN_ERROR,
                    EventType.CODE_TO_TOKEN, EventType.CODE_TO_TOKEN_ERROR);

    private static final String PROVIDER_KEYCLOAK_OPENID = "keycloak";
    private static final String REALM_TAG = "realm";
    private static final String PROVIDER_TAG = "provider";
    private static final String CLIENT_ID_TAG = "client.id";
    private static final String ERROR_TAG = "error";
    private static final String RESOURCE_TAG = "resource";
    private static final String KEYLOAK_METER_NAME_PREFIX = "keycloak.";
    private static final String TOTAL_LOGINS =
            KEYLOAK_METER_NAME_PREFIX + "logins";
    private static final String TOTAL_LOGINS_ATTEMPTS =
            KEYLOAK_METER_NAME_PREFIX + "login.attempts";
    private static final String TOTAL_FAILED_LOGIN_ATTEMPTS =
            KEYLOAK_METER_NAME_PREFIX + "failed.login.attempts";
    private static final String TOTAL_REGISTRATIONS =
            KEYLOAK_METER_NAME_PREFIX + "registrations";
    private static final String TOTAL_REGISTRATIONS_ERRORS =
            KEYLOAK_METER_NAME_PREFIX + "registrations.errors";
    private static final String TOTAL_REFRESH_TOKENS =
            KEYLOAK_METER_NAME_PREFIX + "refresh.tokens";
    private static final String TOTAL_REFRESH_TOKENS_ERRORS =
            KEYLOAK_METER_NAME_PREFIX + "refresh.tokens.errors";
    private static final String TOTAL_CLIENT_LOGINS =
            KEYLOAK_METER_NAME_PREFIX + "client.logins";
    private static final String TOTAL_FAILED_CLIENT_LOGIN_ATTEMPTS =
            KEYLOAK_METER_NAME_PREFIX + "failed.client.login.attempts";
    private static final String TOTAL_CODE_TO_TOKENS =
            KEYLOAK_METER_NAME_PREFIX + "code.to.tokens";
    private static final String TOTAL_CODE_TO_TOKENS_ERRORS =
            KEYLOAK_METER_NAME_PREFIX + "code.to.tokens.errors";
    private static final String USER_EVENT_PREFIX =
            KEYLOAK_METER_NAME_PREFIX + "user.event.";
    private static final String ADMIN_EVENT_PREFIX =
            KEYLOAK_METER_NAME_PREFIX + "admin.event.";

    static final Map<OperationType, String> ADMIN_OPERATION_TYPE_TO_NAME =
            Arrays.stream(OperationType.values())
                    .collect(Collectors.toMap( o -> o, MicrometerMetricsEventListener::buildCounterName));
    static final Map<EventType, String> USER_EVENT_TYPE_TO_NAME =
            Arrays.stream(EventType.values()).filter(o -> !NON_GENERIC_EVENT_TYPES.contains(o))
                    .collect(Collectors.toMap(e -> e, MicrometerMetricsEventListener::buildCounterName));

    private final EventListenerTransaction tx =
            new EventListenerTransaction(this::countRealmResourceTagsFromGenericAdminEvent, this::countEvent);
    private final MeterRegistry meterRegistry = Metrics.globalRegistry;
    private final EnumSet<EventType> includedEvents;
    private final boolean adminEventEnabled;

    public MicrometerMetricsEventListener(KeycloakSession session, EnumSet<EventType> includedEvents,
                                          boolean adminEventEnabled) {
        session.getTransactionManager().enlistAfterCompletion(tx);
        this.includedEvents = includedEvents;
        this.adminEventEnabled = adminEventEnabled;
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
        switch (event.getType()) {
            case LOGIN:
                countLogin(event);
                break;
            case LOGIN_ERROR:
                countLoginError(event);
                break;
            case CLIENT_LOGIN:
                countRealmProviderClientIdTagsFromEvent(TOTAL_CLIENT_LOGINS, event);
                break;
            case CLIENT_LOGIN_ERROR:
                countRealmProviderClientIdErrorTagsFromEvent(TOTAL_FAILED_CLIENT_LOGIN_ATTEMPTS, event);
                break;
            case REGISTER:
                countRealmProviderClientIdTagsFromEvent(TOTAL_REGISTRATIONS, event);
                break;
            case REGISTER_ERROR:
                countRealmProviderClientIdErrorTagsFromEvent(TOTAL_REGISTRATIONS_ERRORS, event);
                break;
            case REFRESH_TOKEN:
                countRealmProviderClientIdTagsFromEvent(TOTAL_REFRESH_TOKENS, event);
                break;
            case REFRESH_TOKEN_ERROR:
                countRealmProviderClientIdErrorTagsFromEvent(TOTAL_REFRESH_TOKENS_ERRORS, event);
                break;
            case CODE_TO_TOKEN:
                countRealmProviderClientIdTagsFromEvent(TOTAL_CODE_TO_TOKENS, event);
                break;
            case CODE_TO_TOKEN_ERROR:
                countRealmProviderClientIdErrorTagsFromEvent(TOTAL_CODE_TO_TOKENS_ERRORS, event);
                break;
            default:
                countRealmTagFromGenericEvent(event);
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        if (adminEventEnabled) {
            tx.addAdminEvent(event, includeRepresentation);
        }
    }

    private void countRealmTagFromGenericEvent(final Event event) {
        meterRegistry.counter(USER_EVENT_TYPE_TO_NAME.get(event.getType()),
                REALM_TAG, nullToEmpty(event.getRealmName())).increment();
    }

    private void countRealmResourceTagsFromGenericAdminEvent(final AdminEvent event, boolean includeRepresentation) {
        logger.debugf("Received admin event of type %s (%s) in realm %s",
                event.getOperationType().name(), event.getResourceType().name(), event.getRealmName());
        meterRegistry.counter(ADMIN_OPERATION_TYPE_TO_NAME.get(event.getOperationType()),
                REALM_TAG, nullToEmpty(event.getRealmName()),
                RESOURCE_TAG, event.getResourceType().name()).increment();
    }

    private void countLogin(final Event event) {
        final String provider = getIdentityProvider(event);
        meterRegistry.counter(TOTAL_LOGINS_ATTEMPTS,
                REALM_TAG, nullToEmpty(event.getRealmName()),
                PROVIDER_TAG, provider,
                CLIENT_ID_TAG, nullToEmpty(event.getClientId())).increment();
        meterRegistry.counter(TOTAL_LOGINS,
                REALM_TAG, nullToEmpty(event.getRealmName()),
                PROVIDER_TAG, provider,
                CLIENT_ID_TAG, nullToEmpty(event.getClientId())).increment();
    }

    private void countLoginError(final Event event) {
        final String provider = getIdentityProvider(event);
        String clientId = getErrorClientId(event);
        meterRegistry.counter(TOTAL_LOGINS_ATTEMPTS,
                REALM_TAG, nullToEmpty(event.getRealmName()),
                PROVIDER_TAG, provider,
                CLIENT_ID_TAG, clientId).increment();
        meterRegistry.counter(TOTAL_FAILED_LOGIN_ATTEMPTS,
                REALM_TAG, nullToEmpty(event.getRealmName()),
                PROVIDER_TAG, provider,
                CLIENT_ID_TAG, clientId,
                ERROR_TAG, nullToEmpty(event.getError())).increment();
    }

    private void countRealmProviderClientIdTagsFromEvent(String name, final Event event) {
        meterRegistry.counter(name,
                REALM_TAG, nullToEmpty(event.getRealmName()),
                PROVIDER_TAG, getIdentityProvider(event),
                CLIENT_ID_TAG, nullToEmpty(event.getClientId())).increment();
    }

    private void countRealmProviderClientIdErrorTagsFromEvent(String name, final Event event) {
        meterRegistry.counter(name,
                REALM_TAG, nullToEmpty(event.getRealmName()),
                PROVIDER_TAG, getIdentityProvider(event),
                CLIENT_ID_TAG, getErrorClientId(event),
                ERROR_TAG, nullToEmpty(event.getError())).increment();
    }

    private String getIdentityProvider(Event event) {
        String identityProvider = null;
        if (event.getDetails() != null) {
            identityProvider = event.getDetails().get(Details.IDENTITY_PROVIDER);
        }
        if (identityProvider == null) {
            identityProvider = PROVIDER_KEYCLOAK_OPENID;
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

    private static String buildCounterName(OperationType type) {
        return ADMIN_EVENT_PREFIX + type.name().toLowerCase().replace("_", ".");
    }

    private static String buildCounterName(EventType type) {
        return USER_EVENT_PREFIX + type.name().toLowerCase().replace("_", ".");
    }

    @Override
    public void close() {
        // unused
    }
}
