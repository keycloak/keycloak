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

import java.util.ArrayList;
import java.util.Collection;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@Entity
@Table(name="FED_USER_CONSENT", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"USER_ID", "CLIENT_ID"})
})
@NamedQueries({
        @NamedQuery(name="userFederatedConsentByUserAndClient", query="select consent from FederatedUserConsentEntity consent where consent.userId = :userId and consent.clientId = :clientId"),
        @NamedQuery(name="userFederatedConsentByUserAndExternalClient", query="select consent from FederatedUserConsentEntity consent where consent.userId = :userId and consent.clientStorageProvider = :clientStorageProvider and consent.externalClientId = :externalClientId"),
        @NamedQuery(name="userFederatedConsentsByUser", query="select consent from FederatedUserConsentEntity consent where consent.userId = :userId"),
        @NamedQuery(name="deleteFederatedUserConsentsByRealm", query="delete from FederatedUserConsentEntity consent where consent.realmId=:realmId"),
        @NamedQuery(name="deleteFederatedUserConsentsByStorageProvider", query="delete from FederatedUserConsentEntity e where e.storageProviderId=:storageProviderId"),
        @NamedQuery(name="deleteFederatedUserConsentsByUser", query="delete from FederatedUserConsentEntity consent where consent.userId = :userId and consent.realmId = :realmId"),
        @NamedQuery(name="deleteFederatedUserConsentsByClient", query="delete from FederatedUserConsentEntity consent where consent.clientId = :clientId"),
        @NamedQuery(name="deleteFederatedUserConsentsByExternalClient", query="delete from FederatedUserConsentEntity consent where consent.clientStorageProvider = :clientStorageProvider and consent.externalClientId = :externalClientId"),
        @NamedQuery(name="deleteFederatedUserConsentsByClientStorageProvider", query="delete from FederatedUserConsentEntity consent where consent.clientStorageProvider = :clientStorageProvider"),
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

    @Column(name="CLIENT_STORAGE_PROVIDER")
    protected String clientStorageProvider;

    @Column(name="EXTERNAL_CLIENT_ID")
    protected String externalClientId;

    @Column(name = "CREATED_DATE")
    private Long createdDate;

    @Column(name = "LAST_UPDATED_DATE")
    private Long lastUpdatedDate;



    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "userConsent")
    Collection<FederatedUserConsentClientScopeEntity> grantedClientScopes = new ArrayList<>();

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

    public String getClientStorageProvider() {
        return clientStorageProvider;
    }

    public void setClientStorageProvider(String clientStorageProvider) {
        this.clientStorageProvider = clientStorageProvider;
    }

    public String getExternalClientId() {
        return externalClientId;
    }

    public void setExternalClientId(String externalClientId) {
        this.externalClientId = externalClientId;
    }

    public Collection<FederatedUserConsentClientScopeEntity> getGrantedClientScopes() {
        return grantedClientScopes;
    }

    public void setGrantedClientScopes(Collection<FederatedUserConsentClientScopeEntity> grantedClientScopes) {
        this.grantedClientScopes = grantedClientScopes;
    }

    public Long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Long createdDate) {
        this.createdDate = createdDate;
    }

    public Long getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(Long lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
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
