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
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;
import org.keycloak.models.sessions.infinispan.util.KeycloakMarshallUtil;

/**
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@SerializeWith(AuthenticatedClientSessionEntity.ExternalizerImpl.class)
public class AuthenticatedClientSessionEntity implements Serializable {

    private String authMethod;
    private String redirectUri;
    private volatile int timestamp;
    private String action;

    private Set<String> roles;
    private Set<String> protocolMappers;
    private Map<String, String> notes = new ConcurrentHashMap<>();

    private String currentRefreshToken;
    private int currentRefreshTokenUseCount;

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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public Set<String> getProtocolMappers() {
        return protocolMappers;
    }

    public void setProtocolMappers(Set<String> protocolMappers) {
        this.protocolMappers = protocolMappers;
    }

    public Map<String, String> getNotes() {
        return notes;
    }

    public void setNotes(Map<String, String> notes) {
        this.notes = notes;
    }

    public String getCurrentRefreshToken() {
        return currentRefreshToken;
    }

    public void setCurrentRefreshToken(String currentRefreshToken) {
        this.currentRefreshToken = currentRefreshToken;
    }

    public int getCurrentRefreshTokenUseCount() {
        return currentRefreshTokenUseCount;
    }

    public void setCurrentRefreshTokenUseCount(int currentRefreshTokenUseCount) {
        this.currentRefreshTokenUseCount = currentRefreshTokenUseCount;
    }

    public static class ExternalizerImpl implements Externalizer<AuthenticatedClientSessionEntity> {

        @Override
        public void writeObject(ObjectOutput output, AuthenticatedClientSessionEntity session) throws IOException {
            MarshallUtil.marshallString(session.getAuthMethod(), output);
            MarshallUtil.marshallString(session.getRedirectUri(), output);
            MarshallUtil.marshallInt(output, session.getTimestamp());
            MarshallUtil.marshallString(session.getAction(), output);

            Map<String, String> notes = session.getNotes();
            KeycloakMarshallUtil.writeMap(notes, KeycloakMarshallUtil.STRING_EXT, KeycloakMarshallUtil.STRING_EXT, output);

            KeycloakMarshallUtil.writeCollection(session.getProtocolMappers(), KeycloakMarshallUtil.STRING_EXT, output);
            KeycloakMarshallUtil.writeCollection(session.getRoles(), KeycloakMarshallUtil.STRING_EXT, output);

            MarshallUtil.marshallString(session.getCurrentRefreshToken(), output);
            MarshallUtil.marshallInt(output, session.getCurrentRefreshTokenUseCount());
        }


        @Override
        public AuthenticatedClientSessionEntity readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            AuthenticatedClientSessionEntity sessionEntity = new AuthenticatedClientSessionEntity();

            sessionEntity.setAuthMethod(MarshallUtil.unmarshallString(input));
            sessionEntity.setRedirectUri(MarshallUtil.unmarshallString(input));
            sessionEntity.setTimestamp(MarshallUtil.unmarshallInt(input));
            sessionEntity.setAction(MarshallUtil.unmarshallString(input));

            Map<String, String> notes = KeycloakMarshallUtil.readMap(input, KeycloakMarshallUtil.STRING_EXT, KeycloakMarshallUtil.STRING_EXT,
                    new KeycloakMarshallUtil.ConcurrentHashMapBuilder<>());
            sessionEntity.setNotes(notes);

            Set<String> protocolMappers = KeycloakMarshallUtil.readCollection(input, KeycloakMarshallUtil.STRING_EXT, new KeycloakMarshallUtil.HashSetBuilder<>());
            sessionEntity.setProtocolMappers(protocolMappers);

            Set<String> roles = KeycloakMarshallUtil.readCollection(input, KeycloakMarshallUtil.STRING_EXT, new KeycloakMarshallUtil.HashSetBuilder<>());
            sessionEntity.setRoles(roles);

            sessionEntity.setCurrentRefreshToken(MarshallUtil.unmarshallString(input));
            sessionEntity.setCurrentRefreshTokenUseCount(MarshallUtil.unmarshallInt(input));

            return sessionEntity;
        }

    }

}
