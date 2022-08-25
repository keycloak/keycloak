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
package org.keycloak.models.map.authSession;

import org.keycloak.models.map.annotations.GenerateEntityImplementations;
import org.keycloak.models.map.common.AbstractEntity;

import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.ExpirableEntity;
import org.keycloak.models.map.common.UpdatableEntity;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
@GenerateEntityImplementations(
        inherits = "org.keycloak.models.map.authSession.MapRootAuthenticationSessionEntity.AbstractRootAuthenticationSessionEntity"
)
@DeepCloner.Root
public interface MapRootAuthenticationSessionEntity extends AbstractEntity, UpdatableEntity, ExpirableEntity {

    public abstract class AbstractRootAuthenticationSessionEntity extends UpdatableEntity.Impl implements MapRootAuthenticationSessionEntity {

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
        public Optional<MapAuthenticationSessionEntity> getAuthenticationSession(String tabId) {
            Set<MapAuthenticationSessionEntity> authenticationSessions = getAuthenticationSessions();
            if (authenticationSessions == null || authenticationSessions.isEmpty()) return Optional.empty();

            return authenticationSessions.stream().filter(as -> Objects.equals(as.getTabId(), tabId)).findFirst();
        }

        @Override
        public Boolean removeAuthenticationSession(String tabId) {
            Set<MapAuthenticationSessionEntity> authenticationSessions = getAuthenticationSessions();
            boolean removed = authenticationSessions != null && authenticationSessions.removeIf(c -> Objects.equals(c.getTabId(), tabId));
            this.updated |= removed;
            return removed;
        }

        @Override
        public boolean isUpdated() {
            return this.updated ||
                    Optional.ofNullable(getAuthenticationSessions()).orElseGet(Collections::emptySet).stream().anyMatch(MapAuthenticationSessionEntity::isUpdated);
        }

        @Override
        public void clearUpdatedFlag() {
            this.updated = false;
            Optional.ofNullable(getAuthenticationSessions()).orElseGet(Collections::emptySet).forEach(UpdatableEntity::clearUpdatedFlag);
        }
    }

    String getRealmId();
    void setRealmId(String realmId);

    /**
     * Returns a point in time (timestamp in milliseconds since The Epoch) when the root authentication session entity was created or
     * updated during an authentication process.
     *
     * @return a timestamp in milliseconds since The Epoch or {@code null} when the time is unknown
     */
    Long getTimestamp();

    /**
     * Sets a point in the (timestamp in milliseconds since The Epoch) when the root authentication session entity was created or
     * updated during an authentication process.
     * @param timestamp a timestamp in milliseconds since The Epoch or {@code null} when the time is unknown
     */
    void setTimestamp(Long timestamp);

    Set<MapAuthenticationSessionEntity> getAuthenticationSessions();
    void setAuthenticationSessions(Set<MapAuthenticationSessionEntity> authenticationSessions);
    Optional<MapAuthenticationSessionEntity> getAuthenticationSession(String tabId);
    void addAuthenticationSession(MapAuthenticationSessionEntity authenticationSession);
    Boolean removeAuthenticationSession(String tabId);
}
