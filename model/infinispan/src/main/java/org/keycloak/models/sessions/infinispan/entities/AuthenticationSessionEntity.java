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
import org.keycloak.models.sessions.infinispan.util.KeycloakMarshallUtil;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.CommonClientSessionModel.ExecutionStatus;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@SerializeWith(AuthenticationSessionEntity.ExternalizerImpl.class)
public class AuthenticationSessionEntity implements Serializable {

    private String clientUUID;

    private String authUserId;

    private int timestamp;

    private String redirectUri;
    private String action;
    private Set<String> clientScopes;

    private Map<String, AuthenticationSessionModel.ExecutionStatus> executionStatus = new ConcurrentHashMap<>();
    private String protocol;

    private Map<String, String> clientNotes;
    private Map<String, String> authNotes;
    private Set<String> requiredActions  = ConcurrentHashMap.newKeySet();
    private Map<String, String> userSessionNotes;

    public AuthenticationSessionEntity() {
    }

    public AuthenticationSessionEntity(
      String clientUUID,
      String authUserId,
      int timestamp,
      String redirectUri, String action, Set<String> clientScopes,
      Map<String, AuthenticationSessionModel.ExecutionStatus> executionStatus, String protocol,
      Map<String, String> clientNotes, Map<String, String> authNotes, Set<String> requiredActions, Map<String, String> userSessionNotes) {
        this(clientUUID, authUserId, redirectUri, action, clientScopes, executionStatus, protocol, clientNotes, authNotes, requiredActions, userSessionNotes);
        this.timestamp = timestamp;
    }

    public AuthenticationSessionEntity(
            String clientUUID,
            String authUserId,
            String redirectUri, String action, Set<String> clientScopes,
            Map<String, AuthenticationSessionModel.ExecutionStatus> executionStatus, String protocol,
            Map<String, String> clientNotes, Map<String, String> authNotes, Set<String> requiredActions, Map<String, String> userSessionNotes) {
        this.clientUUID = clientUUID;

        this.authUserId = authUserId;

        this.redirectUri = redirectUri;
        this.action = action;
        this.clientScopes = clientScopes;

        this.executionStatus = executionStatus;
        this.protocol = protocol;

        this.clientNotes = clientNotes;
        this.authNotes = authNotes;
        this.requiredActions = requiredActions;
        this.userSessionNotes = userSessionNotes;
    }

    public String getClientUUID() {
        return clientUUID;
    }

    public void setClientUUID(String clientUUID) {
        this.clientUUID = clientUUID;
    }

    public String getAuthUserId() {
        return authUserId;
    }

