/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * JPA entity for organization role composite mappings.
 */
@Entity
@Table(name = "ORG_ROLE_COMPOSITE")
@NamedQueries({
    @NamedQuery(name = "organizationRoleCompositeExists", query = "SELECT COUNT(c) FROM OrganizationRoleCompositeEntity c WHERE c.composite = :composite"),
    @NamedQuery(name = "organizationRoleComposite", query = "SELECT c FROM OrganizationRoleCompositeEntity c WHERE c.composite = :composite AND c.childRole = :childRole"),
    @NamedQuery(name = "organizationRoleComposites", query = "SELECT c FROM OrganizationRoleCompositeEntity c WHERE c.composite = :composite"),
    @NamedQuery(name = "deleteOrganizationRoleCompositesByComposite", query = "DELETE FROM OrganizationRoleCompositeEntity c WHERE c.composite = :composite"),
    @NamedQuery(name = "deleteOrganizationRoleCompositesByChild", query = "DELETE FROM OrganizationRoleCompositeEntity c WHERE c.childRole = :childRole")
})
public class OrganizationRoleCompositeEntity {

    @Id
    @Column(name = "COMPOSITE", length = 36)
    private String composite;

    @Id
    @Column(name = "CHILD_ROLE", length = 36)
    private String childRole;

    @Column(name = "CHILD_TYPE")
    private String childType; // REALM, CLIENT, ORGANIZATION

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COMPOSITE", insertable = false, updatable = false)
    private OrganizationRoleEntity compositeRole;

    public String getComposite() {
        return composite;
    }

    public void setComposite(String composite) {
        this.composite = composite;
    }

    public String getChildRole() {
        return childRole;
    }

    public void setChildRole(String childRole) {
        this.childRole = childRole;
    }

    public String getChildType() {
        return childType;
    }

    public void setChildType(String childType) {
        this.childType = childType;
    }

    public OrganizationRoleEntity getCompositeRole() {
        return compositeRole;
    }

    public void setCompositeRole(OrganizationRoleEntity compositeRole) {
        this.compositeRole = compositeRole;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrganizationRoleCompositeEntity)) return false;

        OrganizationRoleCompositeEntity that = (OrganizationRoleCompositeEntity) o;

        if (!composite.equals(that.composite)) return false;
        return childRole.equals(that.childRole);
    }

    @Override
    public int hashCode() {
        int result = composite.hashCode();
        result = 31 * result + childRole.hashCode();
        return result;
    }
}
