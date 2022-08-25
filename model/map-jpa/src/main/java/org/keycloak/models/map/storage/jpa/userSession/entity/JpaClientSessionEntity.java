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
package org.keycloak.models.map.storage.jpa.userSession.entity;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UuidValidator;
import org.keycloak.models.map.storage.jpa.Constants;
import org.keycloak.models.map.storage.jpa.JpaRootVersionedEntity;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.JsonbType;
import org.keycloak.models.map.userSession.MapAuthenticatedClientSessionEntity.AbstractAuthenticatedClientSessionEntity;

/**
 * Entity represents authenticated client session.
 */
@Entity
@Table(name = "kc_client_session")
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonbType.class)})
public class JpaClientSessionEntity extends AbstractAuthenticatedClientSessionEntity implements JpaRootVersionedEntity {

    @Id
    @Column
    private UUID id;

    //used for implicit optimistic locking
    @Version
    @Column
    private int version;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private final JpaClientSessionMetadata metadata;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private Integer entityVersion;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private String clientId;

    @OneToMany(mappedBy = "root", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private final Set<JpaClientSessionNoteEntity> notes = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_root")
    private JpaUserSessionEntity root;

    /**
     * No-argument constructor, used by hibernate to instantiate entities.
     */
    public JpaClientSessionEntity() {
        this.metadata = new JpaClientSessionMetadata();
    }

    public JpaClientSessionEntity(DeepCloner cloner) {
        this.metadata = new JpaClientSessionMetadata(cloner);
    }

    public void setParent(JpaUserSessionEntity root) {
        this.root = root;
    }

    @Override
    public Integer getEntityVersion() {
        return metadata.getEntityVersion();
    }

    @Override
    public void setEntityVersion(Integer entityVersion) {
        metadata.setEntityVersion(entityVersion);
    }

    @Override
    public Integer getCurrentSchemaVersion() {
        return Constants.CURRENT_SCHEMA_VERSION_CLIENT_SESSION;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public String getId() {
        return id == null ? null : id.toString();
    }

    @Override
    public void setId(String id) {
        String validatedId = UuidValidator.validateAndConvert(id);
        this.id = UUID.fromString(validatedId);
    }

    @Override
    public String getRealmId() {
        return metadata.getRealmId();
    }

    @Override
    public void setRealmId(String realmId) {
        metadata.setRealmId(realmId);
    }

    @Override
    public String getClientId() {
        return metadata.getClientId();
    }

    @Override
    public void setClientId(String clientId) {
        metadata.setClientId(clientId);
    }

    @Override
    public String getAuthMethod() {
        return metadata.getAuthMethod();
    }

    @Override
    public void setAuthMethod(String authMethod) {
        metadata.setAuthMethod(authMethod);
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
    public Long getTimestamp() {
        return metadata.getTimestamp();
    }

    @Override
    public void setTimestamp(Long timestamp) {
        metadata.setTimestamp(timestamp);
    }

    @Override
    public Long getExpiration() {
        return metadata.getExpiration();
    }

    @Override
    public void setExpiration(Long expiration) {
        metadata.setExpiration(expiration);
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
    public String getCurrentRefreshToken() {
        return metadata.getCurrentRefreshToken();
    }

    @Override
    public void setCurrentRefreshToken(String currentRefreshToken) {
        metadata.setCurrentRefreshToken(currentRefreshToken);
    }

    @Override
    public Integer getCurrentRefreshTokenUseCount() {
        return metadata.getCurrentRefreshTokenUseCount();
    }

    @Override
    public void setCurrentRefreshTokenUseCount(Integer currentRefreshTokenUseCount) {
        metadata.setCurrentRefreshTokenUseCount(currentRefreshTokenUseCount);
    }

    @Override
    public Boolean isOffline() {
        return metadata.isOffline();
    }

    @Override
    public void setOffline(Boolean offline) {
        metadata.setOffline(offline);
    }

    @Override
    public Map<String, String> getNotes() {
        return notes.stream().collect(Collectors.toMap(JpaClientSessionNoteEntity::getName, JpaClientSessionNoteEntity::getValue));
    }

    @Override
    public void setNotes(Map<String, String> notes) {
        this.notes.clear();
        if (notes == null) return;
        for (Map.Entry<String, String> entry : notes.entrySet()) {
            setNote(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public String getNote(String name) {
        return notes.stream()
                .filter(obj -> Objects.equals(obj.getName(), name))
                .findFirst()
                .map(JpaClientSessionNoteEntity::getValue)
                .orElse(null);
    }

    @Override
    public Boolean removeNote(String name) {
        return notes.removeIf(obj -> Objects.equals(obj.getName(), name));
    }

    @Override
    public void setNote(String name, String value) {
        removeNote(name);
        if (name == null || value == null || value.trim().isEmpty()) return;
        notes.add(new JpaClientSessionNoteEntity(this, name, value));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof JpaClientSessionEntity)) return false;
        return Objects.equals(getId(), ((JpaClientSessionEntity) obj).getId());
    }
}
