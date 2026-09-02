/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
import java.util.Objects;

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
 * Manage compmosite role relations.
 * This used to be a @ManyToMany relation in RoleEntity, and before that there was a native query which lead to stale entities.
 * After those attempts, this is now a separate table that avoids iterating over a lot of parents their entries by applying a simple JPA deletion.
 */
@Entity
@Table(name="COMPOSITE_ROLE")
@NamedQueries({
        @NamedQuery(name="deleteRoleFromComposites", query="delete CompositeRoleEntity c where c.parentRole = :role or c.childRole = :role"),
        @NamedQuery(name="deleteSingleCompositeFromRole", query="delete CompositeRoleEntity c where c.parentRole = :parentRole and c.childRole = :childRole"),
})
@IdClass(CompositeRoleEntity.Key.class)
public class CompositeRoleEntity {
    @Id
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="COMPOSITE")
    private RoleEntity parentRole;

    @Id
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="CHILD_ROLE")
    private RoleEntity childRole;

    public CompositeRoleEntity() {
    }

    public CompositeRoleEntity(RoleEntity parentRole, RoleEntity childRole) {
        // Fields must not be null otherwise the automatic dependency detection of Hibernate will not work
        this.parentRole = parentRole;
        this.childRole = childRole;
    }

    public RoleEntity getParentRole() {
        return parentRole;
    }

    public void setParentRole(RoleEntity parentRole) {
        this.parentRole = parentRole;
    }

    public RoleEntity getChildRole() {
        return childRole;
    }

    public void setChildRole(RoleEntity childRole) {
        this.childRole = childRole;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof CompositeRoleEntity that)) return false;

        return parentRole.equals(that.parentRole) && childRole.equals(that.childRole);
    }

    @Override
    public int hashCode() {
        return Objects.hash(childRole, parentRole);
    }

    public static class Key implements Serializable {
        private RoleEntity childRole;
        private RoleEntity parentRole;

        public Key() {
        }

        public Key(RoleEntity parentRole, RoleEntity childRole) {
            this.childRole = childRole;
            this.parentRole = parentRole;
        }

        public RoleEntity getChildRole() {
            return childRole;
        }

        public void setChildRole(RoleEntity childRole) {
            this.childRole = childRole;
        }

        public RoleEntity getParentRole() {
            return parentRole;
        }

        public void setParentRole(RoleEntity parentRole) {
            this.parentRole = parentRole;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Key key)) return false;
            return Objects.equals(childRole, key.childRole) && Objects.equals(parentRole, key.parentRole);
        }

        @Override
        public int hashCode() {
            return Objects.hash(childRole, parentRole);
        }
    }
}
