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

import org.keycloak.representations.idm.MembershipType;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="userMemberOf", query="select m from UserGroupMembershipEntity m where m.user = :user and m.groupId = :groupId"),
        @NamedQuery(name="userGroupMembership", query="select m from UserGroupMembershipEntity m where m.user = :user"),
        @NamedQuery(name="deleteUserGroupMembershipByRealm", query="delete from  UserGroupMembershipEntity mapping where mapping.user IN (select u from UserEntity u where u.realmId=:realmId)"),
        @NamedQuery(name="deleteUserGroupMembershipsByRealmAndLink", query="delete from  UserGroupMembershipEntity mapping where mapping.user IN (select u from UserEntity u where u.realmId=:realmId and u.federationLink=:link)"),
        @NamedQuery(name="deleteUserGroupMembershipsByGroup", query="delete from UserGroupMembershipEntity m where m.groupId = :groupId"),
        @NamedQuery(name="deleteUserGroupMembershipsByUser", query="delete from UserGroupMembershipEntity m where m.user = :user"),
        @NamedQuery(name="userCountInGroups", query="select count(m.user) from UserGroupMembershipEntity m where m.user.realmId = :realmId and m.groupId in :groupIds")
})
@Table(name="USER_GROUP_MEMBERSHIP")
@Entity
@IdClass(UserGroupMembershipEntity.Key.class)
public class UserGroupMembershipEntity {

    @Id
    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="USER_ID")
    protected UserEntity user;

    @Id
    @Column(name = "GROUP_ID")
    protected String groupId;

    @Column(name = "MEMBERSHIP_TYPE")
    private String membershipType;

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public MembershipType getMembershipType() {
        return MembershipType.valueOf(membershipType);
    }

    public void setMembershipType(MembershipType membershipType) {
        this.membershipType = membershipType.toString();
    }

    public static class Key implements Serializable {

        protected UserEntity user;

        protected String groupId;

        public Key() {
        }

        public Key(UserEntity user, String groupId) {
            this.user = user;
            this.groupId = groupId;
        }

        public UserEntity getUser() {
            return user;
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
            if (!user.equals(key.user)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = user.hashCode();
            result = 31 * result + groupId.hashCode();
            return result;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof UserGroupMembershipEntity)) return false;

        UserGroupMembershipEntity key = (UserGroupMembershipEntity) o;

        if (!groupId.equals(key.groupId)) return false;
        if (!user.equals(key.user)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = user.hashCode();
        result = 31 * result + groupId.hashCode();
        return result;
    }

}
