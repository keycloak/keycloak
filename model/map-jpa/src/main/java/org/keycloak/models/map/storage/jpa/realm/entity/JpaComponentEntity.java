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
package org.keycloak.models.map.storage.jpa.realm.entity;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.common.UuidValidator;
import org.keycloak.models.map.realm.entity.MapComponentEntity;
import org.keycloak.models.map.storage.jpa.Constants;
import org.keycloak.models.map.storage.jpa.JpaRootVersionedEntity;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.JsonbType;


/**
 * JPA {@link MapComponentEntity} implementation. Some fields are annotated with {@code @Column(insertable = false, updatable = false)}
 * to indicate that they are automatically generated from json fields. As such, these fields are non-insertable and non-updatable.
 * <p/>
 * Components are independent (i.e. a component doesn't depend on another component) and can be manipulated directly via
 * the component endpoints. Because of that, this entity  implements {@link JpaRootVersionedEntity} instead of
 * {@link org.keycloak.models.map.storage.jpa.JpaChildEntity}. This prevents {@link javax.persistence.OptimisticLockException}s
 * when different components in the same realm are being manipulated at the same time - for example, when multiple components
 * are being added to the realm by different threads.
 * <p/>
 * By implementing {@link JpaRootVersionedEntity}, this entity will enforce optimistic locking, which can lead to
 * {@link javax.persistence.OptimisticLockException} if more than one thread attempts to modify the <b>same</b> component
 * at the same time.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@Entity
@Table(name = "kc_component")
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonbType.class)})
public class JpaComponentEntity extends UpdatableEntity.Impl implements MapComponentEntity, JpaRootVersionedEntity {

    @Id
    @Column
    private UUID id;

    //used for implicit optimistic locking
    @Version
    @Column
    private int version;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private String providerType;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private final JpaComponentMetadata metadata;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="fk_root")
    private JpaRealmEntity root;

    /**
     * No-argument constructor, used by hibernate to instantiate entities.
     */
    public JpaComponentEntity() {
        this.metadata = new JpaComponentMetadata();
    }

    public JpaComponentEntity(DeepCloner cloner) {
        this.metadata = new JpaComponentMetadata(cloner);
    }

    public void setParent(JpaRealmEntity root) {
        this.root = root;
    }

    public boolean isMetadataInitialized() {
        return this.metadata != null;
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
    public Integer getEntityVersion() {
        return this.metadata.getEntityVersion();
    }

    @Override
    public void setEntityVersion(Integer version) {
        this.metadata.setEntityVersion(version);
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public Integer getCurrentSchemaVersion() {
        return Constants.CURRENT_SCHEMA_VERSION_COMPONENT;
    }

    @Override
    public String getName() {
        return this.metadata.getName();
    }

    @Override
    public void setName(String name) {
        this.metadata.setName(name);
    }

    @Override
    public String getProviderId() {
        return this.metadata.getProviderId();
    }

    @Override
    public void setProviderId(String providerId) {
        this.metadata.setProviderId(providerId);
    }

    @Override
    public String getProviderType() {
        if (this.isMetadataInitialized()) return this.metadata.getProviderType();
        return this.providerType;
    }

    @Override
    public void setProviderType(String providerType) {
        this.metadata.setProviderType(providerType);
    }

    @Override
    public String getSubType() {
        return this.metadata.getSubType();
    }

    @Override
    public void setSubType(String subType) {
        this.metadata.setSubType(subType);
    }

    @Override
    public String getParentId() {
        return this.metadata.getParentId();
    }

    @Override
    public void setParentId(String parentId) {
        this.metadata.setParentId(parentId);
    }

    @Override
    public Map<String, List<String>> getConfig() {
        return this.metadata.getConfig();
    }

    @Override
    public void setConfig(Map<String, List<String>> config) {
        this.metadata.setConfig(config);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof JpaComponentEntity)) return false;
        return Objects.equals(getId(), ((JpaComponentEntity) obj).getId());
    }
}
