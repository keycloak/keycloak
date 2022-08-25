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
package org.keycloak.models.map.storage.jpa.event.admin.entity;

import java.util.Objects;
import java.util.UUID;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UuidValidator;
import org.keycloak.models.map.events.MapAdminEventEntity;
import org.keycloak.models.map.storage.jpa.JpaRootVersionedEntity;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.JsonbType;

import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_ADMIN_EVENT;

/**
 * JPA {@link MapAdminEventEntity} implementation. Some fields are annotated with {@code @Column(insertable = false, updatable = false)}
 * to indicate that they are automatically generated from json fields. As such, these fields are non-insertable and non-updatable.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@Entity
@Table(name = "kc_admin_event")
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonbType.class)})
public class JpaAdminEventEntity extends MapAdminEventEntity.AbstractAdminEventEntity implements JpaRootVersionedEntity {

    @Id
    @Column
    private UUID id;

    //used for implicit optimistic locking
    @Version
    @Column
    private int version;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private final JpaAdminEventMetadata metadata;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private Integer entityVersion;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private String realmId;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private Long timestamp;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private Long expiration;

    /**
     * No-argument constructor, used by hibernate to instantiate entities.
     */
    public JpaAdminEventEntity() {
        this.metadata = new JpaAdminEventMetadata();
    }

    public JpaAdminEventEntity(final DeepCloner cloner) {
        this.metadata = new JpaAdminEventMetadata(cloner);
    }

    public boolean isMetadataInitialized() {
        return metadata != null;
    }

    @Override
    public Integer getEntityVersion() {
        if (this.isMetadataInitialized()) return metadata.getEntityVersion();
        return this.entityVersion;
    }

    @Override
    public void setEntityVersion(final Integer entityVersion) {
        this.metadata.setEntityVersion(entityVersion);
    }

    @Override
    public int getVersion() {
        return this.version;
    }

    @Override
    public String getId() {
        return id == null ? null : id.toString();
    }

    @Override
    public void setId(final String id) {
        String validatedId = UuidValidator.validateAndConvert(id);
        this.id = UUID.fromString(validatedId);
    }

    @Override
    public Integer getCurrentSchemaVersion() {
        return CURRENT_SCHEMA_VERSION_ADMIN_EVENT;
    }

    @Override
    public Long getExpiration() {
        if (this.isMetadataInitialized()) return this.metadata.getExpiration();
        return this.expiration;
    }

    @Override
    public void setExpiration(final Long expiration) {
        this.metadata.setExpiration(expiration);
    }

    @Override
    public Long getTimestamp() {
        if (this.isMetadataInitialized()) return this.metadata.getTimestamp();
        return this.timestamp;
    }

    @Override
    public void setTimestamp(final Long time) {
        this.metadata.setTimestamp(time);
    }

    @Override
    public String getRealmId() {
        if (this.isMetadataInitialized()) return this.metadata.getRealmId();
        return this.realmId;
    }

    @Override
    public void setRealmId(final String realmId) {
        this.metadata.setRealmId(realmId);
    }

    @Override
    public OperationType getOperationType() {
        return this.metadata.getOperationType();
    }

    @Override
    public void setOperationType(final OperationType operationType) {
        this.metadata.setOperationType(operationType);
    }

    @Override
    public String getResourcePath() {
        return this.metadata.getResourcePath();
    }

    @Override
    public void setResourcePath(final String resourcePath) {
        this.metadata.setResourcePath(resourcePath);
    }

    @Override
    public String getResourceType() {
        return this.metadata.getResourceType();
    }

    @Override
    public void setResourceType(final String resourceType) {
        this.metadata.setResourceType(resourceType);
    }

    @Override
    public String getRepresentation() {
        return this.metadata.getRepresentation();
    }

    @Override
    public void setRepresentation(final String representation) {
        this.metadata.setRepresentation(representation);
    }

    @Override
    public String getError() {
        return this.metadata.getError();
    }

    @Override
    public void setError(final String error) {
        this.metadata.setError(error);
    }

    @Override
    public String getAuthClientId() {
        return this.metadata.getAuthClientId();
    }

    @Override
    public void setAuthClientId(final String clientId) {
        this.metadata.setAuthClientId(clientId);
    }

    @Override
    public String getAuthRealmId() {
        return this.metadata.getAuthRealmId();
    }

    @Override
    public void setAuthRealmId(final String realmId) {
        this.metadata.setAuthRealmId(realmId);
    }

    @Override
    public String getAuthUserId() {
        return this.metadata.getAuthUserId();
    }

    @Override
    public void setAuthUserId(final String userId) {
        this.metadata.setAuthUserId(userId);
    }

    @Override
    public String getAuthIpAddress() {
        return this.metadata.getAuthIpAddress();
    }

    @Override
    public void setAuthIpAddress(final String ipAddress) {
        this.metadata.setAuthIpAddress(ipAddress);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof JpaAdminEventEntity)) return false;
        return Objects.equals(getId(), ((JpaAdminEventEntity) obj).getId());
    }
}
