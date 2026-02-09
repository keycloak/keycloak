/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.keycloak.common.util.Time;
import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.UserSessionModel;

import org.infinispan.api.annotations.indexing.Basic;
import org.infinispan.api.annotations.indexing.Indexed;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

@ProtoTypeId(Marshalling.REMOTE_CLIENT_SESSION_ENTITY)
@Indexed
public class RemoteAuthenticatedClientSessionEntity {

    // immutable state
    private final String userSessionId;
    private final String clientId;
    private final String userId;
    private final String realmId;

    // mutable state
    private int started;
    private String protocol;
    private String redirectUri;
    private String action;
    private Map<String, String> notes;
    private int timestamp;

    private RemoteAuthenticatedClientSessionEntity(String userSessionId, String clientId, String userId, String realmId) {
        this.userSessionId = Objects.requireNonNull(userSessionId);
        this.clientId = Objects.requireNonNull(clientId);
        this.userId = Objects.requireNonNull(userId);
        this.realmId = Objects.requireNonNull(realmId);
    }

    @ProtoFactory
    RemoteAuthenticatedClientSessionEntity(String clientId, String userId, String userSessionId, String realmId, Map<String, String> notes, String action, String protocol, String redirectUri, int timestamp, int started) {
        this.userSessionId = userSessionId;
        this.clientId = clientId;
        this.userId = userId;
        this.realmId = realmId;
        this.action = action;
        this.protocol = protocol;
        this.redirectUri = redirectUri;
        this.notes = notes;
        this.timestamp = timestamp;
        this.started = started;
    }

    public static RemoteAuthenticatedClientSessionEntity create(ClientSessionKey id, String realmId, UserSessionModel userSession) {
        var e = new RemoteAuthenticatedClientSessionEntity(id.userSessionId(), id.clientId(), userSession.getUser().getId(), realmId);
        e.timestamp = e.started = Time.currentTime();
        e.notes = new HashMap<>();
        return e;
    }

    public static RemoteAuthenticatedClientSessionEntity createFromModel(ClientSessionKey id, AuthenticatedClientSessionModel model) {
        var e = new RemoteAuthenticatedClientSessionEntity(id.userSessionId(), id.clientId(), model.getUserSession().getUser().getId(), model.getRealm().getId());
        e.timestamp = e.started = Time.currentTime();
        e.notes = model.getNotes() == null || model.getNotes().isEmpty() ?
                new HashMap<>() :
                new HashMap<>(model.getNotes());
        return e;
    }

    // for testing purposes only!
    public static RemoteAuthenticatedClientSessionEntity mockEntity(String userSessionId, String userId, String realmId) {
        return mockEntity(userSessionId, "client", userId, realmId);
    }

    // for testing purposes only!
    public static RemoteAuthenticatedClientSessionEntity mockEntity(String userSessionId, String clientId, String userId, String realmId) {
        return new RemoteAuthenticatedClientSessionEntity(userSessionId, clientId, userId, realmId);
    }

    @ProtoField(1)
    @Basic(projectable = true, sortable = true)
    public String getClientId() {
        return clientId;
    }

    @ProtoField(2)
    @Basic
    public String getUserId() {
        return userId;
    }

    @ProtoField(3)
    @Basic(projectable = true, sortable = true)
    public String getUserSessionId() {
        return userSessionId;
    }

    @ProtoField(4)
    @Basic
    public String getRealmId() {
        return realmId;
    }

    @ProtoField(value = 5, mapImplementation = HashMap.class)
    public Map<String, String> getNotes() {
        return notes;
    }

    public void setNotes(Map<String, String> notes) {
        this.notes = notes;
    }

    @ProtoField(6)
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    @ProtoField(7)
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @ProtoField(8)
    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    @ProtoField(9)
    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    @ProtoField(10)
    public int getStarted() {
        return started;
    }

    public void setStarted(int started) {
        this.started = started;
    }

    public void restart() {
        action = null;
        redirectUri = null;
        timestamp = started = Time.currentTime();
        notes.clear();
    }

    public ClientSessionKey createCacheKey() {
        return new ClientSessionKey(userSessionId, clientId);
    }

    public String createId() {
        return UUID.nameUUIDFromBytes((userSessionId + clientId).getBytes(StandardCharsets.UTF_8)).toString();
    }

}
