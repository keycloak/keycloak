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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.keycloak.marshalling.Marshalling;
import org.keycloak.sessions.AuthenticationSessionModel;

import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@ProtoTypeId(Marshalling.AUTHENTICATION_SESSION_ENTITY)
public class AuthenticationSessionEntity {

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

    @ProtoField(1)
    public String getClientUUID() {
        return clientUUID;
    }

    public void setClientUUID(String clientUUID) {
        this.clientUUID = clientUUID;
    }

    @ProtoField(2)
    public String getAuthUserId() {
        return authUserId;
    }

    public void setAuthUserId(String authUserId) {
        this.authUserId = authUserId;
    }

    @ProtoField(3)
    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    @ProtoField(4)
    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    @ProtoField(5)
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    @ProtoField(value = 6, collectionImplementation = HashSet.class)
    public Set<String> getClientScopes() {
        return clientScopes;
    }

    public void setClientScopes(Set<String> clientScopes) {
        this.clientScopes = clientScopes;
    }

    @ProtoField(value = 7, mapImplementation = ConcurrentHashMap.class)
    public Map<String, AuthenticationSessionModel.ExecutionStatus> getExecutionStatus() {
        return executionStatus;
    }

    public void setExecutionStatus(Map<String, AuthenticationSessionModel.ExecutionStatus> executionStatus) {
        this.executionStatus = executionStatus;
    }

    @ProtoField(8)
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @ProtoField(value = 9, mapImplementation = ConcurrentHashMap.class)
    public Map<String, String> getClientNotes() {
        return clientNotes;
    }

    public void setClientNotes(Map<String, String> clientNotes) {
        this.clientNotes = clientNotes;
    }

    @ProtoField(value = 10, collectionImplementation = HashSet.class)
    public Set<String> getRequiredActions() {
        return requiredActions;
    }

    public void setRequiredActions(Set<String> requiredActions) {
        this.requiredActions = requiredActions;
    }

    @ProtoField(value = 11, mapImplementation = ConcurrentHashMap.class)
    public Map<String, String> getUserSessionNotes() {
        return userSessionNotes;
    }

    public void setUserSessionNotes(Map<String, String> userSessionNotes) {
        this.userSessionNotes = userSessionNotes;
    }

    @ProtoField(value = 12, mapImplementation = ConcurrentHashMap.class)
    public Map<String, String> getAuthNotes() {
        return authNotes;
    }

    public void setAuthNotes(Map<String, String> authNotes) {
        this.authNotes = authNotes;
    }
}