    public void setAuthUserId(String authUserId) {
        this.authUserId = authUserId;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Set<String> getClientScopes() {
        return clientScopes;
    }

    public void setClientScopes(Set<String> clientScopes) {
        this.clientScopes = clientScopes;
    }

    public Map<String, AuthenticationSessionModel.ExecutionStatus> getExecutionStatus() {
        return executionStatus;
    }

    public void setExecutionStatus(Map<String, AuthenticationSessionModel.ExecutionStatus> executionStatus) {
        this.executionStatus = executionStatus;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Map<String, String> getClientNotes() {
        return clientNotes;
    }

    public void setClientNotes(Map<String, String> clientNotes) {
        this.clientNotes = clientNotes;
    }

    public Set<String> getRequiredActions() {
        return requiredActions;
    }

    public void setRequiredActions(Set<String> requiredActions) {
        this.requiredActions = requiredActions;
    }

    public Map<String, String> getUserSessionNotes() {
        return userSessionNotes;
    }

    public void setUserSessionNotes(Map<String, String> userSessionNotes) {
        this.userSessionNotes = userSessionNotes;
    }

    public Map<String, String> getAuthNotes() {
        return authNotes;
    }

    public void setAuthNotes(Map<String, String> authNotes) {
        this.authNotes = authNotes;
    }

    public static class ExternalizerImpl implements Externalizer<AuthenticationSessionEntity> {

        private static final int VERSION_1 = 1;
        private static final int VERSION_2 = 2;

        public static final ExternalizerImpl INSTANCE = new ExternalizerImpl();

        private static AuthenticationSessionModel.ExecutionStatus fromOrdinal(int ordinal) {
            ExecutionStatus[] values = AuthenticationSessionModel.ExecutionStatus.values();
            return (ordinal < 0 || ordinal >= values.length)
              ? null
              : values[ordinal];
        }

        public static final Externalizer<AuthenticationSessionModel.ExecutionStatus> EXECUTION_STATUS_EXT = new Externalizer<AuthenticationSessionModel.ExecutionStatus>() {

            @Override
            public void writeObject(ObjectOutput output, AuthenticationSessionModel.ExecutionStatus e) throws IOException {
                MarshallUtil.marshallEnum(e, output);
            }

            @Override
            public AuthenticationSessionModel.ExecutionStatus readObject(ObjectInput input) throws IOException, ClassNotFoundException {
                return MarshallUtil.unmarshallEnum(input, ExternalizerImpl::fromOrdinal);
            }
        };

        @Override
        public void writeObject(ObjectOutput output, AuthenticationSessionEntity value) throws IOException {
            output.writeByte(VERSION_2);

            MarshallUtil.marshallString(value.clientUUID, output);

            MarshallUtil.marshallString(value.authUserId, output);

            output.writeInt(value.timestamp);

            MarshallUtil.marshallString(value.redirectUri, output);
            MarshallUtil.marshallString(value.action, output);
            KeycloakMarshallUtil.writeCollection(value.clientScopes, KeycloakMarshallUtil.STRING_EXT, output);

            KeycloakMarshallUtil.writeMap(value.executionStatus, KeycloakMarshallUtil.STRING_EXT, EXECUTION_STATUS_EXT, output);
            MarshallUtil.marshallString(value.protocol, output);

            KeycloakMarshallUtil.writeMap(value.clientNotes, KeycloakMarshallUtil.STRING_EXT, KeycloakMarshallUtil.STRING_EXT, output);
            KeycloakMarshallUtil.writeMap(value.authNotes, KeycloakMarshallUtil.STRING_EXT, KeycloakMarshallUtil.STRING_EXT, output);
            KeycloakMarshallUtil.writeCollection(value.requiredActions, KeycloakMarshallUtil.STRING_EXT, output);
            KeycloakMarshallUtil.writeMap(value.userSessionNotes, KeycloakMarshallUtil.STRING_EXT, KeycloakMarshallUtil.STRING_EXT, output);
        }

        @Override
        public AuthenticationSessionEntity readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                case VERSION_2:
                    return readObjectVersion2(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public AuthenticationSessionEntity readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            return new AuthenticationSessionEntity(
              MarshallUtil.unmarshallString(input),     // clientUUID

              MarshallUtil.unmarshallString(input),     // authUserId

              MarshallUtil.unmarshallString(input),     // redirectUri
              MarshallUtil.unmarshallString(input),     // action
              KeycloakMarshallUtil.readCollection(input, KeycloakMarshallUtil.STRING_EXT, ConcurrentHashMap::newKeySet),  // clientScopes

              KeycloakMarshallUtil.readMap(input, KeycloakMarshallUtil.STRING_EXT, EXECUTION_STATUS_EXT, size -> new ConcurrentHashMap<>(size)), // executionStatus
              MarshallUtil.unmarshallString(input),     // protocol

              KeycloakMarshallUtil.readMap(input, KeycloakMarshallUtil.STRING_EXT, KeycloakMarshallUtil.STRING_EXT, size -> new ConcurrentHashMap<>(size)), // clientNotes
              KeycloakMarshallUtil.readMap(input, KeycloakMarshallUtil.STRING_EXT, KeycloakMarshallUtil.STRING_EXT, size -> new ConcurrentHashMap<>(size)), // authNotes
              KeycloakMarshallUtil.readCollection(input, KeycloakMarshallUtil.STRING_EXT, ConcurrentHashMap::newKeySet),  // requiredActions
              KeycloakMarshallUtil.readMap(input, KeycloakMarshallUtil.STRING_EXT, KeycloakMarshallUtil.STRING_EXT, size -> new ConcurrentHashMap<>(size)) // userSessionNotes
            );
        }

        public AuthenticationSessionEntity readObjectVersion2(ObjectInput input) throws IOException, ClassNotFoundException {
            return new AuthenticationSessionEntity(
                    MarshallUtil.unmarshallString(input),     // clientUUID

                    MarshallUtil.unmarshallString(input),     // authUserId

                    input.readInt(),                          // timestamp

                    MarshallUtil.unmarshallString(input),     // redirectUri
                    MarshallUtil.unmarshallString(input),     // action
                    KeycloakMarshallUtil.readCollection(input, KeycloakMarshallUtil.STRING_EXT, ConcurrentHashMap::newKeySet),  // clientScopes

                    KeycloakMarshallUtil.readMap(input, KeycloakMarshallUtil.STRING_EXT, EXECUTION_STATUS_EXT, size -> new ConcurrentHashMap<>(size)), // executionStatus
                    MarshallUtil.unmarshallString(input),     // protocol

                    KeycloakMarshallUtil.readMap(input, KeycloakMarshallUtil.STRING_EXT, KeycloakMarshallUtil.STRING_EXT, size -> new ConcurrentHashMap<>(size)), // clientNotes
                    KeycloakMarshallUtil.readMap(input, KeycloakMarshallUtil.STRING_EXT, KeycloakMarshallUtil.STRING_EXT, size -> new ConcurrentHashMap<>(size)), // authNotes
                    KeycloakMarshallUtil.readCollection(input, KeycloakMarshallUtil.STRING_EXT, ConcurrentHashMap::newKeySet),  // requiredActions
                    KeycloakMarshallUtil.readMap(input, KeycloakMarshallUtil.STRING_EXT, KeycloakMarshallUtil.STRING_EXT, size -> new ConcurrentHashMap<>(size)) // userSessionNotes
            );
        }
    }
}
