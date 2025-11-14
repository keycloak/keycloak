/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.services.resources.admin;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.core.UriInfo;

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
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;

import static org.keycloak.models.utils.StripSecretsUtils.stripSecrets;

public class AdminEventBuilder {

    protected static final Logger logger = Logger.getLogger(AdminEventBuilder.class);
    private final AdminAuth auth;
    private final String ipAddress;
    private final RealmModel realm;
    private final AdminEvent adminEvent;
    private final Map<String, EventListenerProvider> listeners;
    private final KeycloakSession session;

    private EventStoreProvider store;

    public AdminEventBuilder(RealmModel realm, AdminAuth auth, KeycloakSession session, ClientConnection clientConnection) {
        this(realm, auth, session, clientConnection.getRemoteHost(), null);
    }

    private AdminEventBuilder(RealmModel realm, AdminAuth auth, KeycloakSession session, String ipAddress, AdminEvent adminEvent) {
        this.realm = realm;
        this.listeners = new HashMap<>();
        updateStore(session);
        addListeners(session);
        this.auth = auth;
        this.ipAddress = ipAddress;
        if (adminEvent != null) {
            this.adminEvent = new AdminEvent(adminEvent);
        } else {
            this.adminEvent = new AdminEvent();
            // Assumption: the following methods write information to the adminEvent only
            realm(realm);
            authRealm(auth.getRealm());
            authClient(auth.getClient());
            authUser(auth.getUser());
            authIpAddress(ipAddress);
        }
        this.session = session;
    }

    /**
     * Create a new instance of the {@link AdminEventBuilder} that is bound to a new session.
     * Use this when starting, for example, a nested transaction.
     * @param session new session where the {@link AdminEventBuilder} should be bound to.
     * @return a new instance of {@link AdminEventBuilder}
     */
    public AdminEventBuilder clone(KeycloakSession session) {
        RealmModel newEventRealm = session.realms().getRealm(realm.getId());
        RealmModel newAuthRealm = session.realms().getRealm(this.auth.getRealm().getId());
        UserModel newAuthUser = session.users().getUserById(newAuthRealm, this.auth.getUser().getId());
        ClientModel newAuthClient = session.clients().getClientById(newAuthRealm, this.auth.getClient().getId());

        return new AdminEventBuilder(
                newEventRealm,
                new AdminAuth(newAuthRealm, this.auth.getToken(), newAuthUser, newAuthClient),
                session,
                ipAddress,
                adminEvent
        );
    }

    public AdminEventBuilder realm(RealmModel realm) {
        adminEvent.setRealmId(realm.getId());
        adminEvent.setRealmName(realm.getName());
        return this;
    }

    /**
     * Refreshes the builder assuming that the realm event information has
     * changed. Thought to be used when the updateRealmEventsConfig has
     * modified the events configuration. Now the store and the listeners are
     * updated to have previous and new setup.
     * @param session The session
     * @return The same builder
     */
    public AdminEventBuilder refreshRealmEventsConfig(KeycloakSession session) {
        return this.updateStore(session).addListeners(session);
    }

    private AdminEventBuilder updateStore(KeycloakSession session) {
        if (realm.isAdminEventsEnabled() && store == null) {
            this.store = session.getProvider(EventStoreProvider.class);
            if (store == null) {
                ServicesLogger.LOGGER.noEventStoreProvider();
            }
        }
        return this;
    }

    private AdminEventBuilder addListeners(KeycloakSession session) {
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

    public AdminEventBuilder operation(OperationType operationType) {
        adminEvent.setOperationType(operationType);
        return this;
    }

    public AdminEventBuilder resource(ResourceType resourceType){
        adminEvent.setResourceType(resourceType);
        return this;
    }

    /**
     * Setter for custom resource types with values different from {@link ResourceType}.
     */
    public AdminEventBuilder resource(String resourceType){
        adminEvent.setResourceTypeAsString(resourceType);
        return this;
    }

    public AdminEventBuilder authRealm(RealmModel realm) {
        AuthDetails authDetails = adminEvent.getAuthDetails();
        if(authDetails == null) {
            authDetails =  new AuthDetails();
            authDetails.setRealmId(realm.getId());
        } else {
            authDetails.setRealmId(realm.getId());
        }
        authDetails.setRealmName(realm.getName());
        adminEvent.setAuthDetails(authDetails);
        return this;
    }

    public AdminEventBuilder authClient(ClientModel client) {
        AuthDetails authDetails = adminEvent.getAuthDetails();
        if(authDetails == null) {
            authDetails =  new AuthDetails();
            authDetails.setClientId(client.getId());
        } else {
            authDetails.setClientId(client.getId());
        }
        adminEvent.setAuthDetails(authDetails);
        return this;
    }

    public AdminEventBuilder authUser(UserModel user) {
        AuthDetails authDetails = adminEvent.getAuthDetails();
        if(authDetails == null) {
            authDetails =  new AuthDetails();
            authDetails.setUserId(user.getId());
        } else {
            authDetails.setUserId(user.getId());
        }
        adminEvent.setAuthDetails(authDetails);
        return this;
    }

    public AdminEventBuilder authIpAddress(String ipAddress) {
        AuthDetails authDetails = adminEvent.getAuthDetails();
        if(authDetails == null) {
            authDetails =  new AuthDetails();
            authDetails.setIpAddress(ipAddress);
        } else {
            authDetails.setIpAddress(ipAddress);
        }
        adminEvent.setAuthDetails(authDetails);
        return this;
    }

    public AdminEventBuilder resourcePath(String... pathElements) {
        StringBuilder sb = new StringBuilder();
        for (String element : pathElements) {
            sb.append("/");
            sb.append(element);
        }
        if (pathElements.length > 0) sb.deleteCharAt(0); // remove leading '/'

        adminEvent.setResourcePath(sb.toString());
        return this;
    }

    public AdminEventBuilder resourcePath(UriInfo uriInfo) {
        String path = getResourcePath(uriInfo);
        adminEvent.setResourcePath(path);
        return this;
    }

    public AdminEventBuilder resourcePath(UriInfo uriInfo, String id) {
        StringBuilder sb = new StringBuilder();
        sb.append(getResourcePath(uriInfo));
        sb.append("/");
        sb.append(id);
        adminEvent.setResourcePath(sb.toString());
        return this;
    }

    private String getResourcePath(UriInfo uriInfo) {
        String path = uriInfo.getPath();

        StringBuilder sb = new StringBuilder();
        sb.append("/realms/");
        sb.append(realm.getName());
        sb.append("/");
        String realmRelative = sb.toString();

        return path.substring(path.indexOf(realmRelative) + realmRelative.length());
    }

    public AdminEventBuilder representation(Object value) {
        if (value == null || value.equals("")) {
            return this;
        }

        stripSecrets(session, value);

        try {
            adminEvent.setRepresentation(JsonSerialization.writeValueAsString(value));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public AdminEventBuilder detail(String key, String value) {
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
        boolean includeRepresentation = realm.isAdminEventsDetailsEnabled();

        // Event needs to be copied because the same builder can be used with another event
        AdminEvent eventCopy = new AdminEvent(adminEvent);
        eventCopy.setTime(Time.currentTimeMillis());
        eventCopy.setId(UUID.randomUUID().toString());

        if (store != null) {
            store.onEvent(eventCopy, includeRepresentation);
        }

        if (listeners != null) {
            for (EventListenerProvider l : listeners.values()) {
                try {
                    l.onEvent(eventCopy, includeRepresentation);
                } catch (Throwable t) {
                    ServicesLogger.LOGGER.failedToSendType(t, l);
                }
            }
        }
    }

}
