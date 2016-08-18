/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.jpa.entities;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * @author <a href="mailto:bburke@redhat.com">Bill Burke</a>
 */
@NamedQueries({
        @NamedQuery(name="getComponents", query="select attr from ComponentEntity attr where attr.realm = :realm"),
        @NamedQuery(name="getComponentsByParentAndType", query="select attr from ComponentEntity attr where attr.realm = :realm and attr.providerType = :providerType and attr.parentId = :parentId"),
        @NamedQuery(name="getComponentByParent", query="select attr from ComponentEntity attr where attr.realm = :realm and attr.parentId = :parentId"),
        @NamedQuery(name="getComponentIdsByParent", query="select attr.id from ComponentEntity attr where attr.realm = :realm and attr.parentId = :parentId"),
        @NamedQuery(name="deleteComponentByRealm", query="delete from  ComponentEntity c where c.realm = :realm"),
        @NamedQuery(name="deleteComponentByParent", query="delete from  ComponentEntity c where c.parentId = :parentId")
})
@Entity
@Table(name="COMPONENT")
public class ComponentEntity {

    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REALM_ID")
    protected RealmEntity realm;

    @Column(name="NAME")
    protected String name;

    @Column(name="PROVIDER_TYPE")
    protected String providerType;

    @Column(name="PROVIDER_ID")
    protected String providerId;

    @Column(name="PARENT_ID")
    protected String parentId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProviderType() {
        return providerType;
    }

    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public RealmEntity getRealm() {
        return realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof ComponentEntity)) return false;

        ComponentEntity that = (ComponentEntity) o;

        if (!id.equals(that.getId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
