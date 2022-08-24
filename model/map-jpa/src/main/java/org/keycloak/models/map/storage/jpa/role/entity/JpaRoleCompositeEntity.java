/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.jpa.role.entity;

import org.hibernate.annotations.Immutable;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * This is a child table of {@link JpaRoleEntity} that is managed via named queries to avoid loading all its contents
 * via a {@link javax.persistence.OneToMany} relation.
 */
@Entity
@Table(name = "kc_role_composite", uniqueConstraints = {@UniqueConstraint(columnNames = {"role_id", "child_role_id"})})
@Immutable
@NamedQueries({
        @NamedQuery(name = "selectChildRolesFromCompositeRole",
                query = "select rolecomposite.key from JpaRoleCompositeEntity rolecomposite where rolecomposite.key.roleId = :roleId"),
        @NamedQuery(name = "deleteChildRoleFromCompositeRole",
                query = "delete from JpaRoleCompositeEntity rolecomposite where rolecomposite.key.roleId = :roleId and rolecomposite.key.childRoleId = :childRoleId"),
        @NamedQuery(name = "deleteAllChildRolesFromCompositeRole",
                query = "delete from JpaRoleCompositeEntity rolecomposite where rolecomposite.key.roleId = :roleId"),
})
public class JpaRoleCompositeEntity {

    @EmbeddedId
    private JpaRoleCompositeEntityKey key = new JpaRoleCompositeEntityKey();

    public JpaRoleCompositeEntity() {
    }

    public JpaRoleCompositeEntity(JpaRoleCompositeEntityKey key) {
        this.key = key;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof JpaRoleCompositeEntity)) return false;

        JpaRoleCompositeEntity that = (JpaRoleCompositeEntity) o;

        if (!key.equals(that.key)) return false;

        return true;
    }

}
