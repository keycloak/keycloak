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
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
@GenerateEntityImplementations
@DeepCloner.Root
public interface MapAuthenticationSessionEntity extends UpdatableEntity {

    String getTabId();
    void setTabId(String tabId);

    /**
     * Returns a point in time (timestamp in milliseconds since The Epoch) when the authentication session entity was created.
     *
     * @return a timestamp in milliseconds since The Epoch or {@code null} when the time is unknown
     */
    Long getTimestamp();

    /**
     * Sets a point in the (timestamp in milliseconds since The Epoch) when the authentication session entity was created.
     * @param timestamp a timestamp in milliseconds since The Epoch or {@code null} when the time is unknown
     */
    void setTimestamp(Long timestamp);

    Map<String, String> getUserSessionNotes();
    void setUserSessionNotes(Map<String, String> userSessionNotes);
    void setUserSessionNote(String name, String value);

    String getClientUUID();
    void setClientUUID(String clientUUID);

    String getAuthUserId();
    void setAuthUserId(String authUserId);

    String getRedirectUri();
    void setRedirectUri(String redirectUri);

    String getAction();
    void setAction(String action);

    Set<String> getClientScopes();
    void setClientScopes(Set<String> clientScopes);

    Set<String> getRequiredActions();
    void setRequiredActions(Set<String> requiredActions);
    void addRequiredAction(String requiredAction);
    void removeRequiredAction(String action);

    String getProtocol();
    void setProtocol(String protocol);

    Map<String, String> getClientNotes();
    void setClientNotes(Map<String, String> clientNotes);
    void setClientNote(String name, String value);
    void removeClientNote(String name);

    Map<String, String> getAuthNotes();
    void setAuthNotes(Map<String, String> authNotes);
    void setAuthNote(String name, String value);
    void removeAuthNote(String name);

    Map<String, AuthenticationSessionModel.ExecutionStatus> getExecutionStatuses();
    void setExecutionStatuses(Map<String, AuthenticationSessionModel.ExecutionStatus> executionStatus);
    void setExecutionStatus(String authenticator, AuthenticationSessionModel.ExecutionStatus status);
}
