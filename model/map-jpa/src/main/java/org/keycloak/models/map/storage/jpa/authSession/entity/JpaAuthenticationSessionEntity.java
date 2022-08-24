/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.jpa.authSession.entity;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.keycloak.models.map.authSession.MapAuthenticationSessionEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UpdatableEntity;
import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_AUTH_SESSION;
import org.keycloak.models.map.storage.jpa.JpaRootVersionedEntity;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.JsonbType;
import org.keycloak.sessions.CommonClientSessionModel;

/**
 * Entity represents individual authentication session. 
 */
@Entity
@Table(name = "kc_auth_session")
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonbType.class)})
public class JpaAuthenticationSessionEntity extends UpdatableEntity.Impl implements MapAuthenticationSessionEntity, JpaRootVersionedEntity {

    @Id
    @Column
    @GeneratedValue
    private UUID id;

    //used for implicit optimistic locking
    @Version
    @Column
    private int version;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private final JpaAuthenticationSessionMetadata metadata;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="fk_root")
    private JpaRootAuthenticationSessionEntity root;

    /**
     * No-argument constructor, used by hibernate to instantiate entities.
     */
    public JpaAuthenticationSessionEntity() {
        this.metadata = new JpaAuthenticationSessionMetadata();
    }

    public JpaAuthenticationSessionEntity(DeepCloner cloner) {
        this.metadata = new JpaAuthenticationSessionMetadata(cloner);
    }

    public void setParent(JpaRootAuthenticationSessionEntity root) {
        this.root = root;
    }

    @Override
    public String getId() {
        return id == null ? null : id.toString();
    }

    @Override
    public void setId(String id) {
        this.id = id == null ? null : UUID.fromString(id);
    }

    @Override
    public Integer getEntityVersion() {
        return metadata.getEntityVersion();
    }

    @Override
    public void setEntityVersion(Integer version) {
        metadata.setEntityVersion(version);
    }

    @Override
    public Integer getCurrentSchemaVersion() {
        return CURRENT_SCHEMA_VERSION_AUTH_SESSION;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public String getTabId() {
        return metadata.getTabId();
    }

    @Override
    public void setTabId(String tabId) {
        metadata.setTabId(tabId);
    }

    @Override
    public Map<String, String> getUserSessionNotes() {
        return metadata.getUserSessionNotes();
    }

    @Override
    public void setUserSessionNotes(Map<String, String> userSessionNotes) {
        metadata.setUserSessionNotes(userSessionNotes);
    }

    @Override
    public void setUserSessionNote(String name, String value) {
        metadata.setUserSessionNote(name, value);
    }

    @Override
    public String getClientUUID() {
        return metadata.getClientUUID();
    }

    @Override
    public void setClientUUID(String clientUUID) {
        metadata.setClientUUID(clientUUID);
    }

    @Override
    public String getAuthUserId() {
        return metadata.getAuthUserId();
    }

    @Override
    public void setAuthUserId(String authUserId) {
        metadata.setAuthUserId(authUserId);
    }

    @Override
    public Long getTimestamp() {
        return metadata.getTimestamp();
    }

    @Override
    public void setTimestamp(Long timestamp) {
        metadata.setTimestamp(timestamp);
    }

    @Override
    public String getRedirectUri() {
        return metadata.getRedirectUri();
    }

    @Override
    public void setRedirectUri(String redirectUri) {
        metadata.setRedirectUri(redirectUri);
    }

    @Override
    public String getAction() {
        return metadata.getAction();
    }

    @Override
    public void setAction(String action) {
        metadata.setAction(action);
    }

    @Override
    public Set<String> getClientScopes() {
        return metadata.getClientScopes();
    }

    @Override
    public void setClientScopes(Set<String> clientScopes) {
        metadata.setClientScopes(clientScopes);
    }

    @Override
    public Set<String> getRequiredActions() {
        return metadata.getRequiredActions();
    }

    @Override
    public void setRequiredActions(Set<String> requiredActions) {
        metadata.setRequiredActions(requiredActions);
    }

    @Override
    public void addRequiredAction(String requiredAction) {
        metadata.addRequiredAction(requiredAction);
    }

    @Override
    public void removeRequiredAction(String action) {
        metadata.removeRequiredAction(action);
    }

    @Override
    public String getProtocol() {
        return metadata.getProtocol();
    }

    @Override
    public void setProtocol(String protocol) {
        metadata.setProtocol(protocol);
    }

    @Override
    public Map<String, String> getClientNotes() {
        return metadata.getClientNotes();
    }

    @Override
    public void setClientNotes(Map<String, String> clientNotes) {
        metadata.setClientNotes(clientNotes);
    }

    @Override
    public void setClientNote(String name, String value) {
        metadata.setClientNote(name, value);
    }

    @Override
    public void removeClientNote(String name) {
        metadata.removeClientNote(name);
    }

    @Override
    public Map<String, String> getAuthNotes() {
        return metadata.getAuthNotes();
    }

    @Override
    public void setAuthNotes(Map<String, String> authNotes) {
        metadata.setAuthNotes(authNotes);
    }

    @Override
    public void setAuthNote(String name, String value) {
        metadata.setAuthNote(name, value);
    }

    @Override
    public void removeAuthNote(String name) {
        metadata.removeAuthNote(name);
    }

    @Override
    public Map<String, CommonClientSessionModel.ExecutionStatus> getExecutionStatuses() {
        return metadata.getExecutionStatuses();
    }

    @Override
    public void setExecutionStatuses(Map<String, CommonClientSessionModel.ExecutionStatus> executionStatus) {
        metadata.setExecutionStatuses(executionStatus);
    }

    @Override
    public void setExecutionStatus(String authenticator, CommonClientSessionModel.ExecutionStatus status) {
        metadata.setExecutionStatus(authenticator, status);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof JpaAuthenticationSessionEntity)) return false;
        return Objects.equals(getId(), ((JpaAuthenticationSessionEntity) obj).getId()) &&
               Objects.equals(getTabId(), ((JpaAuthenticationSessionEntity) obj).getTabId());
    }
}
