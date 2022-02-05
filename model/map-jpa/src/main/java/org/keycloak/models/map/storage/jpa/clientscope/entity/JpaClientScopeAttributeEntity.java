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
package org.keycloak.models.map.storage.jpa.clientscope.entity;

import java.util.Objects;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.hibernate.annotations.Nationalized;
import org.keycloak.models.map.storage.jpa.JpaChildEntity;

@Entity
@Table(name = "client_scope_attribute")
public class JpaClientScopeAttributeEntity implements JpaChildEntity<JpaClientScopeEntity> {

    @Id
    @Column
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="fk_client_scope")
    private JpaClientScopeEntity clientScope;

    @Column
    private String name;

    @Nationalized
    @Column
    private String value;

    public JpaClientScopeAttributeEntity() {
    }

    public JpaClientScopeAttributeEntity(JpaClientScopeEntity clientScope, String name, String value) {
        this.clientScope = clientScope;
        this.name = name;
        this.value = value;
    }

    public UUID getId() {
        return id;
    }

    public JpaClientScopeEntity getClientScope() {
        return clientScope;
    }

    public void setClientScope(JpaClientScopeEntity clientScope) {
        this.clientScope = clientScope;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
 
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof JpaClientScopeAttributeEntity)) return false;
        JpaClientScopeAttributeEntity that = (JpaClientScopeAttributeEntity) obj;
        return Objects.equals(getClientScope(), that.getClientScope()) &&
               Objects.equals(getName(), that.getName()) &&
               Objects.equals(getValue(), that.getValue());
    }

    @Override
    public JpaClientScopeEntity getParent() {
        return getClientScope();
    }
}
