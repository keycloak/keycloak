/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@NamedQueries({
        @NamedQuery(name="deleteFederatedUserConsentClientScopesByRealm", query="delete from FederatedUserConsentClientScopeEntity grantedScope where grantedScope.userConsent IN (select consent from FederatedUserConsentEntity consent where consent.realmId = :realmId)"),
        @NamedQuery(name="deleteFederatedUserConsentClientScopesByUser", query="delete from FederatedUserConsentClientScopeEntity grantedScope where grantedScope.userConsent IN (select consent from FederatedUserConsentEntity consent where consent.userId = :userId and consent.realmId = :realmId)"),
        @NamedQuery(name="deleteFederatedUserConsentClientScopesByStorageProvider", query="delete from FederatedUserConsentClientScopeEntity grantedScope where grantedScope.userConsent IN (select consent from FederatedUserConsentEntity consent where consent.storageProviderId = :storageProviderId)"),
        @NamedQuery(name="deleteFederatedUserConsentClientScopesByClientScope", query="delete from FederatedUserConsentClientScopeEntity grantedScope where grantedScope.scopeId = :scopeId"),
        @NamedQuery(name="deleteFederatedUserConsentClientScopesByClient", query="delete from FederatedUserConsentClientScopeEntity grantedScope where grantedScope.userConsent IN (select consent from FederatedUserConsentEntity consent where consent.clientId = :clientId)"),
        @NamedQuery(name="deleteFederatedUserConsentClientScopesByExternalClient", query="delete from FederatedUserConsentClientScopeEntity grantedScope where grantedScope.userConsent IN (select consent from FederatedUserConsentEntity consent where consent.clientStorageProvider = :clientStorageProvider and consent.externalClientId = :externalClientId)"),
        @NamedQuery(name="deleteFederatedUserConsentClientScopesByClientStorageProvider", query="delete from FederatedUserConsentClientScopeEntity grantedScope where grantedScope.userConsent IN (select consent from FederatedUserConsentEntity consent where consent.clientStorageProvider = :clientStorageProvider)"),
})
@Entity
@Table(name="FED_USER_CONSENT_CL_SCOPE")
@IdClass(FederatedUserConsentClientScopeEntity.Key.class)
public class FederatedUserConsentClientScopeEntity {

    @Id
    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name = "USER_CONSENT_ID")
    protected FederatedUserConsentEntity userConsent;

    @Id
    @Column(name="SCOPE_ID")
    protected String scopeId;

    public FederatedUserConsentEntity getUserConsent() {
        return userConsent;
    }

    public void setUserConsent(FederatedUserConsentEntity userConsent) {
        this.userConsent = userConsent;
    }

    public String getScopeId() {
        return scopeId;
    }

    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof  FederatedUserConsentClientScopeEntity)) return false;

        FederatedUserConsentClientScopeEntity that = ( FederatedUserConsentClientScopeEntity)o;
        FederatedUserConsentClientScopeEntity.Key myKey = new  FederatedUserConsentClientScopeEntity.Key(this.userConsent, this.scopeId);
        FederatedUserConsentClientScopeEntity.Key hisKey = new  FederatedUserConsentClientScopeEntity.Key(that.userConsent, that.scopeId);
        return myKey.equals(hisKey);
    }

    @Override
    public int hashCode() {
        FederatedUserConsentClientScopeEntity.Key myKey = new FederatedUserConsentClientScopeEntity.Key(this.userConsent, this.scopeId);
        return myKey.hashCode();
    }

    public static class Key implements Serializable {

        protected FederatedUserConsentEntity userConsent;

        protected String scopeId;

        public Key() {
        }

        public Key(FederatedUserConsentEntity userConsent, String scopeId) {
            this.userConsent = userConsent;
            this.scopeId = scopeId;
        }

        public FederatedUserConsentEntity getUserConsent() {
            return userConsent;
        }

        public String getScopeId() {
            return scopeId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FederatedUserConsentClientScopeEntity.Key key = (FederatedUserConsentClientScopeEntity.Key) o;

            if (userConsent != null ? !userConsent.getId().equals(key.userConsent != null ? key.userConsent.getId() : null) : key.userConsent != null) return false;
            if (scopeId != null ? !scopeId.equals(key.scopeId) : key.scopeId != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = userConsent != null ? userConsent.getId().hashCode() : 0;
            result = 31 * result + (scopeId != null ? scopeId.hashCode() : 0);
            return result;
        }
    }
}
