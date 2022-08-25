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

import org.keycloak.models.map.common.StringKeyConverter;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * The composite primary key representation for {@link JpaRoleCompositeEntity}.
 * Required for performing a lookup by primary key through the JPA entity manager.
 */
@Embeddable
public class JpaRoleCompositeEntityKey implements Serializable {

    @Column(name = "role_id")
    private UUID roleId;

    public UUID getRoleId() {
        return roleId;
    }

    public String getChildRoleId() {
        return childRoleId;
    }

    @Column(name = "child_role_id")
    private String childRoleId;

    public JpaRoleCompositeEntityKey() {
    }

    public JpaRoleCompositeEntityKey(String roleId, String childRoleId) {
        this.roleId = StringKeyConverter.UUIDKey.INSTANCE.fromString(roleId);
        this.childRoleId = childRoleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof JpaRoleCompositeEntityKey)) return false;

        JpaRoleCompositeEntityKey that = (JpaRoleCompositeEntityKey) o;

        if (!roleId.equals(that.roleId)) return false;
        if (!childRoleId.equals(that.childRoleId)) return false;

        return true;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(roleId, childRoleId);
    }

}