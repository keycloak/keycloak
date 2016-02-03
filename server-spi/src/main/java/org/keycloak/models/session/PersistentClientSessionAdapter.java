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

package org.keycloak.models.session;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PersistentClientSessionAdapter implements ClientSessionModel {

    private final PersistentClientSessionModel model;
    private final RealmModel realm;
    private final ClientModel client;
    private UserSessionModel userSession;

    private PersistentClientSessionData data;

    public PersistentClientSessionAdapter(ClientSessionModel clientSession) {
        data = new PersistentClientSessionData();
        data.setAction(clientSession.getAction());
        data.setAuthMethod(clientSession.getAuthMethod());
        data.setExecutionStatus(clientSession.getExecutionStatus());
        data.setNotes(clientSession.getNotes());
        data.setProtocolMappers(clientSession.getProtocolMappers());
        data.setRedirectUri(clientSession.getRedirectUri());
        data.setRoles(clientSession.getRoles());
        data.setUserSessionNotes(clientSession.getUserSessionNotes());

        model = new PersistentClientSessionModel();
        model.setClientId(clientSession.getClient().getId());
        model.setClientSessionId(clientSession.getId());
        if (clientSession.getAuthenticatedUser() != null) {
            model.setUserId(clientSession.getAuthenticatedUser().getId());
        }
        model.setUserSessionId(clientSession.getUserSession().getId());
        model.setTimestamp(clientSession.getTimestamp());

        realm = clientSession.getRealm();
        client = clientSession.getClient();
        userSession = clientSession.getUserSession();
    }

    public PersistentClientSessionAdapter(PersistentClientSessionModel model, RealmModel realm, ClientModel client, UserSessionModel userSession) {
        this.model = model;
        this.realm = realm;
        this.client = client;
        this.userSession = userSession;
    }

    // Lazily init data
    private PersistentClientSessionData getData() {
        if (data == null) {
            try {
                data = JsonSerialization.readValue(model.getData(), PersistentClientSessionData.class);
            } catch (IOException ioe) {
                throw new ModelException(ioe);
            }
        }

        return data;
    }

    // Write updated model with latest serialized data
    public PersistentClientSessionModel getUpdatedModel() {
        try {
            String updatedData = JsonSerialization.writeValueAsString(getData());
            this.model.setData(updatedData);
        } catch (IOException ioe) {
            throw new ModelException(ioe);
        }

        return this.model;
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
        this.userSession = userSession;
    }

    @Override
    public String getRedirectUri() {
        return getData().getRedirectUri();
    }

    @Override
    public void setRedirectUri(String uri) {
        getData().setRedirectUri(uri);
    }

    @Override
    public int getTimestamp() {
        return model.getTimestamp();
    }

    @Override
    public void setTimestamp(int timestamp) {
        model.setTimestamp(timestamp);
    }

    @Override
    public String getAction() {
        return getData().getAction();
    }

    @Override
    public void setAction(String action) {
        getData().setAction(action);
    }

    @Override
    public Set<String> getRoles() {
        return getData().getRoles();
    }

    @Override
    public void setRoles(Set<String> roles) {
        getData().setRoles(roles);
    }

    @Override
    public Set<String> getProtocolMappers() {
        return getData().getProtocolMappers();
    }

    @Override
    public void setProtocolMappers(Set<String> protocolMappers) {
        getData().setProtocolMappers(protocolMappers);
    }

    @Override
    public Map<String, ExecutionStatus> getExecutionStatus() {
        return getData().getExecutionStatus();
    }

    @Override
    public void setExecutionStatus(String authenticator, ExecutionStatus status) {
        getData().getExecutionStatus().put(authenticator, status);
    }

    @Override
    public void clearExecutionStatus() {
        getData().getExecutionStatus().clear();
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
        getData().setAuthMethod(method);
    }

    @Override
    public String getNote(String name) {
        PersistentClientSessionData entity = getData();
        return entity.getNotes()==null ? null : entity.getNotes().get(name);
    }

    @Override
    public void setNote(String name, String value) {
        PersistentClientSessionData entity = getData();
        if (entity.getNotes() == null) {
            entity.setNotes(new HashMap<String, String>());
        }
        entity.getNotes().put(name, value);
    }

    @Override
    public void removeNote(String name) {
        PersistentClientSessionData entity = getData();
        if (entity.getNotes() != null) {
            entity.getNotes().remove(name);
        }
    }

    @Override
    public Map<String, String> getNotes() {
        PersistentClientSessionData entity = getData();
        if (entity.getNotes() == null || entity.getNotes().isEmpty()) return Collections.emptyMap();
        return entity.getNotes();
    }

    @Override
    public Set<String> getRequiredActions() {
        return getData().getRequiredActions();
    }

    @Override
    public void addRequiredAction(String action) {
        getData().getRequiredActions().add(action);
    }

    @Override
    public void removeRequiredAction(String action) {
        getData().getRequiredActions().remove(action);
    }

    @Override
    public void addRequiredAction(UserModel.RequiredAction action) {
        addRequiredAction(action.name());
    }

    @Override
    public void removeRequiredAction(UserModel.RequiredAction action) {
        removeRequiredAction(action.name());
    }

    @Override
    public void setUserSessionNote(String name, String value) {
        PersistentClientSessionData entity = getData();
        if (entity.getUserSessionNotes() == null) {
            entity.setUserSessionNotes(new HashMap<String, String>());
        }
        entity.getUserSessionNotes().put(name, value);
    }

    @Override
    public Map<String, String> getUserSessionNotes() {
        PersistentClientSessionData entity = getData();
        if (entity.getUserSessionNotes() == null || entity.getUserSessionNotes().isEmpty()) return Collections.emptyMap();
        return entity.getUserSessionNotes();
    }

    @Override
    public void clearUserSessionNotes() {
        PersistentClientSessionData entity = getData();
        entity.setUserSessionNotes(new HashMap<String, String>());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof ClientSessionModel)) return false;

        ClientSessionModel that = (ClientSessionModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    protected static class PersistentClientSessionData {

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

        @JsonProperty("userSessionNotes")
        private Map<String, String> userSessionNotes;

        @JsonProperty("executionStatus")
        private Map<String, ClientSessionModel.ExecutionStatus> executionStatus = new HashMap<>();

        @JsonProperty("action")
        private String action;

        @JsonProperty("requiredActions")
        private Set<String> requiredActions = new HashSet<>();

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

        public Map<String, String> getUserSessionNotes() {
            return userSessionNotes;
        }

        public void setUserSessionNotes(Map<String, String> userSessionNotes) {
            this.userSessionNotes = userSessionNotes;
        }

        public Map<String, ClientSessionModel.ExecutionStatus> getExecutionStatus() {
            return executionStatus;
        }

        public void setExecutionStatus(Map<String, ClientSessionModel.ExecutionStatus> executionStatus) {
            this.executionStatus = executionStatus;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public Set<String> getRequiredActions() {
            return requiredActions;
        }

        public void setRequiredActions(Set<String> requiredActions) {
            this.requiredActions = requiredActions;
        }
    }
}
