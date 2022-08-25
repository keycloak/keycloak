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

package org.keycloak.models.map.events;

import org.keycloak.events.admin.OperationType;
import org.keycloak.models.map.annotations.GenerateEntityImplementations;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.ExpirableEntity;
import org.keycloak.models.map.common.UpdatableEntity;

@GenerateEntityImplementations(
        inherits = "org.keycloak.models.map.events.MapAdminEventEntity.AbstractAdminEventEntity"
)
@DeepCloner.Root
public interface MapAdminEventEntity extends UpdatableEntity, AbstractEntity, ExpirableEntity {

    public abstract class AbstractAdminEventEntity extends UpdatableEntity.Impl implements MapAdminEventEntity {

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

    /**
     * Returns a point in time (timestamp in milliseconds since The Epoch) when the event was created.
     *
     * @return a timestamp in milliseconds since The Epoch or {@code null} when the time is unknown
     */
    Long getTimestamp();

    /**
     * Sets a point in the (timestamp in milliseconds since The Epoch) when this entity was created.
     * @param timestamp a timestamp in milliseconds since The Epoch or {@code null} when the time is unknown
     */
    void setTimestamp(Long timestamp);

    String getRealmId();
    void setRealmId(String realmId);

    OperationType getOperationType();
    void setOperationType(OperationType operationType);

    String getResourcePath();
    void setResourcePath(String resourcePath);

    String getRepresentation();
    void setRepresentation(String representation);

    String getError();
    void setError(String error);

    String getResourceType();
    void setResourceType(String resourceType);

    String getAuthRealmId();
    void setAuthRealmId(String realmId);

    String getAuthClientId();
    void setAuthClientId(String clientId);

    String getAuthUserId();
    void setAuthUserId(String userId);

    String getAuthIpAddress();
    void setAuthIpAddress(String ipAddress);
}