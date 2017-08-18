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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthenticationSessionEntity extends SessionEntity {

    private String id;

    private String clientUuid;
    private String authUserId;

    private String redirectUri;
    private int timestamp;
    private String action;
    private Set<String> roles;
    private Set<String> protocolMappers;

    private Map<String, AuthenticationSessionModel.ExecutionStatus> executionStatus  = new HashMap<>();;
    private String protocol;

    private Map<String, String> clientNotes;
    private Map<String, String> authNotes;
    private Set<String> requiredActions  = new HashSet<>();
    private Map<String, String> userSessionNotes;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClientUuid() {
        return clientUuid;
    }

    public void setClientUuid(String clientUuid) {
        this.clientUuid = clientUuid;
    }

    public String getAuthUserId() {
        return authUserId;
    }

    public void setAuthUserId(String authUserId) {
        this.authUserId = authUserId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthenticationSessionEntity)) return false;

        AuthenticationSessionEntity that = (AuthenticationSessionEntity) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return String.format("AuthenticationSessionEntity [id=%s, realm=%s, clientUuid=%s ]", getId(), getRealm(), getClientUuid());
    }
}
