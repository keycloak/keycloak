/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.lock;

import org.keycloak.models.map.annotations.GenerateEntityImplementations;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.storage.SearchableModelField;

import java.util.Objects;

/**
 * Entity to hold locks needed for the {@link MapGlobalLockProvider}.
 *
 * @author Alexander Schwartz
 */
@GenerateEntityImplementations(
        inherits = "org.keycloak.models.map.lock.MapLockEntity.AbstractLockEntity"
)
@DeepCloner.Root
public interface MapLockEntity extends UpdatableEntity, AbstractEntity {

    public static class SearchableFields {
        public static final SearchableModelField<MapLockEntity> NAME = new SearchableModelField<>("name", String.class);
    }

    public abstract class AbstractLockEntity extends Impl implements MapLockEntity {

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

    String getKeycloakInstanceIdentifier();
    void setKeycloakInstanceIdentifier(String keycloakInstanceIdentifier);

    Long getTimeAcquired();
    void setTimeAcquired(Long timeAcquired);

    default boolean isLockUnchanged(MapLockEntity otherMapLock) {
        return Objects.equals(getKeycloakInstanceIdentifier(), otherMapLock.getKeycloakInstanceIdentifier()) &&
                Objects.equals(getTimeAcquired(), otherMapLock.getTimeAcquired()) &&
                Objects.equals(getName(), otherMapLock.getName()) &&
                Objects.equals(getId(), otherMapLock.getId());
    }

}
