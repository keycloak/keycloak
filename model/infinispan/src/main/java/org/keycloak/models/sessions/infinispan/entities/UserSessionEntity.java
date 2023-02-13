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

package org.keycloak.models.sessions.infinispan.entities;

import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;
import org.jboss.logging.Logger;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionModel.State;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.util.KeycloakMarshallUtil;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@SerializeWith(UserSessionEntity.ExternalizerImpl.class)
public class UserSessionEntity extends SessionEntity {

    public static final Logger logger = Logger.getLogger(UserSessionEntity.class);

    // Metadata attribute, which contains the lastSessionRefresh available on remoteCache. Used in decide whether we need to write to remoteCache (DC) or not
    public static final String LAST_SESSION_REFRESH_REMOTE = "lsrr";

    private String id;

    private String user;

    private String brokerSessionId;
    private String brokerUserId;

    private String loginUsername;

    private String ipAddress;

    private String authMethod;

    private boolean rememberMe;

    private int started;

    private int lastSessionRefresh;

    private UserSessionModel.State state;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    private Map<String, String> notes = new ConcurrentHashMap<>();

    private AuthenticatedClientSessionStore authenticatedClientSessions = new AuthenticatedClientSessionStore();

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getLoginUsername() {
        return loginUsername;
    }

    public void setLoginUsername(String loginUsername) {
        this.loginUsername = loginUsername;
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

    public int getLastSessionRefresh() {
        return lastSessionRefresh;
    }

    public void setLastSessionRefresh(int lastSessionRefresh) {
        this.lastSessionRefresh = lastSessionRefresh;
    }

    public Map<String, String> getNotes() {
        return notes;
    }

    public void setNotes(Map<String, String> notes) {
        this.notes = notes;
    }

    public AuthenticatedClientSessionStore getAuthenticatedClientSessions() {
        return authenticatedClientSessions;
    }

    public void setAuthenticatedClientSessions(AuthenticatedClientSessionStore authenticatedClientSessions) {
        this.authenticatedClientSessions = authenticatedClientSessions;
    }

    public UserSessionModel.State getState() {
        return state;
    }

    public void setState(UserSessionModel.State state) {
        this.state = state;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserSessionEntity)) return false;

        UserSessionEntity that = (UserSessionEntity) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return String.format("UserSessionEntity [id=%s, realm=%s, lastSessionRefresh=%d, clients=%s]", getId(), getRealmId(), getLastSessionRefresh(),
          new TreeSet(this.authenticatedClientSessions.keySet()));
    }

    @Override
    public SessionEntityWrapper mergeRemoteEntityWithLocalEntity(SessionEntityWrapper localEntityWrapper) {
        int lsrRemote = getLastSessionRefresh();

        SessionEntityWrapper entityWrapper;
        if (localEntityWrapper == null) {
            entityWrapper = new SessionEntityWrapper<>(this);
        } else {
            UserSessionEntity localUserSession = (UserSessionEntity) localEntityWrapper.getEntity();

            // local lastSessionRefresh should always contain the bigger
            if (lsrRemote < localUserSession.getLastSessionRefresh()) {
                setLastSessionRefresh(localUserSession.getLastSessionRefresh());
            }

            entityWrapper = new SessionEntityWrapper<>(localEntityWrapper.getLocalMetadata(), this);
        }

        entityWrapper.putLocalMetadataNoteInt(LAST_SESSION_REFRESH_REMOTE, lsrRemote);

        logger.debugf("Updating session entity '%s'. lastSessionRefresh=%d, lastSessionRefreshRemote=%d", getId(), getLastSessionRefresh(), lsrRemote);

        return entityWrapper;
    }


    public static class ExternalizerImpl implements Externalizer<UserSessionEntity> {

        private static final int VERSION_1 = 1;

        private static final EnumMap<UserSessionModel.State, Integer> STATE_TO_ID = new EnumMap<>(UserSessionModel.State.class);
        private static final Map<Integer, UserSessionModel.State> ID_TO_STATE = new HashMap<>();
        static {
            STATE_TO_ID.put(State.LOGGED_IN, 1);
            STATE_TO_ID.put(State.LOGGED_OUT, 2);
            STATE_TO_ID.put(State.LOGGING_OUT, 3);

            for (Entry<State, Integer> entry : STATE_TO_ID.entrySet()) {
                ID_TO_STATE.put(entry.getValue(), entry.getKey());
            }
        }

        @Override
        public void writeObject(ObjectOutput output, UserSessionEntity session) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(session.getAuthMethod(), output);
            MarshallUtil.marshallString(session.getBrokerSessionId(), output);
            MarshallUtil.marshallString(session.getBrokerUserId(), output);
            MarshallUtil.marshallString(session.getId(), output);
            MarshallUtil.marshallString(session.getIpAddress(), output);
            MarshallUtil.marshallString(session.getLoginUsername(), output);
            MarshallUtil.marshallString(session.getRealmId(), output);
            MarshallUtil.marshallString(session.getUser(), output);

            KeycloakMarshallUtil.marshall(session.getLastSessionRefresh(), output);
            KeycloakMarshallUtil.marshall(session.getStarted(), output);
            output.writeBoolean(session.isRememberMe());

            int state = session.getState() == null ? 0 : STATE_TO_ID.get(session.getState());
            output.writeInt(state);

            Map<String, String> notes = session.getNotes();
            KeycloakMarshallUtil.writeMap(notes, KeycloakMarshallUtil.STRING_EXT, KeycloakMarshallUtil.STRING_EXT, output);

            output.writeObject(session.getAuthenticatedClientSessions());
        }


        @Override
        public UserSessionEntity readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public UserSessionEntity readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            UserSessionEntity sessionEntity = new UserSessionEntity();

            sessionEntity.setAuthMethod(MarshallUtil.unmarshallString(input));
            sessionEntity.setBrokerSessionId(MarshallUtil.unmarshallString(input));
            sessionEntity.setBrokerUserId(MarshallUtil.unmarshallString(input));
            final String userSessionId = MarshallUtil.unmarshallString(input);
            sessionEntity.setId(userSessionId);
            sessionEntity.setIpAddress(MarshallUtil.unmarshallString(input));
            sessionEntity.setLoginUsername(MarshallUtil.unmarshallString(input));
            sessionEntity.setRealmId(MarshallUtil.unmarshallString(input));
            sessionEntity.setUser(MarshallUtil.unmarshallString(input));

            sessionEntity.setLastSessionRefresh(KeycloakMarshallUtil.unmarshallInteger(input));
            sessionEntity.setStarted(KeycloakMarshallUtil.unmarshallInteger(input));
            sessionEntity.setRememberMe(input.readBoolean());

            sessionEntity.setState(ID_TO_STATE.get(input.readInt()));

            Map<String, String> notes = KeycloakMarshallUtil.readMap(input, KeycloakMarshallUtil.STRING_EXT, KeycloakMarshallUtil.STRING_EXT,
                    new KeycloakMarshallUtil.ConcurrentHashMapBuilder<>());
            sessionEntity.setNotes(notes);

            AuthenticatedClientSessionStore authSessions = (AuthenticatedClientSessionStore) input.readObject();
            sessionEntity.setAuthenticatedClientSessions(authSessions);

            return sessionEntity;
        }

    }
}
