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
package org.keycloak.models.map.clientscope;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.keycloak.models.map.annotations.GenerateEntityImplementations;
import org.keycloak.models.map.client.MapProtocolMapperEntity;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.EntityWithAttributes;
import org.keycloak.models.map.common.UpdatableEntity;

@GenerateEntityImplementations(
  inherits = "org.keycloak.models.map.clientscope.MapClientScopeEntity.AbstractClientScopeEntity"
)
@DeepCloner.Root
public interface MapClientScopeEntity extends AbstractEntity, UpdatableEntity, EntityWithAttributes {

    public abstract class AbstractClientScopeEntity extends UpdatableEntity.Impl implements MapClientScopeEntity {

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
        public Optional<MapProtocolMapperEntity> getProtocolMapper(String id) {
            Set<MapProtocolMapperEntity> mappers = getProtocolMappers();
            if (mappers == null || mappers.isEmpty()) return Optional.empty();

            return mappers.stream().filter(m -> Objects.equals(m.getId(), id)).findFirst();
        }

        @Override
        public void removeProtocolMapper(String id) {
            Set<MapProtocolMapperEntity> mappers = getProtocolMappers();
            this.updated |= mappers != null && mappers.removeIf(m -> Objects.equals(m.getId(), id));
        }
    }

    String getName();

    String getDescription();

    String getProtocol();

    String getRealmId();

    void setName(String name);

    void setDescription(String description);

    void setProtocol(String protocol);

    void setRealmId(String realmId);

    Optional<MapProtocolMapperEntity> getProtocolMapper(String id);
    Set<MapProtocolMapperEntity> getProtocolMappers();
    void addProtocolMapper(MapProtocolMapperEntity mapping);
    void removeProtocolMapper(String id);

    void addScopeMapping(String id);
    void removeScopeMapping(String id);
    Collection<String> getScopeMappings();
}
