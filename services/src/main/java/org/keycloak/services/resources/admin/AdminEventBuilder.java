package org.keycloak.services.resources.admin;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.ClientConnection;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AuthDetails;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.Time;

public class AdminEventBuilder {
    
    private static final Logger log = Logger.getLogger(AdminEventBuilder.class);

    private EventStoreProvider store;
    private List<EventListenerProvider> listeners;
    private RealmModel realm;
    private AdminEvent adminEvent;

    public AdminEventBuilder(RealmModel realm, AdminAuth auth, KeycloakSession session, ClientConnection clientConnection) {
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

        authRealm(auth.getRealm());
        authClient(auth.getClient());
        authUser(auth.getUser());
        authIpAddress(clientConnection.getRemoteAddr());
    }
    
    public AdminEventBuilder realm(RealmModel realm) {
        adminEvent.setRealmId(realm.getId());
        return this;
    }
    
    public AdminEventBuilder realm(String realmId) {
        adminEvent.setRealmId(realmId);
        return this;
    }
    
    public AdminEventBuilder operation(OperationType e) {
        adminEvent.setOperationType(e);
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
        adminEvent.setAuthDetails(authDetails);
        return this;
    }

    public AdminEventBuilder authRealm(String realmId) {
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

    public AdminEventBuilder authClient(String clientId) {
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

    public AdminEventBuilder authUser(String userId) {
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
    
    public AdminEventBuilder resourcePath(String resourcePath) {
        adminEvent.setResourcePath(resourcePath);
        return this;
    }
    
    public AdminEventBuilder resourcePath(String resourcePath, boolean segment) {
        if(segment) {
            int index = resourcePath.lastIndexOf('/');
            int subIndex = resourcePath.lastIndexOf('/', index - 1);
            adminEvent.setResourcePath(resourcePath.substring(subIndex));
        } else {
            adminEvent.setResourcePath(resourcePath.substring(resourcePath.lastIndexOf('/')));
        }
        return this;
    }
    
    public AdminEventBuilder resourcePath(Object model) {
        StringBuilder sb = new StringBuilder();
        sb.append(getResourcePath(model));
        adminEvent.setResourcePath(sb.toString());
        return this;
    }
    
    public AdminEventBuilder resourcePath(Object model, String resourcePath) {
        StringBuilder sb = new StringBuilder();
        sb.append(getResourcePath(model));
        sb.append(resourcePath.substring(resourcePath.lastIndexOf('/')));
        adminEvent.setResourcePath(sb.toString());
        return this;
    }
    
    public AdminEventBuilder resourcePath(Object model, String resourcePath, boolean segment) {
        StringBuilder sb = new StringBuilder();
        sb.append(getResourcePath(model));
        int index = resourcePath.lastIndexOf('/');
        int subIndex = resourcePath.lastIndexOf('/', index - 1);
        sb.append(resourcePath.substring(subIndex));
        adminEvent.setResourcePath(sb.toString());
        return this;
    }
    
    public AdminEventBuilder resourcePath(Object model, Object subModel, String resourcePath) {
        StringBuilder sb = new StringBuilder();
        sb.append(getResourcePath(model));
        int index = resourcePath.lastIndexOf('/');
        int subIndex = resourcePath.lastIndexOf('/', index - 1);
        sb.append(resourcePath.substring(subIndex, index+1));
        sb.append(getResourcePath(subModel));
        adminEvent.setResourcePath(sb.toString());
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
            try {
                store.onEvent(adminEvent, includeRepresentation);
            } catch (Throwable t) {
                log.error("Failed to save event", t);
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
    
    private String getResourcePath(Object model) {

        StringBuilder sb = new StringBuilder();

        if (model instanceof RealmModel) {
            RealmModel realm = (RealmModel) model;
            sb.append("realms/" + realm.getId());
        } else if (model instanceof ClientModel) {
            ClientModel client = (ClientModel) model;
            sb.append("clients/" + client.getId());
        } else if (model instanceof UserModel) {
            UserModel user = (UserModel) model;
            sb.append("users/" + user.getId());

        } else if (model instanceof IdentityProviderModel) {
            IdentityProviderModel provider = (IdentityProviderModel) model;
            sb.append("identity-Providers/" + provider.getProviderId());
        } else if (model instanceof IdentityProviderRepresentation) {
            IdentityProviderRepresentation provider = (IdentityProviderRepresentation) model;
            sb.append("identity-Providers/" + provider.getProviderId());
        } else if (model instanceof IdentityProviderMapperModel) {
            IdentityProviderMapperModel provider = (IdentityProviderMapperModel) model;
            sb.append("identity-Provider-Mappers/" + provider.getId());
        } else if (model instanceof IdentityProviderFactory) {
            IdentityProviderFactory provider = (IdentityProviderFactory) model;
            sb.append("identity-Provider-Factory/" + provider.getId());

        } else if (model instanceof ProtocolMapperModel) {
            ProtocolMapperModel mapper = (ProtocolMapperModel) model;
            sb.append("protocol-Mappers/" + mapper.getId());

        } else if (model instanceof UserFederationProviderModel) {
            UserFederationProviderModel provider = (UserFederationProviderModel) model;
            sb.append("user-Federation-Providers/" + provider.getId());
        
        } else if (model instanceof RoleModel) {
            RoleModel role = (RoleModel) model;
            sb.append("roles/" + role.getId());
        }

        return sb.toString();
    }
}
