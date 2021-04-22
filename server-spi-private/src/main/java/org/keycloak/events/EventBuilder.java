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

package org.keycloak.events;

import org.jboss.logging.Logger;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class EventBuilder {

    private static final Logger log = Logger.getLogger(EventBuilder.class);

    private EventStoreProvider store;
    private List<EventListenerProvider> listeners;
    private RealmModel realm;
    private Event event;

    public EventBuilder(RealmModel realm, KeycloakSession session, ClientConnection clientConnection) {
        this.realm = realm;

        event = new Event();

        if (realm.isEventsEnabled()) {
            EventStoreProvider store = session.getProvider(EventStoreProvider.class);
            if (store != null) {
                this.store = store;
            } else {
                log.error("Events enabled, but no event store provider configured");
            }
        }


        this.listeners = realm.getEventsListenersStream()
                .map(id -> {
                    EventListenerProvider listener = session.getProvider(EventListenerProvider.class, id);
                    if (listener != null) {
                        return listener;
                    } else {
                        log.error("Event listener '" + id + "' registered, but provider not found");
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        realm(realm);
        ipAddress(clientConnection.getRemoteAddr());
    }

    private EventBuilder(EventStoreProvider store, List<EventListenerProvider> listeners, RealmModel realm, Event event) {
        this.store = store;
        this.listeners = listeners;
        this.realm = realm;
        this.event = event;
    }

    public EventBuilder realm(RealmModel realm) {
        event.setRealmId(realm == null ? null : realm.getId());
        return this;
    }

    public EventBuilder realm(String realmId) {
        event.setRealmId(realmId);
        return this;
    }

    public EventBuilder client(ClientModel client) {
        event.setClientId(client == null ? null : client.getClientId());
        return this;
    }

    public EventBuilder client(String clientId) {
        event.setClientId(clientId);
        return this;
    }

    public EventBuilder user(UserModel user) {
        event.setUserId(user == null ? null : user.getId());
        return this;
    }

    public EventBuilder user(String userId) {
        event.setUserId(userId);
        return this;
    }

    public EventBuilder session(UserSessionModel session) {
        event.setSessionId(session == null ? null : session.getId());
        return this;
    }

    public EventBuilder session(String sessionId) {
        event.setSessionId(sessionId);
        return this;
    }

    public EventBuilder ipAddress(String ipAddress) {
        event.setIpAddress(ipAddress);
        return this;
    }

    public EventBuilder event(EventType e) {
        event.setType(e);
        return this;
    }

    public EventBuilder detail(String key, String value) {
        if (value == null || value.equals("")) {
            return this;
        }

        if (event.getDetails() == null) {
            event.setDetails(new HashMap<>());
        }
        event.getDetails().put(key, value);
        return this;
    }

    public EventBuilder removeDetail(String key) {
        if (event.getDetails() != null) {
            event.getDetails().remove(key);
        }
        return this;
    }

    public Event getEvent() {
        return event;
    }

    public void success() {
        send();
    }

    public void error(String error) {
        if (Objects.isNull(event.getType())) {
            throw new IllegalStateException("Attempted to define event error without first setting the event type");
        }

        if (!event.getType().name().endsWith("_ERROR")) {
            event.setType(EventType.valueOf(event.getType().name() + "_ERROR"));
        }
        event.setError(error);
        send();
    }

    public EventBuilder clone() {
        return new EventBuilder(store, listeners, realm, event.clone());
    }

    private void send() {
        event.setTime(Time.currentTimeMillis());
        event.setId(UUID.randomUUID().toString());

        if (store != null) {
            Set<String> eventTypes = realm.getEnabledEventTypesStream().collect(Collectors.toSet());
            if (!eventTypes.isEmpty() ? eventTypes.contains(event.getType().name()) : event.getType().isSaveByDefault()) {
                try {
                    store.onEvent(event);
                } catch (Throwable t) {
                    log.error("Failed to save event", t);
                }
            }
        }

        if (listeners != null) {
            for (EventListenerProvider l : listeners) {
                try {
                    l.onEvent(event);
                } catch (Throwable t) {
                    log.error("Failed to send type to " + l, t);
                }
            }
        }
    }

}
