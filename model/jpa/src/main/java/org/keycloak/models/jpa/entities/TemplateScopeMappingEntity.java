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
        @NamedQuery(name="templateHasScope", query="select m from TemplateScopeMappingEntity m where m.template = :template and m.role = :role"),
        @NamedQuery(name="clientTemplateScopeMappings", query="select m from TemplateScopeMappingEntity m where m.template = :template"),
        @NamedQuery(name="clientTemplateScopeMappingIds", query="select m.role.id from TemplateScopeMappingEntity m where m.template = :template"),
        @NamedQuery(name="deleteTemplateScopeMappingByRole", query="delete from TemplateScopeMappingEntity where role = :role"),
        @NamedQuery(name="deleteTemplateScopeMappingByClient", query="delete from TemplateScopeMappingEntity where template = :template")
})
@Table(name="TEMPLATE_SCOPE_MAPPING")
@Entity
@IdClass(TemplateScopeMappingEntity.Key.class)
public class TemplateScopeMappingEntity {

    @Id
    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name = "TEMPLATE_ID")
    protected ClientTemplateEntity template;

    @Id
    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="ROLE_ID")
    protected RoleEntity role;

    public ClientTemplateEntity getTemplate() {
        return template;
    }

    public void setTemplate(ClientTemplateEntity template) {
        this.template = template;
    }

    public RoleEntity getRole() {
        return role;
    }

    public void setRole(RoleEntity role) {
        this.role = role;
    }

    public static class Key implements Serializable {

        protected ClientTemplateEntity template;

        protected RoleEntity role;

        public Key() {
        }

        public Key(ClientTemplateEntity template, RoleEntity role) {
            this.template = template;
            this.role = role;
        }

        public ClientTemplateEntity getTemplate() {
            return template;
        }

        public RoleEntity getRole() {
            return role;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (template != null ? !template.getId().equals(key.template != null ? key.template.getId() : null) : key.template != null) return false;
            if (role != null ? !role.getId().equals(key.role != null ? key.role.getId() : null) : key.role != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = template != null ? template.getId().hashCode() : 0;
            result = 31 * result + (role != null ? role.getId().hashCode() : 0);
            return result;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!(o instanceof TemplateScopeMappingEntity)) return false;

        TemplateScopeMappingEntity key = (TemplateScopeMappingEntity) o;

        if (template != null ? !template.getId().equals(key.template != null ? key.template.getId() : null) : key.template != null) return false;
        if (role != null ? !role.getId().equals(key.role != null ? key.role.getId() : null) : key.role != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = template != null ? template.getId().hashCode() : 0;
        result = 31 * result + (role != null ? role.getId().hashCode() : 0);
        return result;
    }


}
