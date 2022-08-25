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
package org.keycloak.models.map.storage.jpa.singleUseObject.entity;

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
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UuidValidator;
import org.keycloak.models.map.singleUseObject.MapSingleUseObjectEntity;
import org.keycloak.models.map.storage.jpa.JpaRootVersionedEntity;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.JsonbType;

import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_SINGLE_USE_OBJECT;

/**
 * JPA {@link MapSingleUseObjectEntity} implementation. Some fields are annotated with {@code @Column(insertable = false, updatable = false)}
 * to indicate that they are automatically generated from json fields. As such, these fields are non-insertable and non-updatable.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@Entity
@Table(name = "kc_single_use_obj")
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonbType.class)})
public class JpaSingleUseObjectEntity extends MapSingleUseObjectEntity.AbstractSingleUseObjectEntity implements JpaRootVersionedEntity {

    @Id
    @Column
    private UUID id;

    //used for implicit optimistic locking
    @Version
    @Column
    private int version;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private final JpaSingleUseObjectMetadata metadata;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private Integer entityVersion;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private String objectKey;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private Long expiration;

    @OneToMany(mappedBy = "root", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private final Set<JpaSingleUseObjectNoteEntity> notes = new HashSet<>();

    /**
     * No-argument constructor, used by hibernate to instantiate entities.
     */
    public JpaSingleUseObjectEntity() {
        this.metadata = new JpaSingleUseObjectMetadata();
    }

    public JpaSingleUseObjectEntity(final DeepCloner cloner) {
        this.metadata = new JpaSingleUseObjectMetadata(cloner);
    }

    public boolean isMetadataInitialized() {
        return this.metadata != null;
    }

    @Override
    public int getVersion() {
        return this.version;
    }

    @Override
    public Integer getEntityVersion() {
        if (this.isMetadataInitialized()) return this.metadata.getEntityVersion();
        return this.entityVersion;
    }

    @Override
    public void setEntityVersion(Integer entityVersion) {
        this.metadata.setEntityVersion(entityVersion);
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
    public String getObjectKey() {
        if (this.isMetadataInitialized()) return this.metadata.getObjectKey();
        return this.objectKey;
    }

    @Override
    public void setObjectKey(final String objectKey) {
        this.metadata.setObjectKey(objectKey);
    }

    @Override
    public Integer getCurrentSchemaVersion() {
        return CURRENT_SCHEMA_VERSION_SINGLE_USE_OBJECT;
    }

    @Override
    public String getActionId() {
        return this.metadata.getActionId();
    }

    @Override
    public void setActionId(String actionId) {
        this.metadata.setActionId(actionId);
    }

    @Override
    public String getActionVerificationNonce() {
        return this.metadata.getActionVerificationNonce();
    }

    @Override
    public void setActionVerificationNonce(String actionVerificationNonce) {
        this.metadata.setActionVerificationNonce(actionVerificationNonce);
    }

    @Override
    public Map<String, String> getNotes() {
        return this.notes.stream()
                .collect(Collectors.toMap(JpaSingleUseObjectNoteEntity::getName, JpaSingleUseObjectNoteEntity::getValue));
    }

    @Override
    public String getNote(String name) {
        return this.notes.stream().filter(note -> Objects.equals(note.getName(), name))
                .findFirst()
                .map(JpaSingleUseObjectNoteEntity::getValue)
                .orElse(null);
    }

    @Override
    public void setNotes(Map<String, String> notes) {
        this.notes.clear();
        if (notes != null) {
            notes.forEach(this::setNote);
        }
    }

    @Override
    public void setNote(String name, String value) {
        if (name != null) {
            this.notes.removeIf(note -> Objects.equals(note.getName(), name));
            if (value != null && !value.trim().isEmpty())
                this.notes.add(new JpaSingleUseObjectNoteEntity(this, name, value));
        }
    }

    @Override
    public String getUserId() {
        return this.metadata.getUserId();
    }

    @Override
    public void setUserId(String userId) {
        this.metadata.setUserId(userId);
    }

    @Override
    public Long getExpiration() {
        if (this.isMetadataInitialized()) return this.metadata.getExpiration();
        return this.expiration;
    }

    @Override
    public void setExpiration(Long expiration) {
        this.metadata.setExpiration(expiration);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof JpaSingleUseObjectEntity)) return false;
        return Objects.equals(getId(), ((JpaSingleUseObjectEntity) obj).getId());
    }
}
