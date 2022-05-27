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

import org.keycloak.events.EventType;
import org.keycloak.models.map.annotations.GenerateEntityImplementations;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.ExpirableEntity;
import org.keycloak.models.map.common.UpdatableEntity;

import java.util.Map;

@GenerateEntityImplementations(
        inherits = "org.keycloak.models.map.events.MapAuthEventEntity.AbstractAuthEventEntity"
)
@DeepCloner.Root
public interface MapAuthEventEntity extends UpdatableEntity, AbstractEntity, ExpirableEntity {

    public abstract class AbstractAuthEventEntity extends UpdatableEntity.Impl implements MapAuthEventEntity {

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
     * Returns a point in time (timestamp in milliseconds since The Epoch) when the event entity was created.
     *
     * @return a timestamp in milliseconds since The Epoch or {@code null} when the time is unknown
     */
    Long getTimestamp();

    /**
     * Sets a point in the (timestamp in milliseconds since The Epoch) when the event entity was created.
     * @param timestamp a timestamp in milliseconds since The Epoch or {@code null} when the time is unknown
     */
    void setTimestamp(Long timestamp);

    EventType getType();
    void setType(EventType type);

    String getRealmId();
    void setRealmId(String realmId);

    String getClientId();
    void setClientId(String clientId);

    String getUserId();
    void setUserId(String userId);

    String getSessionId();
    void setSessionId(String sessionId);

    String getIpAddress();
    void setIpAddress(String ipAddress);

    String getError();
    void setError(String error);

    Map<String, String> getDetails();
    void setDetails(Map<String, String> details);
}
