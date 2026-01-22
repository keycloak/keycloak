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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Manage compmosite role relations.
 * This used to be a @ManyToMany relation in RoleEntity, and before that there was a native query which lead to stale entities.
 * After those attempts, this is now a separate table that avoids iterating over a lot of parents their entries by applying a simple JPA deletion.
 */
@Entity
@Table(name="COMPOSITE_ROLE")
@NamedQueries({
        @NamedQuery(name="deleteRoleFromComposites", query="delete CompositeRoleEntity c where c.parentRoleId = :roleId or c.childRoleId = :roleId"),
        @NamedQuery(name="deleteSingleCompositeFromRole", query="delete CompositeRoleEntity c where c.parentRoleId = :parentRoleId and c.childRoleId = :childRoleId"),
})
@IdClass(CompositeRoleEntity.Key.class)
public class CompositeRoleEntity {
    @Id
    @Column(name="COMPOSITE", length = 36)
    private String parentRoleId;

    @Id
    @Column(name="CHILD_ROLE", length = 36)
    private String childRoleId;

    /* Although this field seems not to be used, it is important for Hibernate to figure out the dependencies when
    sorting the SQL statements for inserting and deleting. Otherwise, we might see primary key violations. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COMPOSITE", updatable = false, insertable = false)
    RoleEntity parentRole;

    /* Although this field seems not to be used, it is important for Hibernate to figure out the dependencies when
    sorting the SQL statements for inserting and deleting. Otherwise, we might see primary key violations. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CHILD_ROLE", updatable = false, insertable = false)
    RoleEntity childRole;

    public CompositeRoleEntity() {
    }

    public CompositeRoleEntity(RoleEntity parentRole, RoleEntity childRole) {
        // Fields must not be null otherwise the automatic dependency detection of Hibernate will not work
        this.parentRole = parentRole;
        this.childRole = childRole;
        this.childRoleId = childRole.getId();
        this.parentRoleId = parentRole.getId();
    }

    public String getParentRoleId() {
        return parentRoleId;
    }

    public void setParentRoleId(String parentRoleId) {
        this.parentRoleId = parentRoleId;
    }

    public String getChildRoleId() {
        return childRoleId;
    }

    public void setChildRoleId(String childRoleId) {
        this.childRoleId = childRoleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof CompositeRoleEntity that)) return false;

        return parentRoleId.equals(that.parentRoleId) && childRoleId.equals(that.childRoleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(childRoleId, parentRoleId);
    }

    public static class Key implements Serializable {
        private String childRoleId;
        private String parentRoleId;

        public Key() {
        }

        public Key(String parentRoleId, String childRoleId) {
            this.childRoleId = childRoleId;
            this.parentRoleId = parentRoleId;
        }

        public String getChildRoleId() {
            return childRoleId;
        }

        public void setChildRoleId(String childRoleId) {
            this.childRoleId = childRoleId;
        }

        public String getParentRoleId() {
            return parentRoleId;
        }

        public void setParentRoleId(String parentRole) {
            this.parentRoleId = parentRole;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Key key)) return false;
            return Objects.equals(childRoleId, key.childRoleId) && Objects.equals(parentRoleId, key.parentRoleId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(childRoleId, parentRoleId);
        }
    }
}
