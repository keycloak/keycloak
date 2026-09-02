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
        @NamedQuery(name="feduserHasRole", query="select m from FederatedUserRoleMappingEntity m where m.userId = :userId and m.roleId = :roleId"),
        @NamedQuery(name="feduserRoleMappings", query="select m from FederatedUserRoleMappingEntity m where m.userId = :userId"),
        @NamedQuery(name="deleteFederatedUserRoleMappingsByRealm", query="delete from  FederatedUserRoleMappingEntity mapping where mapping.realmId=:realmId"),
        @NamedQuery(name="deleteFederatedUserRoleMappingsByStorageProvider", query="delete from FederatedUserRoleMappingEntity e where e.storageProviderId=:storageProviderId"),
        @NamedQuery(name="deleteFederatedUserRoleMappingsByRealmAndLink", query="delete from  FederatedUserRoleMappingEntity mapping where mapping.userId IN (select u.id from UserEntity u where u.realmId=:realmId and u.federationLink=:link)"),
        @NamedQuery(name="deleteFederatedUserRoleMappingsByRole", query="delete from FederatedUserRoleMappingEntity m where m.roleId = :roleId"),
        @NamedQuery(name="deleteFederatedUserRoleMappingsByUser", query="delete from FederatedUserRoleMappingEntity m where m.userId = :userId and m.realmId = :realmId"),
        @NamedQuery(name="fedRoleMembership", query="select m.userId FROM FederatedUserRoleMappingEntity m where m.roleId = :roleId AND m.realmId = :realmId"),  
})
@Table(name="FED_USER_ROLE_MAPPING")
@Entity
@IdClass(FederatedUserRoleMappingEntity.Key.class)
public class FederatedUserRoleMappingEntity {

    @Id
    @Column(name = "USER_ID")
    protected String userId;

    @Id
    @Column(name = "ROLE_ID")
    protected String roleId;

    @Column(name = "REALM_ID")
    protected String realmId;

    @Column(name = "STORAGE_PROVIDER_ID")
    protected String storageProviderId;

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

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }


    public static class Key implements Serializable {

        protected String userId;

        protected String roleId;

        public Key() {
        }

        public Key(String userId, String roleId) {
            this.userId = userId;
            this.roleId = roleId;
        }

        public String getUserId() {
            return userId;
        }

        public String getRoleId() {
            return roleId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (!roleId.equals(key.roleId)) return false;
            if (!userId.equals(key.userId)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = userId.hashCode();
            result = 31 * result + roleId.hashCode();
            return result;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof FederatedUserRoleMappingEntity)) return false;

        FederatedUserRoleMappingEntity key = (FederatedUserRoleMappingEntity) o;

        if (!roleId.equals(key.roleId)) return false;
        if (!userId.equals(key.userId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = userId.hashCode();
        result = 31 * result + roleId.hashCode();
        return result;
    }

}
