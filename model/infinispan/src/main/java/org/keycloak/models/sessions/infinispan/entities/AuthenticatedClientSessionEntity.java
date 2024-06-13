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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;
import org.jboss.logging.Logger;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.util.KeycloakMarshallUtil;
import java.util.UUID;

/**
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@SerializeWith(AuthenticatedClientSessionEntity.ExternalizerImpl.class)
public class AuthenticatedClientSessionEntity extends SessionEntity {

    public static final Logger logger = Logger.getLogger(AuthenticatedClientSessionEntity.class);

    // Metadata attribute, which contains the last timestamp available on remoteCache. Used in decide whether we need to write to remoteCache (DC) or not
    public static final String LAST_TIMESTAMP_REMOTE = "lstr";
    public static final String CLIENT_ID_NOTE = "clientId";

    private String authMethod;
    private String redirectUri;
    private volatile int timestamp;
    private String action;

    private Map<String, String> notes = new ConcurrentHashMap<>();

    private final UUID id;

    private transient String userSessionId;

    public AuthenticatedClientSessionEntity(UUID id) {
        this.id = id;
    }

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

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public int getUserSessionStarted() {
        String started = getNotes().get(AuthenticatedClientSessionModel.USER_SESSION_STARTED_AT_NOTE);
        return started == null ? timestamp : Integer.parseInt(started);
    }

    public int getStarted() {
        String started = getNotes().get(AuthenticatedClientSessionModel.STARTED_AT_NOTE);
        return started == null ? timestamp : Integer.parseInt(started);
    }

    public boolean isUserSessionRememberMe() {
        return Boolean.parseBoolean(getNotes().get(AuthenticatedClientSessionModel.USER_SESSION_REMEMBER_ME_NOTE));
    }

    public String getClientId() {
        return getNotes().get(CLIENT_ID_NOTE);
    }

    public void setClientId(String clientId) {
        getNotes().put(CLIENT_ID_NOTE, clientId);
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Map<String, String> getNotes() {
        return notes;
    }

    public void setNotes(Map<String, String> notes) {
        this.notes = notes;
    }

    public UUID getId() {
        return id;
    }

    @Override
    public String toString() {
        return "AuthenticatedClientSessionEntity [" + "id=" + id + ']';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthenticatedClientSessionEntity)) return false;

        AuthenticatedClientSessionEntity that = (AuthenticatedClientSessionEntity) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public SessionEntityWrapper mergeRemoteEntityWithLocalEntity(SessionEntityWrapper localEntityWrapper) {
        int timestampRemote = getTimestamp();

        SessionEntityWrapper entityWrapper;
        if (localEntityWrapper == null) {
            entityWrapper = new SessionEntityWrapper<>(this);
        } else {
            AuthenticatedClientSessionEntity localClientSession = (AuthenticatedClientSessionEntity) localEntityWrapper.getEntity();

            // local timestamp should always contain the bigger
            if (timestampRemote < localClientSession.getTimestamp()) {
                setTimestamp(localClientSession.getTimestamp());
            }

            entityWrapper = new SessionEntityWrapper<>(localEntityWrapper.getLocalMetadata(), this);
        }

        entityWrapper.putLocalMetadataNoteInt(LAST_TIMESTAMP_REMOTE, timestampRemote);

        logger.debugf("Updating client session entity %s. timestamp=%d, timestampRemote=%d", getId(), getTimestamp(), timestampRemote);

        return entityWrapper;
    }

    public String getUserSessionId() {
        return userSessionId;
    }

    public void setUserSessionId(String userSessionId) {
        this.userSessionId = userSessionId;
    }

    public static class ExternalizerImpl implements Externalizer<AuthenticatedClientSessionEntity> {

        @Override
        public void writeObject(ObjectOutput output, AuthenticatedClientSessionEntity session) throws IOException {
            MarshallUtil.marshallUUID(session.id, output, false);
            MarshallUtil.marshallString(session.getRealmId(), output);
            MarshallUtil.marshallString(session.getAuthMethod(), output);
            MarshallUtil.marshallString(session.getRedirectUri(), output);
            KeycloakMarshallUtil.marshall(session.getTimestamp(), output);
            MarshallUtil.marshallString(session.getAction(), output);

            Map<String, String> notes = session.getNotes();
            KeycloakMarshallUtil.writeMap(notes, KeycloakMarshallUtil.STRING_EXT, KeycloakMarshallUtil.STRING_EXT, output);

        }


        @Override
        public AuthenticatedClientSessionEntity readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            AuthenticatedClientSessionEntity sessionEntity = new AuthenticatedClientSessionEntity(MarshallUtil.unmarshallUUID(input, false));

            sessionEntity.setRealmId(MarshallUtil.unmarshallString(input));

            sessionEntity.setAuthMethod(MarshallUtil.unmarshallString(input));
            sessionEntity.setRedirectUri(MarshallUtil.unmarshallString(input));
            sessionEntity.setTimestamp(KeycloakMarshallUtil.unmarshallInteger(input));
            sessionEntity.setAction(MarshallUtil.unmarshallString(input));

            Map<String, String> notes = KeycloakMarshallUtil.readMap(input, KeycloakMarshallUtil.STRING_EXT, KeycloakMarshallUtil.STRING_EXT,
                    new KeycloakMarshallUtil.ConcurrentHashMapBuilder<>());
            sessionEntity.setNotes(notes);

            return sessionEntity;
        }

    }

}
