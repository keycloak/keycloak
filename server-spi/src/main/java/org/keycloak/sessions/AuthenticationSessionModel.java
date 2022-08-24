/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.sessions;

import java.util.Map;
import java.util.Set;

import org.keycloak.models.UserModel;

/**
 * Represents the state of the authentication. If the login is requested from different tabs of same browser, every browser
 * tab has it's own state of the authentication. So there is separate AuthenticationSessionModel for every tab. Whole browser
 * is represented by {@link RootAuthenticationSessionModel}
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface AuthenticationSessionModel extends CommonClientSessionModel {

    /**
     * @return ID of this subsession (in other words, usually browser tab). For lookup the AuthenticationSessionModel, you need:
     * ID of rootSession (parent), client UUID and tabId. For lookup the ID of the parent, use {@link #getParentSession().getId()}
     */
    String getTabId();

    /**
     * Returns the root authentication session that is parent of this authentication session.
     * @return {@code RootAuthenticationSessionModel}
     */
    RootAuthenticationSessionModel getParentSession();

    /**
     * Returns execution status of the authentication session.
     * @return {@code Map<String, ExecutionStatus>} Never returns {@code null}.
     */
    Map<String, ExecutionStatus> getExecutionStatus();

    /**
     * Sets execution status of the authentication session.
     * @param authenticator {@code String} Can't be {@code null}.
     * @param status {@code ExecutionStatus} Can't be {@code null}.
     */
    void setExecutionStatus(String authenticator, ExecutionStatus status);

    /**
     * Clears execution status of the authentication session.
     */
    void clearExecutionStatus();

    /**
     * Returns authenticated user that is associated to the authentication session.
     * @return {@code UserModel} or null if there's no authenticated user.
     */
    UserModel getAuthenticatedUser();

    /**
     * Sets authenticated user that is associated to the authentication session.
     * @param user {@code UserModel} If {@code null} then {@code null} will be set to the authenticated user.
     */
    void setAuthenticatedUser(UserModel user);

    /**
     * Returns required actions that are attached to this client session.
     * @return {@code Set<String>} Never returns {@code null}.
     */
    Set<String> getRequiredActions();

    /**
     * Adds a required action to the authentication session.
     * @param action {@code String} Can't be {@code null}.
     */
    void addRequiredAction(String action);

    /**
     * Removes a required action from the authentication session.
     * @param action {@code String} Can't be {@code null}.
     */
    void removeRequiredAction(String action);

    /**
     * Adds a required action to the authentication session.
     * @param action {@code UserModel.RequiredAction} Can't be {@code null}.
     */
    void addRequiredAction(UserModel.RequiredAction action);

    /**
     * Removes a required action from the authentication session.
     * @param action {@code UserModel.RequiredAction} Can't be {@code null}.
     */
    void removeRequiredAction(UserModel.RequiredAction action);

    /**
     * Sets the given user session note to the given value. User session notes are notes
     * you want be applied to the UserSessionModel when the client session is attached to it.
     * @param name {@code String} If {@code null} is provided the method won't have an effect.
     * @param value {@code String} If {@code null} is provided the method won't have an effect.
     */
    void setUserSessionNote(String name, String value);

    /**
     * Retrieves value of given user session note. User session notes are notes
     * you want be applied to the UserSessionModel when the client session is attached to it.
     * @return {@code Map<String, String>} never returns {@code null}
     */
    Map<String, String> getUserSessionNotes();

    /**
     * Clears all user session notes. User session notes are notes
     * you want be applied to the UserSessionModel when the client session is attached to it.
     */
    void clearUserSessionNotes();

    /**
     * Retrieves value of the given authentication note to the given value. Authentication notes are notes
     * used typically by authenticators and authentication flows. They are cleared when
     * authentication session is restarted.
     * @param name {@code String} If {@code null} is provided then the method will return {@code null}.
     * @return {@code String} or {@code null} if no authentication note is set.
     */
    String getAuthNote(String name);

    /**
     * Sets the given authentication note to the given value. Authentication notes are notes
     * used typically by authenticators and authentication flows. They are cleared when
     * authentication session is restarted.
     * @param name {@code String} If {@code null} is provided the method won't have an effect.
     * @param value {@code String} If {@code null} is provided the method won't have an effect.
     */
    void setAuthNote(String name, String value);

    /**
     * Removes the given authentication note. Authentication notes are notes
     * used typically by authenticators and authentication flows. They are cleared when
     * authentication session is restarted.
     * @param name {@code String} If {@code null} is provided the method won't have an effect.
     */
    void removeAuthNote(String name);

    /**
     * Clears all authentication note. Authentication notes are notes
     * used typically by authenticators and authentication flows. They are cleared when
     * authentication session is restarted.
     */
    void clearAuthNotes();

    /**
     * Retrieves value of the given client note to the given value. Client notes are notes
     * specific to client protocol. They are NOT cleared when authentication session is restarted.
     * @param name {@code String} If {@code null} if provided then the method will return {@code null}.
     * @return {@code String} or {@code null} if no client's note is set.
     */
    String getClientNote(String name);

    /**
     * Sets the given client note to the given value. Client notes are notes
     * specific to client protocol. They are NOT cleared when authentication session is restarted.
     * @param name {@code String} If {@code null} is provided the method won't have an effect.
     * @param value {@code String} If {@code null} is provided the method won't have an effect.
     */
    void setClientNote(String name, String value);

    /**
     * Removes the given client note. Client notes are notes
     * specific to client protocol. They are NOT cleared when authentication session is restarted.
     * @param name {@code String} If {@code null} is provided the method won't have an effect.
     */
    void removeClientNote(String name);

    /**
     * Retrieves the (name, value) map of client notes. Client notes are notes
     * specific to client protocol. They are NOT cleared when authentication session is restarted.
     * @return {@code Map<String, String>} never returns {@code null}.
     */
    Map<String, String> getClientNotes();

    /**
     * Clears all client notes. Client notes are notes
     * specific to client protocol. They are NOT cleared when authentication session is restarted.
     */
    void clearClientNotes();

    /**
     * Gets client scope IDs from the authentication session.
     * @return {@code Set<String>} never returns {@code null}.
     */
    Set<String> getClientScopes();

    /**
     * Sets client scope IDs to the authentication session.
     * @param clientScopes {@code Set<String>} Can't be {@code null}.
     */
    void setClientScopes(Set<String> clientScopes);
}
