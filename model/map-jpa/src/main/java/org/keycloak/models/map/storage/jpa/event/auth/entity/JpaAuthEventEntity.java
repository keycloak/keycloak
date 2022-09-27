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
package org.keycloak.models.map.storage.jpa.event.auth.entity;

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
import org.keycloak.events.EventType;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UuidValidator;
import org.keycloak.models.map.events.MapAuthEventEntity;
import org.keycloak.models.map.storage.jpa.JpaRootVersionedEntity;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.JsonbType;

import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_AUTH_EVENT;

/**
 * JPA {@link MapAuthEventEntity} implementation. Some fields are annotated with {@code @Column(insertable = false, updatable = false)}
 * to indicate that they are automatically generated from json fields. As such, these fields are non-insertable and non-updatable.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@Entity
@Table(name = "kc_auth_event")
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonbType.class)})
public class JpaAuthEventEntity extends MapAuthEventEntity.AbstractAuthEventEntity implements JpaRootVersionedEntity {

    @Id
    @Column
    private UUID id;

    //used for implicit optimistic locking
    @Version
    @Column
    private int version;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private final JpaAuthEventMetadata metadata;

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

    @OneToMany(mappedBy = "root", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private final Set<JpaAuthEventDetailEntity> details = new HashSet<>();

    /**
     * No-argument constructor, used by hibernate to instantiate entities.
     */
    public JpaAuthEventEntity() {
        this.metadata = new JpaAuthEventMetadata();
    }

    public JpaAuthEventEntity(final DeepCloner cloner) {
        this.metadata = new JpaAuthEventMetadata(cloner);
    }

    public boolean isMetadataInitialized() {
        return metadata != null;
    }

    @Override
    public Integer getCurrentSchemaVersion() {
        return CURRENT_SCHEMA_VERSION_AUTH_EVENT;
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
    public void setTimestamp(final Long timestamp) {
        this.metadata.setTimestamp(timestamp);
    }

    @Override
    public String getClientId() {
        return this.metadata.getClientId();
    }

    @Override
    public void setClientId(final String clientId) {
        this.metadata.setClientId(clientId);
    }

    @Override
    public Map<String, String> getDetails() {
        return this.details.stream().collect(Collectors.toMap(JpaAuthEventDetailEntity::getName, JpaAuthEventDetailEntity::getValue));
    }

    @Override
    public void setDetails(final Map<String, String> details) {
        this.details.clear();
        if (details != null) {
            details.forEach((key, value) -> this.details.add(new JpaAuthEventDetailEntity(this, key, value)));
        }
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
    public String getIpAddress() {
        return this.metadata.getIpAddress();
    }

    @Override
    public void setIpAddress(final String ipAddress) {
        this.metadata.setIpAddress(ipAddress);
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
    public String getSessionId() {
        return this.metadata.getSessionId();
    }

    @Override
    public void setSessionId(final String sessionId) {
        this.metadata.setSessionId(sessionId);
    }

    @Override
    public String getUserId() {
        return this.metadata.getUserId();
    }

    @Override
    public void setUserId(final String userId) {
        this.metadata.setUserId(userId);
    }

    @Override
    public EventType getType() {
        return this.metadata.getType();
    }

    @Override
    public void setType(final EventType type) {
        this.metadata.setType(type);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof JpaAuthEventEntity)) return false;
        return Objects.equals(getId(), ((JpaAuthEventEntity) obj).getId());
    }
}
