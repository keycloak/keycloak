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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.keycloak.common.util.MultiSiteUtils;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.OfflineUserSessionModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.light.LightweightUserAdapter;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.annotation.JsonProperty;

import static org.keycloak.models.Constants.SESSION_NOTE_LIGHTWEIGHT_USER;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PersistentUserSessionAdapter implements OfflineUserSessionModel {

    private final PersistentUserSessionModel model;
    private UserModel user;
    private final String userId;
    private RealmModel realm;
    private KeycloakSession session;
    private final Map<String, AuthenticatedClientSessionModel> authenticatedClientSessions;

    private PersistentUserSessionData data;
    private Consumer<Map<String, AuthenticatedClientSessionModel>> clientSessionsLoader = ignored -> {};

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

        this.model = new PersistentUserSessionModel() {
            private String userSessionId;
            private int started;
            private int lastSessionRefresh;
            private boolean offline;
            private String data;
            private boolean rememberMe;

            @Override
            public String getUserSessionId() {
                return userSessionId;
            }

            @Override
            public void setUserSessionId(String userSessionId) {
                this.userSessionId = userSessionId;
            }

            @Override
            public int getStarted() {
                return started;
            }

            @Override
            public void setStarted(int started) {
                this.started = started;
            }

            @Override
            public int getLastSessionRefresh() {
                return lastSessionRefresh;
            }

            @Override
            public void setLastSessionRefresh(int lastSessionRefresh) {
                this.lastSessionRefresh = lastSessionRefresh;
            }

            @Override
            public boolean isOffline() {
                return offline;
            }

            @Override
            public void setOffline(boolean offline) {
                this.offline = offline;
            }

            @Override
            public String getData() {
                return data;
            }

            @Override
            public void setData(String data) {
                this.data = data;
            }

            @Override
            public void setRealmId(String realmId) {
                /* ignored */
            }

            @Override
            public void setUserId(String userId) {
                /* ignored */
            }

            @Override
            public void setBrokerSessionId(String brokerSessionId) {
                /* ignored */
            }

            @Override
            public boolean isRememberMe() {
                return rememberMe;
            }

            @Override
            public void setRememberMe(boolean rememberMe) {
                this.rememberMe = rememberMe;
            }
        };
        this.model.setStarted(other.getStarted());
        this.model.setUserSessionId(other.getId());
        this.model.setLastSessionRefresh(other.getLastSessionRefresh());
        this.model.setRememberMe(other.isRememberMe());

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
                throw new ModelException("Error restoring session", ioe);
            }
        }

        return data;
    }

    // Write updated model with latest serialized data
    public PersistentUserSessionModel getUpdatedModel() {
        try {
            if (data != null) {
                // If data hasn't been initialized, it hasn't been touched and is unchanged. So need to deserialize and serialize it
                String updatedData = JsonSerialization.writeValueAsString(getData());
                this.model.setData(updatedData);
            }
        } catch (IOException ioe) {
            throw new ModelException("Error persisting session", ioe);
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
            if (LightweightUserAdapter.isLightweightUser(userId)) {
                user = LightweightUserAdapter.fromString(session, realm, getData().getNotes().get(SESSION_NOTE_LIGHTWEIGHT_USER));
            } else {
                user = session.users().getUserById(realm, userId);
            }
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
        if (isOffline() || !MultiSiteUtils.isPersistentSessionsEnabled()) {
            return getUser().getUsername();
        } else {
            return getData().getLoginUsername();
        }
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
        return model.isRememberMe() || getData().isRememberMe();
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
        if (seconds <= getLastSessionRefresh()) {
            return;
        }
        model.setLastSessionRefresh(seconds);
    }

    @Override
    public boolean isOffline() {
        return model.isOffline();
    }

    @Override
    public Map<String, AuthenticatedClientSessionModel> getAuthenticatedClientSessions() {
        clientSessionsLoader.accept(authenticatedClientSessions);
        return authenticatedClientSessions;
    }

    @Override
    public void removeAuthenticatedClientSessions(Collection<String> removedClientUUIDS) {
        if (removedClientUUIDS == null || ! removedClientUUIDS.iterator().hasNext()) {
            return;
        }

        removedClientUUIDS.forEach(getAuthenticatedClientSessions()::remove);
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
    public void setLoginUsername(String loginUsername) {
        getData().setLoginUsername(loginUsername);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof UserSessionModel that && that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public String toString() {
        return getId();
    }

    public void setRealm(RealmModel realm) {
        this.realm = realm;
        model.setRealmId(realm.getId());
    }

    public void setUser(UserModel user) {
        this.user = user;
        model.setUserId(user.getId());
    }

    public void setIpAddress(String ipAddress) {
        getData().setIpAddress(ipAddress);
    }

    public void setAuthMethod(String authMethod) {
        getData().setAuthMethod(authMethod);
    }

    public void setRememberMe(boolean rememberMe) {
        getData().setRememberMe(rememberMe);
        model.setRememberMe(rememberMe);
    }

    public void setStarted(int started) {
        getData().setStarted(started);
        model.setStarted(started);
    }

    public void setBrokerSessionId(String brokerSessionId) {
        getData().setBrokerSessionId(brokerSessionId);
        model.setBrokerSessionId(brokerSessionId);
    }

    public void setBrokerUserId(String brokerUserId) {
        getData().setBrokerUserId(brokerUserId);
    }

    public void setClientSessionsLoader(Consumer<Map<String, AuthenticatedClientSessionModel>> clientSessionsLoader) {
        this.clientSessionsLoader = Objects.requireNonNullElse(clientSessionsLoader, this.clientSessionsLoader);
    }

    public boolean requiresRememberMeMigration() {
        return model.isRememberMe() != getData().isRememberMe();
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

        @JsonProperty("loginUsername")
        private String loginUsername;

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

        @Deprecated(since = "26.5", forRemoval = true)
        public boolean isRememberMe() {
            return rememberMe;
        }

        @Deprecated(since = "26.5", forRemoval = true)
        public void setRememberMe(boolean rememberMe) {
            this.rememberMe = rememberMe;
        }

        @Deprecated(since = "26.5", forRemoval = true)
        public int getStarted() {
            return started;
        }

        @Deprecated(since = "26.5", forRemoval = true)
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

        public void setLoginUsername(String loginUsername) {
            this.loginUsername = loginUsername;
        }

        public String getLoginUsername() {
            return loginUsername;
        }
    }
}
