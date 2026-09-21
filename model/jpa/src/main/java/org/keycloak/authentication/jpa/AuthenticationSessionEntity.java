/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authentication.jpa;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.keycloak.connections.jpa.AsynchronousCommitAllowed;

import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "AUTH_SESSION")
@DynamicUpdate
@IdClass(AuthenticationSessionKey.class)
public class AuthenticationSessionEntity implements AsynchronousCommitAllowed {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ROOT_AUTH_SESSION_ID")
    private RootAuthenticationSessionEntity rootAuthenticationSession;

    @Id
    @Column(name = "TAB_ID", length = 36)
    private String tabId;

    @Column(name = "CLIENT_UUID")
    private String clientUUID;

    @Column(name = "AUTH_USER_ID")
    private String authUserId;

    @Column(name = "TIMESTAMP")
    private long timestamp;

    @Version
    @Column(name = "VERSION")
    private int version;

    @Column(name = "REDIRECT_URI", columnDefinition = "TEXT")
    private String redirectUri;

    @Column(name = "ACTION")
    private String action;

    @Column(name = "PROTOCOL")
    private String protocol;

    @Column(name = "CLIENT_SCOPES", columnDefinition = "TEXT")
    private String clientScopes;

    @Column(name = "EXECUTION_STATUS", columnDefinition = "TEXT")
    private String executionStatus;

    @Column(name = "CLIENT_NOTES", columnDefinition = "TEXT")
    private String clientNotes;

    @Column(name = "AUTH_NOTES", columnDefinition = "TEXT")
    private String authNotes;

    @Column(name = "REQUIRED_ACTIONS", columnDefinition = "TEXT")
    private String requiredActions;

    @Column(name = "USER_SESSION_NOTES", columnDefinition = "TEXT")
    private String userSessionNotes;

    public RootAuthenticationSessionEntity getRootAuthenticationSession() {
        return rootAuthenticationSession;
    }

    public void setRootAuthenticationSession(RootAuthenticationSessionEntity rootAuthenticationSession) {
        this.rootAuthenticationSession = rootAuthenticationSession;
    }

    public String getTabId() {
        return tabId;
    }

    public void setTabId(String tabId) {
        this.tabId = tabId;
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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
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

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getClientScopes() {
        return clientScopes;
    }

    public void setClientScopes(String clientScopes) {
        this.clientScopes = clientScopes;
    }

    public String getExecutionStatus() {
        return executionStatus;
    }

    public void setExecutionStatus(String executionStatus) {
        this.executionStatus = executionStatus;
    }

    public String getClientNotes() {
        return clientNotes;
    }

    public void setClientNotes(String clientNotes) {
        this.clientNotes = clientNotes;
    }

    public String getAuthNotes() {
        return authNotes;
    }

    public void setAuthNotes(String authNotes) {
        this.authNotes = authNotes;
    }

    public String getRequiredActions() {
        return requiredActions;
    }

    public void setRequiredActions(String requiredActions) {
        this.requiredActions = requiredActions;
    }

    public String getUserSessionNotes() {
        return userSessionNotes;
    }

    public void setUserSessionNotes(String userSessionNotes) {
        this.userSessionNotes = userSessionNotes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthenticationSessionEntity that)) return false;
        return Objects.equals(tabId, that.tabId);
    }

    @Override
    public int hashCode() {
        return tabId != null ? tabId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "AuthenticationSessionEntity{" +
                "tabId='" + tabId + '\'' +
                ", rootAuthenticationSessionId=" + (rootAuthenticationSession != null ? rootAuthenticationSession.getId() : null) + // avoid lazy-load just for a log message.
                '}';
    }

}
