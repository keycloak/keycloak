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
    @NamedQuery(name="groupsInRole", query="select g from GroupRoleMappingEntity m, GroupEntity g where m.roleId=:roleId and g.id=m.group"),
        @NamedQuery(name="groupHasRole", query="select m from GroupRoleMappingEntity m where m.group = :group and m.roleId = :roleId"),
        @NamedQuery(name="groupRoleMappings", query="select m from GroupRoleMappingEntity m where m.group = :group"),
        @NamedQuery(name="groupRoleMappingIds", query="select m.roleId from GroupRoleMappingEntity m where m.group = :group"),
        @NamedQuery(name="deleteGroupRoleMappingsByRealm", query="delete from  GroupRoleMappingEntity mapping where mapping.group IN (select u from GroupEntity u where u.realm=:realm)"),
        @NamedQuery(name="deleteGroupRoleMappingsByRole", query="delete from GroupRoleMappingEntity m where m.roleId = :roleId"),
        @NamedQuery(name="deleteGroupRoleMappingsByGroup", query="delete from GroupRoleMappingEntity m where m.group = :group")

})
@Table(name="GROUP_ROLE_MAPPING")
@Entity
@IdClass(GroupRoleMappingEntity.Key.class)
public class GroupRoleMappingEntity {

    @Id
    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="GROUP_ID")
    protected GroupEntity group;

    @Id
    @Column(name = "ROLE_ID")
    protected String roleId;

    public GroupEntity getGroup() {
        return group;
    }

    public void setGroup(GroupEntity group) {
        this.group = group;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public static class Key implements Serializable {

        protected GroupEntity group;

        protected String roleId;

        public Key() {
        }

        public Key(GroupEntity group, String roleId) {
            this.group = group;
            this.roleId = roleId;
        }

        public GroupEntity getGroup() {
            return group;
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
            if (!group.equals(key.group)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = group.hashCode();
            result = 31 * result + roleId.hashCode();
            return result;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof GroupRoleMappingEntity)) return false;

        GroupRoleMappingEntity key = (GroupRoleMappingEntity) o;

        if (!roleId.equals(key.roleId)) return false;
        if (!group.equals(key.group)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = group.hashCode();
        result = 31 * result + roleId.hashCode();
        return result;
    }

}
