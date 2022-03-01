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
import org.keycloak.models.map.common.UpdatableEntity;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
@GenerateEntityImplementations(
        inherits = "org.keycloak.models.map.authSession.MapRootAuthenticationSessionEntity.AbstractRootAuthenticationSessionEntity"
)
@DeepCloner.Root
public interface MapRootAuthenticationSessionEntity extends AbstractEntity, UpdatableEntity {

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
        public boolean isUpdated() {
            return this.updated ||
                    Optional.ofNullable(getAuthenticationSessions()).orElseGet(Collections::emptyMap).values().stream().anyMatch(MapAuthenticationSessionEntity::isUpdated);
        }

        @Override
        public void clearUpdatedFlag() {
            this.updated = false;
            Optional.ofNullable(getAuthenticationSessions()).orElseGet(Collections::emptyMap).values().forEach(UpdatableEntity::clearUpdatedFlag);
        }
    }

    String getRealmId();
    void setRealmId(String realmId);

    Integer getTimestamp();
    void setTimestamp(Integer timestamp);

    Map<String, MapAuthenticationSessionEntity> getAuthenticationSessions();
    void setAuthenticationSessions(Map<String, MapAuthenticationSessionEntity> authenticationSessions);
    void setAuthenticationSession(String tabId, MapAuthenticationSessionEntity entity);
    Boolean removeAuthenticationSession(String tabId);
}
