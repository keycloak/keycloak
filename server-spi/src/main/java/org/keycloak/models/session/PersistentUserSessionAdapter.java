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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PersistentUserSessionAdapter implements UserSessionModel {

    private final PersistentUserSessionModel model;
    private final UserModel user;
    private final RealmModel realm;
    private final List<ClientSessionModel> clientSessions;

    private PersistentUserSessionData data;

    public PersistentUserSessionAdapter(UserSessionModel other) {
        this.data = new PersistentUserSessionData();
        data.setAuthMethod(other.getAuthMethod());
        data.setBrokerSessionId(other.getBrokerSessionId());
        data.setBrokerUserId(other.getBrokerUserId());
        data.setIpAddress(other.getIpAddress());
        data.setNotes(other.getNotes());
        data.setRememberMe(other.isRememberMe());
        data.setStarted(other.getStarted());
        data.setState(other.getState());

        this.model = new PersistentUserSessionModel();
        this.model.setUserSessionId(other.getId());
        this.model.setLastSessionRefresh(other.getLastSessionRefresh());

        this.user = other.getUser();
        this.realm = other.getRealm();
        this.clientSessions = other.getClientSessions();
    }

    public PersistentUserSessionAdapter(PersistentUserSessionModel model, RealmModel realm, UserModel user, List<ClientSessionModel> clientSessions) {
        this.model = model;
        this.realm = realm;
        this.user = user;
        this.clientSessions = clientSessions;
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
        return user;
    }

    @Override
    public RealmModel getRealm() {
        return realm;
    }

    @Override
    public String getLoginUsername() {
        return user.getUsername();
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
        return getData().getStarted();
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
    public List<ClientSessionModel> getClientSessions() {
        return clientSessions;
    }

    @Override
    public String getNote(String name) {
        return getData().getNotes()==null ? null : getData().getNotes().get(name);
    }

    @Override
    public void setNote(String name, String value) {
        PersistentUserSessionData data = getData();
        if (data.getNotes() == null) {
            data.setNotes(new HashMap<String, String>());
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
        return getData().getState();
    }

    @Override
    public void setState(State state) {
        getData().setState(state);
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

        @JsonProperty("started")
        private int started;

        @JsonProperty("notes")
        private Map<String, String> notes;

        @JsonProperty("state")
        private State state;

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

        public int getStarted() {
            return started;
        }

        public void setStarted(int started) {
            this.started = started;
        }

        public Map<String, String> getNotes() {
            return notes;
        }

        public void setNotes(Map<String, String> notes) {
            this.notes = notes;
        }

        public State getState() {
            return state;
        }

        public void setState(State state) {
            this.state = state;
        }
    }
}
