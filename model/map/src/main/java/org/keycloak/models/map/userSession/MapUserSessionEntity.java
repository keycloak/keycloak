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

import org.keycloak.models.UserSessionModel;
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
        inherits = "org.keycloak.models.map.userSession.MapUserSessionEntity.AbstractUserSessionEntity"
)
@DeepCloner.Root
public interface MapUserSessionEntity extends AbstractEntity, UpdatableEntity, ExpirableEntity {

    abstract class AbstractUserSessionEntity extends UpdatableEntity.Impl implements MapUserSessionEntity {

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

    String getRealmId();
    void setRealmId(String realmId);

    String getUserId();
    void setUserId(String userId);

    String getBrokerSessionId();
    void setBrokerSessionId(String brokerSessionId);

    String getBrokerUserId();
    void setBrokerUserId(String brokerUserId);

    String getLoginUsername();
    void setLoginUsername(String loginUsername);

    String getIpAddress();
    void setIpAddress(String ipAddress);

    String getAuthMethod();
    void setAuthMethod(String authMethod);

    Boolean isRememberMe();
    void setRememberMe(Boolean rememberMe);

    /**
     * Returns a point in time (timestamp in milliseconds since The Epoch) when the user session entity was created.
     *
     * @return a timestamp in milliseconds since The Epoch or {@code null} when the time is unknown
     */
    Long getTimestamp();

    /**
     * Sets a point in the (timestamp in milliseconds since The Epoch) when the user session entity was created.
     * @param timestamp a timestamp in milliseconds since The Epoch or {@code null} when the time is unknown
     */
    void setTimestamp(Long timestamp);

    /**
     * Returns a point in time (timestamp in milliseconds since The Epoch) when the user session entity was last refreshed.
     *
     * @return a timestamp in milliseconds since The Epoch or {@code null} when the time is unknown
     */
    Long getLastSessionRefresh();

    /**
     * Sets a point in the (timestamp in milliseconds since The Epoch) when the user session entity was last refreshed.
     * @param lastSessionRefresh a timestamp in milliseconds since The Epoch or {@code null} when the time is unknown
     */
    void setLastSessionRefresh(Long lastSessionRefresh);

    Map<String, String> getNotes();
    String getNote(String name);
    void setNotes(Map<String, String> notes);
    Boolean removeNote(String name);
    void setNote(String name, String value);

    UserSessionModel.State getState();
    void setState(UserSessionModel.State state);

    Map<String, String> getAuthenticatedClientSessions();
    void setAuthenticatedClientSessions(Map<String, String> authenticatedClientSessions);
    String getAuthenticatedClientSession(String clientUUID);
    void setAuthenticatedClientSession(String clientUUID, String clientSessionId);
    Boolean removeAuthenticatedClientSession(String clientUUID);

    Boolean isOffline();
    void setOffline(Boolean offline);

    UserSessionModel.SessionPersistenceState getPersistenceState();
    void setPersistenceState(UserSessionModel.SessionPersistenceState persistenceState);
}
