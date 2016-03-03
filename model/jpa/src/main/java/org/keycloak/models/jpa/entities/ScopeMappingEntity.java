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
        @NamedQuery(name="hasScope", query="select m from ScopeMappingEntity m where m.client = :client and m.role = :role"),
        @NamedQuery(name="clientScopeMappings", query="select m from ScopeMappingEntity m where m.client = :client"),
        @NamedQuery(name="clientScopeMappingIds", query="select m.role.id from ScopeMappingEntity m where m.client = :client"),
        @NamedQuery(name="deleteScopeMappingByRole", query="delete from ScopeMappingEntity where role = :role"),
        @NamedQuery(name="deleteScopeMappingByClient", query="delete from ScopeMappingEntity where client = :client")
})
@Table(name="SCOPE_MAPPING")
@Entity
@IdClass(ScopeMappingEntity.Key.class)
public class ScopeMappingEntity {

    @Id
    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name = "CLIENT_ID")
    protected ClientEntity client;

    @Id
    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="ROLE_ID")
    protected RoleEntity role;

    public ClientEntity getClient() {
        return client;
    }

    public void setClient(ClientEntity client) {
        this.client = client;
    }

    public RoleEntity getRole() {
        return role;
    }

    public void setRole(RoleEntity role) {
        this.role = role;
    }

    public static class Key implements Serializable {

        protected ClientEntity client;

        protected RoleEntity role;

        public Key() {
        }

        public Key(ClientEntity client, RoleEntity role) {
            this.client = client;
            this.role = role;
        }

        public ClientEntity getClient() {
            return client;
        }

        public RoleEntity getRole() {
            return role;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (client != null ? !client.getId().equals(key.client != null ? key.client.getId() : null) : key.client != null) return false;
            if (role != null ? !role.getId().equals(key.role != null ? key.role.getId() : null) : key.role != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = client != null ? client.getId().hashCode() : 0;
            result = 31 * result + (role != null ? role.getId().hashCode() : 0);
            return result;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof ScopeMappingEntity)) return false;

        ScopeMappingEntity key = (ScopeMappingEntity) o;

        if (client != null ? !client.getId().equals(key.client != null ? key.client.getId() : null) : key.client != null) return false;
        if (role != null ? !role.getId().equals(key.role != null ? key.role.getId() : null) : key.role != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = client != null ? client.getId().hashCode() : 0;
        result = 31 * result + (role != null ? role.getId().hashCode() : 0);
        return result;
    }


}
