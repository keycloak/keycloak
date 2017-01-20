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

import org.keycloak.storage.jpa.KeyUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name= "findBrokerLinkByUser", query="select link from BrokerLinkEntity link where link.userId = :userId"),
        @NamedQuery(name= "findBrokerLinkByUserAndProvider", query="select link from BrokerLinkEntity link where link.userId = :userId and link.identityProvider = :identityProvider and link.realmId = :realmId"),
        @NamedQuery(name= "findUserByBrokerLinkAndRealm", query="select link.userId from BrokerLinkEntity link where link.realmId = :realmId and link.identityProvider = :identityProvider and link.brokerUserId = :brokerUserId"),
        @NamedQuery(name= "deleteBrokerLinkByStorageProvider", query="delete from BrokerLinkEntity social where social.storageProviderId = :storageProviderId"),
        @NamedQuery(name= "deleteBrokerLinkByRealm", query="delete from BrokerLinkEntity social where social.realmId = :realmId"),
        @NamedQuery(name= "deleteBrokerLinkByRealmAndLink", query="delete from BrokerLinkEntity social where social.userId IN (select u.id from UserEntity u where realmId=:realmId and u.federationLink=:link)"),
        @NamedQuery(name= "deleteBrokerLinkByUser", query="delete from BrokerLinkEntity social where social.userId = :userId and social.realmId = :realmId")
})
@Table(name="BROKER_LINK")
@Entity
@IdClass(BrokerLinkEntity.Key.class)
public class BrokerLinkEntity {

    @Id
    @Column(name = "USER_ID")
    private String userId;

    @Id
    @Column(name = "IDENTITY_PROVIDER")
    protected String identityProvider;

    @Column(name = "REALM_ID")
    protected String realmId;

    @Column(name = "STORAGE_PROVIDER_ID")
    protected String storageProviderId;

    @Column(name = "BROKER_USER_ID")
    protected String brokerUserId;
    @Column(name = "BROKER_USERNAME")
    protected String brokerUserName;

    @Column(name = "TOKEN")
    protected String token;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        KeyUtils.assertValidKey(userId);
        this.userId = userId;
    }

    public String getIdentityProvider() {
        return identityProvider;
    }

    public void setIdentityProvider(String identityProvider) {
        this.identityProvider = identityProvider;
    }

    public String getBrokerUserId() {
        return brokerUserId;
    }

    public void setBrokerUserId(String brokerUserId) {
        this.brokerUserId = brokerUserId;
    }

    public String getBrokerUserName() {
        return brokerUserName;
    }

    public void setBrokerUserName(String brokerUserName) {
        this.brokerUserName = brokerUserName;
    }

    public String getStorageProviderId() {
        return storageProviderId;
    }

    public void setStorageProviderId(String storageProviderId) {
        this.storageProviderId = storageProviderId;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public static class Key implements Serializable {

        protected String userId;

        protected String identityProvider;

        public Key() {
        }

        public Key(String userId, String identityProvider) {
            this.userId = userId;
            this.identityProvider = identityProvider;
        }

        public String getUserId() {
            return userId;
        }

        public String getIdentityProvider() {
            return identityProvider;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (identityProvider != null ? !identityProvider.equals(key.identityProvider) : key.identityProvider != null)
                return false;
            if (userId != null ? !userId.equals(key.userId != null ? key.userId : null) : key.userId != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = userId != null ? userId.hashCode() : 0;
            result = 31 * result + (identityProvider != null ? identityProvider.hashCode() : 0);
            return result;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof BrokerLinkEntity)) return false;

        BrokerLinkEntity key = (BrokerLinkEntity) o;

        if (identityProvider != null ? !identityProvider.equals(key.identityProvider) : key.identityProvider != null)
            return false;
        if (userId != null ? !userId.equals(key.userId != null ? key.userId : null) : key.userId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = userId != null ? userId.hashCode() : 0;
        result = 31 * result + (identityProvider != null ? identityProvider.hashCode() : 0);
        return result;
    }


}
