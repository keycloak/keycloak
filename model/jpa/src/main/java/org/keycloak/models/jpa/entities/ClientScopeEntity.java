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

import org.hibernate.annotations.Nationalized;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Entity
@Table(name="CLIENT_SCOPE", uniqueConstraints = {@UniqueConstraint(columnNames = {"REALM_ID", "NAME"})})
public class ClientScopeEntity {

    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    private String id;
    @Column(name = "NAME")
    private String name;
    @Nationalized
    @Column(name = "DESCRIPTION")
    private String description;
    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "clientScope")
    Collection<ProtocolMapperEntity> protocolMappers = new ArrayList<ProtocolMapperEntity>();
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REALM_ID")
    protected RealmEntity realm;

    @Column(name="PROTOCOL")
    private String protocol;


    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "clientScope")
    protected Collection<ClientScopeAttributeEntity> attributes = new ArrayList<>();

    public RealmEntity getRealm() {
        return realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
    }

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Collection<ProtocolMapperEntity> getProtocolMappers() {
        return protocolMappers;
    }

    public void setProtocolMappers(Collection<ProtocolMapperEntity> protocolMappers) {
        this.protocolMappers = protocolMappers;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Collection<ClientScopeAttributeEntity> getAttributes() {
        return attributes;
    }

    public void setAttributes(Collection<ClientScopeAttributeEntity> attributes) {
        this.attributes = attributes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof ClientScopeEntity)) return false;

        ClientScopeEntity that = (ClientScopeEntity) o;

        if (!id.equals(that.getId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
