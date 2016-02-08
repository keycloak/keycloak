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
import java.io.Serializable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name= "findFederatedIdentityByUser", query="select link from FederatedIdentityEntity link where link.user = :user"),
        @NamedQuery(name= "findFederatedIdentityByUserAndProvider", query="select link from FederatedIdentityEntity link where link.user = :user and link.identityProvider = :identityProvider"),
        @NamedQuery(name= "findUserByFederatedIdentityAndRealm", query="select link.user from FederatedIdentityEntity link where link.realmId = :realmId and link.identityProvider = :identityProvider and link.userId = :userId"),
        @NamedQuery(name= "deleteFederatedIdentityByRealm", query="delete from FederatedIdentityEntity social where social.user IN (select u from UserEntity u where realmId=:realmId)"),
        @NamedQuery(name= "deleteFederatedIdentityByRealmAndLink", query="delete from FederatedIdentityEntity social where social.user IN (select u from UserEntity u where realmId=:realmId and u.federationLink=:link)"),
        @NamedQuery(name= "deleteFederatedIdentityByUser", query="delete from FederatedIdentityEntity social where social.user = :user")
})
@Table(name="FEDERATED_IDENTITY")
@Entity
@IdClass(FederatedIdentityEntity.Key.class)
public class FederatedIdentityEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID")
    private UserEntity user;

    @Column(name = "REALM_ID")
    protected String realmId;

    @Id
    @Column(name = "IDENTITY_PROVIDER")
    protected String identityProvider;
    @Column(name = "FEDERATED_USER_ID")
    protected String userId;
    @Column(name = "FEDERATED_USERNAME")
    protected String userName;

    @Column(name = "TOKEN")
    protected String token;

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public String getIdentityProvider() {
        return identityProvider;
    }

    public void setIdentityProvider(String identityProvider) {
        this.identityProvider = identityProvider;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
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

        protected UserEntity user;

        protected String identityProvider;

        public Key() {
        }

        public Key(UserEntity user, String identityProvider) {
            this.user = user;
            this.identityProvider = identityProvider;
        }

        public UserEntity getUser() {
            return user;
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
            if (user != null ? !user.getId().equals(key.user != null ? key.user.getId() : null) : key.user != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = user != null ? user.getId().hashCode() : 0;
            result = 31 * result + (identityProvider != null ? identityProvider.hashCode() : 0);
            return result;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof FederatedIdentityEntity)) return false;

        FederatedIdentityEntity key = (FederatedIdentityEntity) o;

        if (identityProvider != null ? !identityProvider.equals(key.identityProvider) : key.identityProvider != null)
            return false;
        if (user != null ? !user.getId().equals(key.user != null ? key.user.getId() : null) : key.user != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = user != null ? user.getId().hashCode() : 0;
        result = 31 * result + (identityProvider != null ? identityProvider.hashCode() : 0);
        return result;
    }


}
