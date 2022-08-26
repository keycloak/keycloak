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
package org.keycloak.models.map.storage.jpa.loginFailure.entity;

import java.util.Objects;
import java.util.UUID;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UuidValidator;
import org.keycloak.models.map.loginFailure.MapUserLoginFailureEntity;
import org.keycloak.models.map.storage.jpa.JpaRootVersionedEntity;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.JsonbType;

import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_USER_LOGIN_FAILURE;

/**
 * JPA {@link MapUserLoginFailureEntity} implementation. Some fields are annotated with {@code @Column(insertable = false, updatable = false)}
 * to indicate that they are automatically generated from json fields. As such, these fields are non-insertable and non-updatable.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@Entity
@Table(name = "kc_user_login_failure",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"realmId", "userId"}
                )
})
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonbType.class)})
public class JpaUserLoginFailureEntity extends MapUserLoginFailureEntity.AbstractUserLoginFailureEntity implements JpaRootVersionedEntity {

    @Id
    @Column
    private UUID id;

    //used for implicit optimistic locking
    @Version
    @Column
    private int version;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private final JpaUserLoginFailureMetadata metadata;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private Integer entityVersion;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private String realmId;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private String userId;

    /**
     * No-argument constructor, used by hibernate to instantiate entities.
     */
    public JpaUserLoginFailureEntity() {
        this.metadata = new JpaUserLoginFailureMetadata();
    }

    public JpaUserLoginFailureEntity(DeepCloner cloner) {
        this.metadata = new JpaUserLoginFailureMetadata(cloner);
    }

    /**
     * Used by hibernate when calling cb.construct from read(QueryParameters) method.
     * It is used to select user login failure without metadata(json) field.
     */
    public JpaUserLoginFailureEntity(UUID id, int version, Integer entityVersion,  String realmId, String userId) {
        this.id = id;
        this.version = version;
        this.entityVersion = entityVersion;
        this.realmId = realmId;
        this.userId = userId;
        this.metadata = null;
    }

    public boolean isMetadataInitialized() {
        return this.metadata != null;
    }

    @Override
    public Integer getEntityVersion() {
        if (isMetadataInitialized()) return metadata.getEntityVersion();
        return this.entityVersion;
    }

    @Override
    public void setEntityVersion(Integer entityVersion) {
        this.metadata.setEntityVersion(entityVersion);
    }

    @Override
    public Integer getCurrentSchemaVersion() {
        return CURRENT_SCHEMA_VERSION_USER_LOGIN_FAILURE;
    }

    @Override
    public int getVersion() {
        return this.version;
    }

    @Override
    public String getId() {
        return this.id == null ? null : this.id.toString();
    }

    @Override
    public void setId(String id) {
        String validatedId = UuidValidator.validateAndConvert(id);
        this.id = UUID.fromString(validatedId);
    }

    @Override
    public String getRealmId() {
        if (isMetadataInitialized()) return this.metadata.getRealmId();
        return this.realmId;
    }

    @Override
    public void setRealmId(String realmId) {
        this.metadata.setRealmId(realmId);
    }

    @Override
    public String getUserId() {
        if (isMetadataInitialized()) return this.metadata.getUserId();
        return this.userId;
    }

    @Override
    public void setUserId(String userId) {
        this.metadata.setUserId(userId);
    }

    @Override
    public Long getFailedLoginNotBefore() {
        return this.metadata.getFailedLoginNotBefore();
    }

    @Override
    public void setFailedLoginNotBefore(Long failedLoginNotBefore) {
        this.metadata.setFailedLoginNotBefore(failedLoginNotBefore);
    }

    @Override
    public Integer getNumFailures() {
        return this.metadata.getNumFailures();
    }

    @Override
    public void setNumFailures(Integer numFailures) {
        this.metadata.setNumFailures(numFailures);
    }

    @Override
    public Long getLastFailure() {
        return this.metadata.getLastFailure();
    }

    @Override
    public void setLastFailure(Long lastFailure) {
        this.metadata.setLastFailure(lastFailure);
    }

    @Override
    public String getLastIPFailure() {
        return this.metadata.getLastIPFailure();
    }

    @Override
    public void setLastIPFailure(String lastIPFailure) {
        this.metadata.setLastIPFailure(lastIPFailure);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof JpaUserLoginFailureEntity)) return false;
        return Objects.equals(getId(), ((JpaUserLoginFailureEntity) obj).getId());
    }
}
