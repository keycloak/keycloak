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
package org.keycloak.models.map.storage.jpa.user.entity;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.storage.jpa.JpaChildEntity;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.JsonbType;
import org.keycloak.models.map.user.MapUserConsentEntity;

/**
 * JPA {@link MapUserConsentEntity} implementation. Some fields are annotated with {@code @Column(insertable = false, updatable = false)}
 * to indicate that they are automatically generated from json fields. As such, these fields are non-insertable and non-updatable.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@Entity
@Table(name = "kc_user_consent",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"clientId"})
        })
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonbType.class)})
public class JpaUserConsentEntity extends UpdatableEntity.Impl implements MapUserConsentEntity, JpaChildEntity<JpaUserEntity> {

    @Id
    @Column
    @GeneratedValue
    private UUID id;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private String clientId;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private final JpaUserConsentMetadata metadata;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="fk_root")
    private JpaUserEntity root;

    public JpaUserConsentEntity() {
        this.metadata = new JpaUserConsentMetadata();
    }

    public JpaUserConsentEntity(final DeepCloner cloner) {
        this.metadata = new JpaUserConsentMetadata(cloner);
    }

    public Integer getEntityVersion() {
        return this.metadata.getEntityVersion();
    }

    public void setEntityVersion(Integer version) {
        this.metadata.setEntityVersion(version);
    }

    @Override
    public JpaUserEntity getParent() {
        return this.root;
    }

    public void setParent(final JpaUserEntity root) {
        this.root = root;
    }

    @Override
    public String getClientId() {
        return this.metadata.getClientId();
    }

    @Override
    public void setClientId(String clientId) {
        this.metadata.setClientId(clientId);
    }

    @Override
    public Set<String> getGrantedClientScopesIds() {
        return this.metadata.getGrantedClientScopesIds();
    }

    @Override
    public void addGrantedClientScopesId(String scope) {
        this.metadata.addGrantedClientScopesId(scope);
    }

    @Override
    public void setGrantedClientScopesIds(Set<String> scopesIds) {
        this.metadata.setGrantedClientScopesIds(scopesIds);
    }

    @Override
    public void removeGrantedClientScopesId(String scopes) {
        this.metadata.removeGrantedClientScopesId(scopes);
    }

    @Override
    public Long getCreatedDate() {
        return this.metadata.getCreatedDate();
    }

    @Override
    public void setCreatedDate(Long createdDate) {
        this.metadata.setCreatedDate(createdDate);
    }

    @Override
    public Long getLastUpdatedDate() {
        return this.metadata.getLastUpdatedDate();
    }

    @Override
    public void setLastUpdatedDate(Long lastUpdatedDate) {
        this.metadata.setLastUpdatedDate(lastUpdatedDate);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof JpaUserConsentEntity)) return false;
        return Objects.equals(id, ((JpaUserConsentEntity) obj).id);
    }

}
