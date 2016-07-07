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

package org.keycloak.storage.jpa.entity;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@Entity
@Table(name="FED_USER_CONSENT", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"USER_ID", "CLIENT_ID"})
})
@NamedQueries({
        @NamedQuery(name="userFederatedConsentByUserAndClient", query="select consent from FederatedUserConsentEntity consent where consent.userId = :userId and consent.clientId = :clientId"),
        @NamedQuery(name="userFederatedConsentsByUser", query="select consent from FederatedUserConsentEntity consent where consent.userId = :userId"),
        @NamedQuery(name="deleteFederatedUserConsentsByRealm", query="delete from FederatedUserConsentEntity consent where consent.realmId=:realmId"),
        @NamedQuery(name="deleteFederatedUserConsentsByStorageProvider", query="delete from FederatedUserConsentEntity e where e.storageProviderId=:storageProviderId"),
        @NamedQuery(name="deleteFederatedUserConsentsByUser", query="delete from FederatedUserConsentEntity consent where consent.userId = :userId and consent.realmId = :realmId"),
        @NamedQuery(name="deleteFederatedUserConsentsByClient", query="delete from FederatedUserConsentEntity consent where consent.clientId = :clientId"),
})
public class FederatedUserConsentEntity {

    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String id;

    @Column(name = "USER_ID")
    protected String userId;

    @Column(name = "REALM_ID")
    protected String realmId;

    @Column(name = "STORAGE_PROVIDER_ID")
    protected String storageProviderId;

    @Column(name="CLIENT_ID")
    protected String clientId;

    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "userConsent")
    Collection<FederatedUserConsentRoleEntity> grantedRoles = new ArrayList<FederatedUserConsentRoleEntity>();

    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "userConsent")
    Collection<FederatedUserConsentProtocolMapperEntity> grantedProtocolMappers = new ArrayList<FederatedUserConsentProtocolMapperEntity>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getStorageProviderId() {
        return storageProviderId;
    }

    public void setStorageProviderId(String storageProviderId) {
        this.storageProviderId = storageProviderId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Collection<FederatedUserConsentRoleEntity> getGrantedRoles() {
        return grantedRoles;
    }

    public void setGrantedRoles(Collection<FederatedUserConsentRoleEntity> grantedRoles) {
        this.grantedRoles = grantedRoles;
    }

    public Collection<FederatedUserConsentProtocolMapperEntity> getGrantedProtocolMappers() {
        return grantedProtocolMappers;
    }

    public void setGrantedProtocolMappers(Collection<FederatedUserConsentProtocolMapperEntity> grantedProtocolMappers) {
        this.grantedProtocolMappers = grantedProtocolMappers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof FederatedUserConsentEntity)) return false;

        FederatedUserConsentEntity that = (FederatedUserConsentEntity) o;

        if (!id.equals(that.getId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
