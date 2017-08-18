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
        @NamedQuery(name="feduserMemberOf", query="select m from FederatedUserGroupMembershipEntity m where m.userId = :userId and m.groupId = :groupId"),
        @NamedQuery(name="feduserGroupMembership", query="select m from FederatedUserGroupMembershipEntity m where m.userId = :userId"),
        @NamedQuery(name="fedgroupMembership", query="select g.userId from FederatedUserGroupMembershipEntity g where g.groupId = :groupId and g.realmId = :realmId"),
        @NamedQuery(name="feduserGroupIds", query="select m.groupId from FederatedUserGroupMembershipEntity m where m.userId = :userId"),
        @NamedQuery(name="deleteFederatedUserGroupMembershipByRealm", query="delete from  FederatedUserGroupMembershipEntity mapping where mapping.realmId=:realmId"),
        @NamedQuery(name="deleteFederatedUserGroupMembershipByStorageProvider", query="delete from FederatedUserGroupMembershipEntity e where e.storageProviderId=:storageProviderId"),
        @NamedQuery(name="deleteFederatedUserGroupMembershipsByRealmAndLink", query="delete from  FederatedUserGroupMembershipEntity mapping where mapping.userId IN (select u.id from UserEntity u where u.realmId=:realmId and u.federationLink=:link)"),
        @NamedQuery(name="deleteFederatedUserGroupMembershipsByGroup", query="delete from FederatedUserGroupMembershipEntity m where m.groupId = :groupId"),
        @NamedQuery(name="deleteFederatedUserGroupMembershipsByUser", query="delete from FederatedUserGroupMembershipEntity m where m.userId = :userId and m.realmId = :realmId")

})
@Table(name="FED_USER_GROUP_MEMBERSHIP")
@Entity
@IdClass(FederatedUserGroupMembershipEntity.Key.class)
public class FederatedUserGroupMembershipEntity {

    @Id
    @Column(name = "USER_ID")
    protected String userId;

    @Id
    @Column(name = "GROUP_ID")
    protected String groupId;

    @Column(name = "REALM_ID")
    protected String realmId;

    @Column(name = "STORAGE_PROVIDER_ID")
    protected String storageProviderId;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
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

        protected String groupId;

        public Key() {
        }

        public Key(String userId, String groupId) {
            this.userId = userId;
            this.groupId = groupId;
        }

        public String getUserId() {
            return userId;
        }

        public String getGroupId() {
            return groupId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (!groupId.equals(key.groupId)) return false;
            if (!userId.equals(key.userId)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = userId.hashCode();
            result = 31 * result + groupId.hashCode();
            return result;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof FederatedUserGroupMembershipEntity)) return false;

        FederatedUserGroupMembershipEntity key = (FederatedUserGroupMembershipEntity) o;

        if (!groupId.equals(key.groupId)) return false;
        if (!userId.equals(key.userId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = userId.hashCode();
        result = 31 * result + groupId.hashCode();
        return result;
    }

}
