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

package org.keycloak.models.map.authorization.entity;

import org.keycloak.models.map.annotations.GenerateEntityImplementations;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UpdatableEntity;

@GenerateEntityImplementations(
        inherits = "org.keycloak.models.map.authorization.entity.MapScopeEntity.AbstractMapScopeEntity"
)
@DeepCloner.Root
public interface MapScopeEntity extends UpdatableEntity, AbstractEntity {

    public abstract class AbstractMapScopeEntity extends UpdatableEntity.Impl implements MapScopeEntity {

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

    String getDisplayName();
    void setDisplayName(String displayName);

    String getIconUri();
    void setIconUri(String iconUri);

    String getResourceServerId();
    void setResourceServerId(String resourceServerId);
}
