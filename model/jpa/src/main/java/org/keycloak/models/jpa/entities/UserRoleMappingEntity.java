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
        @NamedQuery(name="usersInRole", query="select u from UserRoleMappingEntity m, UserEntity u where m.roleId=:roleId and u.id=m.user"),        
        @NamedQuery(name="userHasRole", query="select m from UserRoleMappingEntity m where m.user = :user and m.roleId = :roleId"),
        @NamedQuery(name="userRoleMappings", query="select m from UserRoleMappingEntity m where m.user = :user"),
        @NamedQuery(name="userRoleMappingIds", query="select m.roleId from UserRoleMappingEntity m where m.user = :user"),
        @NamedQuery(name="deleteUserRoleMappingsByRealm", query="delete from  UserRoleMappingEntity mapping where mapping.user IN (select u from UserEntity u where u.realmId=:realmId)"),
        @NamedQuery(name="deleteUserRoleMappingsByRealmAndLink", query="delete from  UserRoleMappingEntity mapping where mapping.user IN (select u from UserEntity u where u.realmId=:realmId and u.federationLink=:link)"),
        @NamedQuery(name="deleteUserRoleMappingsByRole", query="delete from UserRoleMappingEntity m where m.roleId = :roleId"),
        @NamedQuery(name="deleteUserRoleMappingsByUser", query="delete from UserRoleMappingEntity m where m.user = :user"),
        @NamedQuery(name="grantRoleToAllUsers", query="insert into UserRoleMappingEntity (roleId, user) select role.id, user from RoleEntity role, UserEntity user where role.id = :roleId AND role.realm.id = :realmId AND user.realmId = :realmId")

})
@Table(name="USER_ROLE_MAPPING")
@Entity
@IdClass(UserRoleMappingEntity.Key.class)
public class UserRoleMappingEntity  {

    @Id
    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="USER_ID")
    protected UserEntity user;

    @Id
    @Column(name = "ROLE_ID")
    protected String roleId;

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }


    public static class Key implements Serializable {

        protected UserEntity user;

        protected String roleId;

        public Key() {
        }

        public Key(UserEntity user, String roleId) {
            this.user = user;
            this.roleId = roleId;
        }

        public UserEntity getUser() {
            return user;
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
            if (!user.equals(key.user)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = user.hashCode();
            result = 31 * result + roleId.hashCode();
            return result;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof UserRoleMappingEntity)) return false;

        UserRoleMappingEntity key = (UserRoleMappingEntity) o;

        if (!roleId.equals(key.roleId)) return false;
        if (!user.equals(key.user)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = user.hashCode();
        result = 31 * result + roleId.hashCode();
        return result;
    }

}
