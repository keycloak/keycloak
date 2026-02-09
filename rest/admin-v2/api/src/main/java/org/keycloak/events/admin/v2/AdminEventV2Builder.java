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
package org.keycloak.events.admin.v2;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Time;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AuthDetails;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;

/**
 * Builder for Admin API v2 events.
 * <p>
 * Similar to the v1 AdminEventBuilder but designed to work with v2 representations.
 * Events are dispatched through the same EventListenerProvider mechanism as v1,
 * but with an "apiVersion" detail set to "v2" to distinguish them from v1 events.
 */
public class AdminEventV2Builder {

    private static final Logger logger = Logger.getLogger(AdminEventV2Builder.class);
    private static final String API_VERSION_DETAIL_KEY = "apiVersion";
    private static final String API_VERSION_V2 = "v2";

    private final AdminAuth auth;
    private final String ipAddress;
    private final RealmModel realm;
    private final AdminEvent adminEvent;
    private final Map<String, EventListenerProvider> listeners;
    private final KeycloakSession session;

    public AdminEventV2Builder(RealmModel realm, AdminAuth auth, KeycloakSession session, ClientConnection clientConnection) {
        this(realm, auth, session, clientConnection.getRemoteHost(), null);
    }

    private AdminEventV2Builder(RealmModel realm, AdminAuth auth, KeycloakSession session, String ipAddress, AdminEvent adminEvent) {
        this.realm = realm;
        this.listeners = new HashMap<>();
        this.auth = auth;
        this.ipAddress = ipAddress;
        this.session = session;

        // Note: We don't call updateStore() here anymore - we'll get the store at send time
        // to ensure we have the latest realm config
        addListeners(session);

        if (adminEvent != null) {
            this.adminEvent = new AdminEvent(adminEvent);
        } else {
            this.adminEvent = new AdminEvent();
            // Initialize event with realm and auth details
            realm(realm);
            authRealm(auth.getRealm());
            authClient(auth.getClient());
            authUser(auth.getUser());
            authIpAddress(ipAddress);
            // Mark this as a v2 API event
            detail(API_VERSION_DETAIL_KEY, API_VERSION_V2);
        }
    }

    /**
     * Create a new instance of the {@link AdminEventV2Builder} that is bound to a new session.
     * Use this when starting, for example, a nested transaction.
     * @param session new session where the {@link AdminEventV2Builder} should be bound to.
     * @return a new instance of {@link AdminEventV2Builder}
     */
    public AdminEventV2Builder clone(KeycloakSession session) {
        RealmModel newEventRealm = session.realms().getRealm(realm.getId());
        RealmModel newAuthRealm = session.realms().getRealm(this.auth.getRealm().getId());
        UserModel newAuthUser = session.users().getUserById(newAuthRealm, this.auth.getUser().getId());
        ClientModel newAuthClient = session.clients().getClientById(newAuthRealm, this.auth.getClient().getId());

        return new AdminEventV2Builder(
                newEventRealm,
                new AdminAuth(newAuthRealm, this.auth.getToken(), newAuthUser, newAuthClient),
                session,
                ipAddress,
                adminEvent
        );
    }

    public AdminEventV2Builder realm(RealmModel realm) {
        adminEvent.setRealmId(realm.getId());
        adminEvent.setRealmName(realm.getName());
        return this;
    }

    /**
     * Refreshes the builder assuming that the realm event information has changed.
     * Used when the updateRealmEventsConfig has modified the events configuration.
     * @param session The session
     * @return The same builder
     */
    public AdminEventV2Builder refreshRealmEventsConfig(KeycloakSession session) {
        return this.addListeners(session);
    }

    private AdminEventV2Builder addListeners(KeycloakSession session) {
        HashSet<String> realmListeners = new HashSet<>(realm.getEventsListenersStream().toList());
        session.getKeycloakSessionFactory().getProviderFactoriesStream(EventListenerProvider.class)
                .filter(providerFactory -> realmListeners.contains(providerFactory.getId()) || ((EventListenerProviderFactory) providerFactory).isGlobal())
                .forEach(providerFactory -> {
                    realmListeners.remove(providerFactory.getId());
                    if (!listeners.containsKey(providerFactory.getId())) {
                        listeners.put(providerFactory.getId(), ((EventListenerProviderFactory) providerFactory).create(session));
                    }
                });
        realmListeners.forEach(ServicesLogger.LOGGER::providerNotFound);
        return this;
    }

    public AdminEventV2Builder operation(OperationType operationType) {
        adminEvent.setOperationType(operationType);
        return this;
    }

