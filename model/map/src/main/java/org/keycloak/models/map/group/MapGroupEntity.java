/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.group;

import org.keycloak.models.map.annotations.GenerateEntityImplementations;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.EntityWithAttributes;
import org.keycloak.models.map.common.UpdatableEntity;

import java.util.Set;

@GenerateEntityImplementations(
        inherits = "org.keycloak.models.map.group.MapGroupEntity.AbstractGroupEntity"
)
@DeepCloner.Root
public interface MapGroupEntity extends UpdatableEntity, AbstractEntity, EntityWithAttributes {

    public abstract class AbstractGroupEntity extends UpdatableEntity.Impl implements MapGroupEntity {

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

    }

    String getName();
    void setName(String name);

    String getParentId();
    void setParentId(String parentId);

    String getRealmId();
    void setRealmId(String realmId);

    Set<String> getGrantedRoles();
    void setGrantedRoles(Set<String> grantedRoles);
    void addGrantedRole(String role);
    void removeGrantedRole(String role);
}
