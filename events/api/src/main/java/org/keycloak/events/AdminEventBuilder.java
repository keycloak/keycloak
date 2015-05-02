package org.keycloak.events;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.ClientConnection;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AuthDetails;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.Time;

public class AdminEventBuilder {
    
    private static final Logger log = Logger.getLogger(AdminEventBuilder.class);

    private EventStoreProvider store;
    private List<EventListenerProvider> listeners;
    private RealmModel realm;
    private AdminEvent adminEvent;

    public AdminEventBuilder(RealmModel realm, KeycloakSession session, ClientConnection clientConnection) {
        this.realm = realm;

        adminEvent = new AdminEvent();

        if (realm.isAdminEventsEnabled()) {
            EventStoreProvider store = session.getProvider(EventStoreProvider.class);
            if (store != null) {
                this.store = store;
            } else {
                log.error("Admin Events enabled, but no event store provider configured");
            }
        }

        if (realm.getEventsListeners() != null && !realm.getEventsListeners().isEmpty()) {
            this.listeners = new LinkedList<>();
            for (String id : realm.getEventsListeners()) {
                EventListenerProvider listener = session.getProvider(EventListenerProvider.class, id);
                if (listener != null) {
                    listeners.add(listener);
                } else {
                    log.error("Event listener '" + id + "' registered, but provider not found");
                }
            }
        }

        realm(realm);
        ipAddress(clientConnection.getRemoteAddr());
    }
    
    public AdminEventBuilder operation(OperationType e) {
        adminEvent.setOperationType(e);
        return this;
    }

    public AdminEventBuilder realm(RealmModel realm) {
        AuthDetails authDetails = adminEvent.getAuthDetails();
        if(authDetails == null) {
            authDetails =  new AuthDetails();
            authDetails.setRealmId(realm.getId());
        } else {
            authDetails.setRealmId(realm.getId());
        }
        adminEvent.setAuthDetails(authDetails);
        return this;
    }

    public AdminEventBuilder realm(String realmId) {
        AuthDetails authDetails = adminEvent.getAuthDetails();
        if(authDetails == null) {
            authDetails =  new AuthDetails();
            authDetails.setRealmId(realmId);
        } else {
            authDetails.setRealmId(realmId);
        }
        adminEvent.setAuthDetails(authDetails);
        return this;
    }

    public AdminEventBuilder client(ClientModel client) {
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

    public AdminEventBuilder client(String clientId) {
        AuthDetails authDetails = adminEvent.getAuthDetails();
        if(authDetails == null) {
            authDetails =  new AuthDetails();
            authDetails.setClientId(clientId);
        } else {
            authDetails.setClientId(clientId);
        }
        adminEvent.setAuthDetails(authDetails);
        return this;
    }

    public AdminEventBuilder user(UserModel user) {
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

    public AdminEventBuilder user(String userId) {
        AuthDetails authDetails = adminEvent.getAuthDetails();
        if(authDetails == null) {
            authDetails =  new AuthDetails();
            authDetails.setUserId(userId);
        } else {
            authDetails.setUserId(userId);
        }
        adminEvent.setAuthDetails(authDetails);
        return this;
    }

    public AdminEventBuilder ipAddress(String ipAddress) {
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
    
    public AdminEventBuilder resourcePath(String resourcePath) {
        adminEvent.setResourcePath(resourcePath);
        return this;
    }

    public void error(String error) {
        adminEvent.setOperationType(OperationType.valueOf(adminEvent.getOperationType().name() + "_ERROR"));
        adminEvent.setError(error);
        send();
    }
    
    public AdminEventBuilder representation(Object value) {
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
    
    public AdminEvent getEvent() {
        return adminEvent;
    }

    public void success() {
        send();
    }

    private void send() {
        boolean includeRepresentation = false;
        if(realm.isAdminEventsDetailsEnabled()) {
            includeRepresentation = true;
        }
        adminEvent.setTime(Time.toMillis(Time.currentTime()));

        if (store != null) {
            if (realm.getAdminEnabledEventOperations() != null && !realm.getAdminEnabledEventOperations().isEmpty() ? realm.getAdminEnabledEventOperations().contains(adminEvent.getOperationType().name()) : adminEvent.getOperationType().isSaveByDefault()) {
                try {
                    store.onEvent(adminEvent, includeRepresentation);
                } catch (Throwable t) {
                    log.error("Failed to save event", t);
                }
            }
        }
        
        if (listeners != null) {
            for (EventListenerProvider l : listeners) {
                try {
                    l.onEvent(adminEvent, includeRepresentation);
                } catch (Throwable t) {
                    log.error("Failed to send type to " + l, t);
                }
            }
        }
    }
}
