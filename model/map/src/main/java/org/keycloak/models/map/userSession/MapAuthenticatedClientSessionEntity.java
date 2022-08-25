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
package org.keycloak.models.map.userSession;

import org.keycloak.models.map.annotations.GenerateEntityImplementations;
import org.keycloak.models.map.common.AbstractEntity;

import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.ExpirableEntity;
import org.keycloak.models.map.common.UpdatableEntity;
import java.util.Map;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
@GenerateEntityImplementations(
        inherits = "org.keycloak.models.map.userSession.MapAuthenticatedClientSessionEntity.AbstractAuthenticatedClientSessionEntity"
)
@DeepCloner.Root
public interface MapAuthenticatedClientSessionEntity extends AbstractEntity, UpdatableEntity, ExpirableEntity {

    abstract class AbstractAuthenticatedClientSessionEntity extends UpdatableEntity.Impl implements MapAuthenticatedClientSessionEntity {

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
     * Returns a point in time (timestamp in milliseconds since The Epoch) when the client session entity was created or updated (refreshed).
     *
     * @return a timestamp in milliseconds since The Epoch or {@code null} when the time is unknown
     */
    Long getTimestamp();

    /**
     * Sets a point in the (timestamp in milliseconds since The Epoch) when the client session entity was created or updated (refreshed).
     * @param timestamp a timestamp in milliseconds since The Epoch or {@code null} when the time is unknown
     */
    void setTimestamp(Long timestamp);

    String getRealmId();
    void setRealmId(String realmId);

    String getClientId();
    void setClientId(String clientId);

    String getAuthMethod();
    void setAuthMethod(String authMethod);

    String getRedirectUri();
    void setRedirectUri(String redirectUri);
    String getAction();
    void setAction(String action);

    Map<String, String> getNotes();
    void setNotes(Map<String, String> notes);
    String getNote(String name);
    Boolean removeNote(String name);
    void setNote(String name, String value);

    String getCurrentRefreshToken();
    void setCurrentRefreshToken(String currentRefreshToken);

    Integer getCurrentRefreshTokenUseCount();
    void setCurrentRefreshTokenUseCount(Integer currentRefreshTokenUseCount);

    Boolean isOffline();
    void setOffline(Boolean offline);
}
