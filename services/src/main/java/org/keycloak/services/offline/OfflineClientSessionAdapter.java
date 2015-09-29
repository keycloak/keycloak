package org.keycloak.services.offline;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonProperty;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.ModelException;
import org.keycloak.models.OfflineClientSessionModel;
import org.keycloak.models.OfflineUserSessionModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OfflineClientSessionAdapter implements ClientSessionModel {

    private final OfflineClientSessionModel model;
    private final RealmModel realm;
    private final ClientModel client;
    private final OfflineUserSessionAdapter userSession;

    private OfflineClientSessionData data;

    public OfflineClientSessionAdapter(OfflineClientSessionModel model, RealmModel realm, ClientModel client, OfflineUserSessionAdapter userSession) {
        this.model = model;
        this.realm = realm;
        this.client = client;
        this.userSession = userSession;
    }

    // lazily init representation
    private OfflineClientSessionData getData() {
        if (data == null) {
            try {
                data = JsonSerialization.readValue(model.getData(), OfflineClientSessionData.class);
            } catch (IOException ioe) {
                throw new ModelException(ioe);
            }
        }

        return data;
    }

    @Override
    public String getId() {
        return model.getClientSessionId();
    }

    @Override
    public RealmModel getRealm() {
        return realm;
    }

    @Override
    public ClientModel getClient() {
        return client;
    }

    @Override
    public UserSessionModel getUserSession() {
        return userSession;
    }

    @Override
    public void setUserSession(UserSessionModel userSession) {
        throw new IllegalStateException("Not supported setUserSession");
    }

    @Override
    public String getRedirectUri() {
        return getData().getRedirectUri();
    }

    @Override
    public void setRedirectUri(String uri) {
        throw new IllegalStateException("Not supported setRedirectUri");
    }

    @Override
    public int getTimestamp() {
        return getData().getTimestamp();
    }

    @Override
    public void setTimestamp(int timestamp) {
        throw new IllegalStateException("Not supported setTimestamp");
    }

    @Override
    public String getAction() {
        return null;
    }

    @Override
    public void setAction(String action) {
        throw new IllegalStateException("Not supported setAction");
    }

    @Override
    public Set<String> getRoles() {
        return getData().getRoles();
    }

    @Override
    public void setRoles(Set<String> roles) {
        throw new IllegalStateException("Not supported setRoles");
    }

    @Override
    public Set<String> getProtocolMappers() {
        return getData().getProtocolMappers();
    }

    @Override
    public void setProtocolMappers(Set<String> protocolMappers) {
        throw new IllegalStateException("Not supported setProtocolMappers");
    }

    @Override
    public Map<String, ExecutionStatus> getExecutionStatus() {
        return getData().getAuthenticatorStatus();
    }

    @Override
    public void setExecutionStatus(String authenticator, ExecutionStatus status) {
        throw new IllegalStateException("Not supported setExecutionStatus");
    }

    @Override
    public void clearExecutionStatus() {
        throw new IllegalStateException("Not supported clearExecutionStatus");
    }

    @Override
    public UserModel getAuthenticatedUser() {
        return userSession.getUser();
    }

    @Override
    public void setAuthenticatedUser(UserModel user) {
        throw new IllegalStateException("Not supported setAuthenticatedUser");
    }

    @Override
    public String getAuthMethod() {
        return getData().getAuthMethod();
    }

    @Override
    public void setAuthMethod(String method) {
        throw new IllegalStateException("Not supported setAuthMethod");
    }

    @Override
    public String getNote(String name) {
        return getData().getNotes()==null ? null : getData().getNotes().get(name);
    }

    @Override
    public void setNote(String name, String value) {
        throw new IllegalStateException("Not supported setNote");
    }

    @Override
    public void removeNote(String name) {
        throw new IllegalStateException("Not supported removeNote");
    }

    @Override
    public Map<String, String> getNotes() {
        return getData().getNotes();
    }

    @Override
    public Set<String> getRequiredActions() {
        throw new IllegalStateException("Not supported getRequiredActions");
    }

    @Override
    public void addRequiredAction(String action) {
        throw new IllegalStateException("Not supported addRequiredAction");
    }

    @Override
    public void removeRequiredAction(String action) {
        throw new IllegalStateException("Not supported removeRequiredAction");
    }

    @Override
    public void addRequiredAction(UserModel.RequiredAction action) {
        throw new IllegalStateException("Not supported addRequiredAction");
    }

    @Override
    public void removeRequiredAction(UserModel.RequiredAction action) {
        throw new IllegalStateException("Not supported removeRequiredAction");
    }

    @Override
    public void setUserSessionNote(String name, String value) {
        throw new IllegalStateException("Not supported setUserSessionNote");
    }

    @Override
    public Map<String, String> getUserSessionNotes() {
        throw new IllegalStateException("Not supported getUserSessionNotes");
    }

    @Override
    public void clearUserSessionNotes() {
        throw new IllegalStateException("Not supported clearUserSessionNotes");
    }

    protected static class OfflineClientSessionData {

        @JsonProperty("authMethod")
        private String authMethod;

        @JsonProperty("redirectUri")
        private String redirectUri;

        @JsonProperty("protocolMappers")
        private Set<String> protocolMappers;

        @JsonProperty("roles")
        private Set<String> roles;

        @JsonProperty("notes")
        private Map<String, String> notes;

        @JsonProperty("authenticatorStatus")
        private Map<String, ClientSessionModel.ExecutionStatus> authenticatorStatus = new HashMap<>();

        @JsonProperty("timestamp")
        private int timestamp;

        public String getAuthMethod() {
            return authMethod;
        }

        public void setAuthMethod(String authMethod) {
            this.authMethod = authMethod;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }

        public Set<String> getProtocolMappers() {
            return protocolMappers;
        }

        public void setProtocolMappers(Set<String> protocolMappers) {
            this.protocolMappers = protocolMappers;
        }

        public Set<String> getRoles() {
            return roles;
        }

        public void setRoles(Set<String> roles) {
            this.roles = roles;
        }

        public Map<String, String> getNotes() {
            return notes;
        }

        public void setNotes(Map<String, String> notes) {
            this.notes = notes;
        }

        public Map<String, ClientSessionModel.ExecutionStatus> getAuthenticatorStatus() {
            return authenticatorStatus;
        }

        public void setAuthenticatorStatus(Map<String, ClientSessionModel.ExecutionStatus> authenticatorStatus) {
            this.authenticatorStatus = authenticatorStatus;
        }

        public int getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(int timestamp) {
            this.timestamp = timestamp;
        }
    }
}
