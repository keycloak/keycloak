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

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * Using class for now to avoid many updates among implementations
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface AuthenticationSessionModel extends CommonClientSessionModel {

//
//    public UserSessionModel getUserSession();
//    public void setUserSession(UserSessionModel userSession);


    Map<String, ExecutionStatus> getExecutionStatus();
    void setExecutionStatus(String authenticator, ExecutionStatus status);
    void clearExecutionStatus();
    UserModel getAuthenticatedUser();
    void setAuthenticatedUser(UserModel user);

    /**
     * Required actions that are attached to this client session.
     *
     * @return
     */
    Set<String> getRequiredActions();

    void addRequiredAction(String action);

    void removeRequiredAction(String action);

    void addRequiredAction(UserModel.RequiredAction action);

    void removeRequiredAction(UserModel.RequiredAction action);


    // These are notes you want applied to the UserSessionModel when the client session is attached to it.
    void setUserSessionNote(String name, String value);
    Map<String, String> getUserSessionNotes();
    void clearUserSessionNotes();

    // These are notes used typically by authenticators and authentication flows. They are cleared when authentication session is restarted
    String getAuthNote(String name);
    void setAuthNote(String name, String value);
    void removeAuthNote(String name);
    void clearAuthNotes();

    // These are notes specific to client protocol. They are NOT cleared when authentication session is restarted
    String getClientNote(String name);
    void setClientNote(String name, String value);
    void removeClientNote(String name);
    Map<String, String> getClientNotes();
    void clearClientNotes();

    void updateClient(ClientModel client);

    // Will completely restart whole state of authentication session. It will just keep same ID. It will setup it with provided realm and client.
    void restartSession(RealmModel realm, ClientModel client);
}
