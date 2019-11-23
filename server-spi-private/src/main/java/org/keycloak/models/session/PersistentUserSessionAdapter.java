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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.OfflineUserSessionModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PersistentUserSessionAdapter implements OfflineUserSessionModel {

    private final PersistentUserSessionModel model;
    private UserModel user;
    private String userId;
    private final RealmModel realm;
    private KeycloakSession session;
    private final Map<String, AuthenticatedClientSessionModel> authenticatedClientSessions;

    private PersistentUserSessionData data;

    public PersistentUserSessionAdapter(UserSessionModel other) {
        this.data = new PersistentUserSessionData();
        data.setAuthMethod(other.getAuthMethod());
        data.setBrokerSessionId(other.getBrokerSessionId());
        data.setBrokerUserId(other.getBrokerUserId());
        data.setIpAddress(other.getIpAddress());
        data.setNotes(other.getNotes());
        data.setRememberMe(other.isRememberMe());
        if (other.getState() != null) {
            data.setState(other.getState().toString());
        }

        this.model = new PersistentUserSessionModel();
        this.model.setStarted(other.getStarted());
        this.model.setUserSessionId(other.getId());
        this.model.setLastSessionRefresh(other.getLastSessionRefresh());

        this.user = other.getUser();
        this.userId = this.user.getId();
        this.realm = other.getRealm();
        this.authenticatedClientSessions = other.getAuthenticatedClientSessions();
    }

    public PersistentUserSessionAdapter(KeycloakSession session, PersistentUserSessionModel model, RealmModel realm, String userId, Map<String, AuthenticatedClientSessionModel> clientSessions) {
        this.session = session;
        this.model = model;
        this.realm = realm;
        this.userId = userId;
        this.authenticatedClientSessions = clientSessions;
    }

    // Lazily init data
    private PersistentUserSessionData getData() {
        if (data == null) {
            try {
                data = JsonSerialization.readValue(model.getData(), PersistentUserSessionData.class);
            } catch (IOException ioe) {
                throw new ModelException(ioe);
            }
        }

        return data;
    }

    // Write updated model with latest serialized data
    public PersistentUserSessionModel getUpdatedModel() {
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
        return model.getUserSessionId();
    }

    @Override
    public String getBrokerSessionId() {
        return getData().getBrokerSessionId();
    }

    @Override
    public String getBrokerUserId() {
        return getData().getBrokerUserId();
    }

    @Override
    public UserModel getUser() {
        if (user == null) {
            user = session.users().getUserById(userId, realm);
        }
        return user;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public RealmModel getRealm() {
        return realm;
    }

    @Override
    public String getLoginUsername() {
        return getUser().getUsername();
    }

    @Override
    public String getIpAddress() {
        return getData().getIpAddress();
    }

    @Override
    public String getAuthMethod() {
        return getData().getAuthMethod();
    }

    @Override
    public boolean isRememberMe() {
        return getData().isRememberMe();
    }

    @Override
    public int getStarted() {
        return model.getStarted();
    }

    @Override
    public int getLastSessionRefresh() {
        return model.getLastSessionRefresh();
    }

    @Override
    public void setLastSessionRefresh(int seconds) {
        model.setLastSessionRefresh(seconds);
    }

    @Override
    public boolean isOffline() {
        return model.isOffline();
    }

    @Override
    public Map<String, AuthenticatedClientSessionModel> getAuthenticatedClientSessions() {
        return authenticatedClientSessions;
    }

    @Override
    public void removeAuthenticatedClientSessions(Collection<String> removedClientUUIDS) {
        if (removedClientUUIDS == null || ! removedClientUUIDS.iterator().hasNext()) {
            return;
        }

        removedClientUUIDS.forEach(authenticatedClientSessions::remove);
    }

    @Override
    public String getNote(String name) {
        return getData().getNotes()==null ? null : getData().getNotes().get(name);
    }

    @Override
    public void setNote(String name, String value) {
        PersistentUserSessionData data = getData();
        if (data.getNotes() == null) {
            data.setNotes(new HashMap<>());
        }
        data.getNotes().put(name, value);

    }

    @Override
    public void removeNote(String name) {
        if (getData().getNotes() != null) {
            getData().getNotes().remove(name);
        }
    }

    @Override
    public Map<String, String> getNotes() {
        return getData().getNotes();
    }

    @Override
    public State getState() {
        String state = getData().getState();

        if (state == null) {
            return null;
        }

        // Migration to Keycloak 3.2
        if (state.equals("LOGGING_IN")) {
            return State.LOGGED_IN;
        }

        return State.valueOf(state);
    }

    @Override
    public void setState(State state) {
        String stateStr = state==null ? null : state.toString();
        getData().setState(stateStr);
    }

    @Override
    public void restartSession(RealmModel realm, UserModel user, String loginUsername, String ipAddress, String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId) {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof UserSessionModel)) return false;

        UserSessionModel that = (UserSessionModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    protected static class PersistentUserSessionData {

        @JsonProperty("brokerSessionId")
        private String brokerSessionId;

        @JsonProperty("brokerUserId")
        private String brokerUserId;

        @JsonProperty("ipAddress")
        private String ipAddress;

        @JsonProperty("authMethod")
        private String authMethod;

        @JsonProperty("rememberMe")
        private boolean rememberMe;

        // TODO: Keeping those just for backwards compatibility. @JsonIgnoreProperties doesn't work on Wildfly - probably due to classloading issues
        @JsonProperty("started")
        private int started;

        @JsonProperty("notes")
        private Map<String, String> notes;

        @JsonProperty("state")
        private String state;

        public String getBrokerSessionId() {
            return brokerSessionId;
        }

        public void setBrokerSessionId(String brokerSessionId) {
            this.brokerSessionId = brokerSessionId;
        }

        public String getBrokerUserId() {
            return brokerUserId;
        }

        public void setBrokerUserId(String brokerUserId) {
            this.brokerUserId = brokerUserId;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }

        public String getAuthMethod() {
            return authMethod;
        }

        public void setAuthMethod(String authMethod) {
            this.authMethod = authMethod;
        }

        public boolean isRememberMe() {
            return rememberMe;
        }

        public void setRememberMe(boolean rememberMe) {
            this.rememberMe = rememberMe;
        }

        @Deprecated
        public int getStarted() {
            return started;
        }

        @Deprecated
        public void setStarted(int started) {
            this.started = started;
        }

        public Map<String, String> getNotes() {
            return notes;
        }

        public void setNotes(Map<String, String> notes) {
            this.notes = notes;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }
    }
}
