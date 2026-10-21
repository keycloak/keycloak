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

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import org.keycloak.storage.jpa.KeyUtils;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="getFederatedUserRequiredActionsByUser", query="select action from FederatedUserRequiredActionEntity action where action.userId = :userId and action.realmId=:realmId"),
        @NamedQuery(name="deleteFederatedUserRequiredActionsByUser", query="delete from FederatedUserRequiredActionEntity action where action.realmId=:realmId and action.userId = :userId"),
        @NamedQuery(name="deleteFederatedUserRequiredActionsByRealm", query="delete from FederatedUserRequiredActionEntity action where action.realmId=:realmId"),
        @NamedQuery(name="deleteFederatedUserRequiredActionsByStorageProvider", query="delete from FederatedUserRequiredActionEntity e where e.storageProviderId=:storageProviderId"),
        @NamedQuery(name="deleteFederatedUserRequiredActionsByRealmAndLink", query="delete from FederatedUserRequiredActionEntity action where action.userId IN (select u.id from UserEntity u where u.realmId=:realmId and u.federationLink=:link)")
})
@Entity
@Table(name="FED_USER_REQUIRED_ACTION")
@IdClass(FederatedUserRequiredActionEntity.Key.class)
public class FederatedUserRequiredActionEntity {

    @Id
    @Column(name="USER_ID")
    protected String userId;

    @Id
    @Column(name="REQUIRED_ACTION")
    protected String action;

    @Column(name = "REALM_ID")
    protected String realmId;

    @Column(name = "STORAGE_PROVIDER_ID")
    protected String storageProviderId;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        KeyUtils.assertValidKey(userId);
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

    public static class Key implements Serializable {

        protected String userId;

        protected String action;

        public Key() {
        }

        public Key(String user, String action) {
            this.userId = user;
            this.action = action;
        }

        public String getUserId() {
            return userId;
        }

        public String getAction() {
            return action;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (!Objects.equals(action, key.action)) return false;
            if (userId != null ? !userId.equals(key.userId != null ? key.userId : null) : key.userId != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = userId != null ? userId.hashCode() : 0;
            result = 31 * result + (action != null ? action.hashCode() : 0);
            return result;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof FederatedUserRequiredActionEntity)) return false;

        FederatedUserRequiredActionEntity key = (FederatedUserRequiredActionEntity) o;

        if (!Objects.equals(action, key.action)) return false;
        if (userId != null ? !userId.equals(key.userId != null ? key.userId : null) : key.userId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = userId != null ? userId.hashCode() : 0;
        result = 31 * result + (action != null ? action.hashCode() : 0);
        return result;
    }


}