    public AdminEventV2Builder resource(ResourceType resourceType) {
        adminEvent.setResourceType(resourceType);
        return this;
    }

    /**
     * Setter for custom resource types with values different from {@link ResourceType}.
     */
    public AdminEventV2Builder resource(String resourceType) {
        adminEvent.setResourceTypeAsString(resourceType);
        return this;
    }

    public AdminEventV2Builder authRealm(RealmModel realm) {
        AuthDetails authDetails = adminEvent.getAuthDetails();
        if (authDetails == null) {
            authDetails = new AuthDetails();
        }
        authDetails.setRealmId(realm.getId());
        authDetails.setRealmName(realm.getName());
        adminEvent.setAuthDetails(authDetails);
        return this;
    }

    public AdminEventV2Builder authClient(ClientModel client) {
        AuthDetails authDetails = adminEvent.getAuthDetails();
        if (authDetails == null) {
            authDetails = new AuthDetails();
        }
        authDetails.setClientId(client.getId());
        adminEvent.setAuthDetails(authDetails);
        return this;
    }

    public AdminEventV2Builder authUser(UserModel user) {
        AuthDetails authDetails = adminEvent.getAuthDetails();
        if (authDetails == null) {
            authDetails = new AuthDetails();
        }
        authDetails.setUserId(user.getId());
        adminEvent.setAuthDetails(authDetails);
        return this;
    }

    public AdminEventV2Builder authIpAddress(String ipAddress) {
        AuthDetails authDetails = adminEvent.getAuthDetails();
        if (authDetails == null) {
            authDetails = new AuthDetails();
        }
        authDetails.setIpAddress(ipAddress);
        adminEvent.setAuthDetails(authDetails);
        return this;
    }

    public AdminEventV2Builder resourcePath(String... pathElements) {
        StringBuilder sb = new StringBuilder();
        for (String element : pathElements) {
            sb.append("/");
            sb.append(element);
        }
        if (pathElements.length > 0) {
            sb.deleteCharAt(0); // remove leading '/'
        }
        adminEvent.setResourcePath(sb.toString());
        return this;
    }

    /**
     * Sets the v2 representation to be included in the event.
     * The representation is serialized to JSON.
     * 
     * @param value the v2 representation object (e.g., BaseClientRepresentation)
     * @return this builder
     */
    public AdminEventV2Builder representation(Object value) {
        if (value == null || value.equals("")) {
            return this;
        }

        try {
            adminEvent.setRepresentation(JsonSerialization.writeValueAsString(value));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public AdminEventV2Builder detail(String key, String value) {
        if (StringUtil.isBlank(value)) {
            return this;
        }

        if (adminEvent.getDetails() == null) {
            adminEvent.setDetails(new HashMap<>());
        }

        adminEvent.getDetails().put(key, value);

        return this;
    }

    public AdminEvent getEvent() {
        return adminEvent;
    }

    public void success() {
        send();
    }

    private void send() {
        // Refresh realm to get latest admin events config
        RealmModel currentRealm = session.realms().getRealm(realm.getId());
        if (currentRealm == null) {
            logger.warnf("Could not refresh realm %s, using original realm model", realm.getId());
            currentRealm = realm;
        }
        
        boolean adminEventsEnabled = currentRealm.isAdminEventsEnabled();
        boolean includeRepresentation = currentRealm.isAdminEventsDetailsEnabled();

        logger.debugf("Sending v2 admin event for realm %s, adminEventsEnabled=%s, includeRepresentation=%s", 
                currentRealm.getName(), adminEventsEnabled, includeRepresentation);

        // Event needs to be copied because the same builder can be used with another event
        AdminEvent eventCopy = new AdminEvent(adminEvent);
        eventCopy.setTime(Time.currentTimeMillis());
        eventCopy.setId(UUID.randomUUID().toString());

        // Store the event if admin events are enabled
        if (adminEventsEnabled) {
            EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
            if (eventStore != null) {
                logger.debugf("Storing v2 admin event with id %s, details: %s", eventCopy.getId(), eventCopy.getDetails());
                eventStore.onEvent(eventCopy, includeRepresentation);
            } else {
                logger.warn("No EventStoreProvider available, v2 admin event not stored");
            }
        } else {
            logger.debugf("Admin events not enabled for realm %s, skipping store", currentRealm.getName());
        }

        // Send to listeners
        for (EventListenerProvider l : listeners.values()) {
            try {
                l.onEvent(eventCopy, includeRepresentation);
            } catch (Throwable t) {
                ServicesLogger.LOGGER.failedToSendType(t, l);
            }
        }
    }
}
