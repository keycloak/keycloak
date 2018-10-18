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

package org.keycloak.models.jpa.entities;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@NamedQueries({
        @NamedQuery(name="deleteUserConsentClientScopesByRealm", query="delete from UserConsentClientScopeEntity grantedScope where grantedScope.userConsent IN (select consent from UserConsentEntity consent where consent.user IN (select user from UserEntity user where user.realmId = :realmId))"),
        @NamedQuery(name="deleteUserConsentClientScopesByRealmAndLink", query="delete from UserConsentClientScopeEntity grantedScope where grantedScope.userConsent IN (select consent from UserConsentEntity consent where consent.user IN (select u from UserEntity u where u.realmId=:realmId and u.federationLink=:link))"),
        @NamedQuery(name="deleteUserConsentClientScopesByUser", query="delete from UserConsentClientScopeEntity grantedScope where grantedScope.userConsent IN (select consent from UserConsentEntity consent where consent.user = :user)"),
        @NamedQuery(name="deleteUserConsentClientScopesByClientScope", query="delete from UserConsentClientScopeEntity grantedScope where grantedScope.scopeId = :scopeId"),
        @NamedQuery(name="deleteUserConsentClientScopesByClient", query="delete from UserConsentClientScopeEntity grantedScope where grantedScope.userConsent IN (select consent from UserConsentEntity consent where consent.clientId = :clientId)"),
        @NamedQuery(name="deleteUserConsentClientScopesByExternalClient", query="delete from UserConsentClientScopeEntity grantedScope where grantedScope.userConsent IN (select consent from UserConsentEntity consent where consent.clientStorageProvider = :clientStorageProvider and consent.externalClientId = :externalClientId)"),
        @NamedQuery(name="deleteUserConsentClientScopesByClientStorageProvider", query="delete from UserConsentClientScopeEntity grantedScope where grantedScope.userConsent IN (select consent from UserConsentEntity consent where consent.clientStorageProvider = :clientStorageProvider)"),
})
@Entity
@Table(name="USER_CONSENT_CLIENT_SCOPE")
@IdClass(UserConsentClientScopeEntity.Key.class)
public class UserConsentClientScopeEntity {

    @Id
    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name = "USER_CONSENT_ID")
    protected UserConsentEntity userConsent;

    @Id
    @Column(name="SCOPE_ID")
    protected String scopeId;

    public UserConsentEntity getUserConsent() {
        return userConsent;
    }

    public void setUserConsent(UserConsentEntity userConsent) {
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
        if (!(o instanceof UserConsentClientScopeEntity)) return false;

        UserConsentClientScopeEntity that = (UserConsentClientScopeEntity)o;
        UserConsentClientScopeEntity.Key myKey = new UserConsentClientScopeEntity.Key(this.userConsent, this.scopeId);
        UserConsentClientScopeEntity.Key hisKey = new UserConsentClientScopeEntity.Key(that.userConsent, that.scopeId);
        return myKey.equals(hisKey);
    }

    @Override
    public int hashCode() {
        UserConsentClientScopeEntity.Key myKey = new UserConsentClientScopeEntity.Key(this.userConsent, this.scopeId);
        return myKey.hashCode();
    }

    public static class Key implements Serializable {

        protected UserConsentEntity userConsent;

        protected String scopeId;

        public Key() {
        }

        public Key(UserConsentEntity userConsent, String scopeId) {
            this.userConsent = userConsent;
            this.scopeId = scopeId;
        }

        public UserConsentEntity getUserConsent() {
            return userConsent;
        }

        public String getScopeId() {
            return scopeId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            UserConsentClientScopeEntity.Key key = (UserConsentClientScopeEntity.Key) o;

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
