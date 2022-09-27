/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.role;

import java.util.Set;
import org.keycloak.models.map.annotations.GenerateEntityImplementations;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.EntityWithAttributes;
import org.keycloak.models.map.common.UpdatableEntity;

@GenerateEntityImplementations(
  inherits = "org.keycloak.models.map.role.MapRoleEntity.AbstractRoleEntity"
)
@DeepCloner.Root
public interface MapRoleEntity extends AbstractEntity, UpdatableEntity, EntityWithAttributes {

    public abstract class AbstractRoleEntity extends UpdatableEntity.Impl implements MapRoleEntity {

        private String id;

        @Override
        public String getId() {
            return this.id;
        }

        @Override
        public void setId(String id) {
            if (this.id != null) throw new IllegalStateException("Id cannot be changed");
            this.id = id;
            this.updated |= id != null;
        }

        @Override
        public Boolean isClientRole() {
            return getClientId() != null;
        }
    }

    Boolean isClientRole();

    String getRealmId();

    String getClientId();

    String getName();

    String getDescription();

    void setClientRole(Boolean clientRole);

    void setRealmId(String realmId);

    void setClientId(String clientId);

    void setName(String name);

    void setDescription(String description);

    Set<String> getCompositeRoles();
    void setCompositeRoles(Set<String> compositeRoles);
    void addCompositeRole(String roleId);
    void removeCompositeRole(String roleId);
}
