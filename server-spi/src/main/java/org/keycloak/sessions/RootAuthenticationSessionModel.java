/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;

/**
 * Represents usually one browser session with potentially many browser tabs. Every browser tab is represented by {@link AuthenticationSessionModel}
 * of different client.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface RootAuthenticationSessionModel {

    String getId();
    RealmModel getRealm();

    int getTimestamp();
    void setTimestamp(int timestamp);


    /**
     * Key is tabId, Value is AuthenticationSessionModel.
     * @return authentication sessions or empty map if no authenticationSessions presents. Never return null.
     */
    Map<String, AuthenticationSessionModel> getAuthenticationSessions();


    /**
     * @return authentication session for particular client and tab or null if it doesn't yet exists.
     */
    AuthenticationSessionModel getAuthenticationSession(ClientModel client, String tabId);


    /**
     * Create new authentication session and returns it. Overwrites existing session for particular client if already exists.
     *
     * @param client
     * @return non-null fresh authentication session
     */
    AuthenticationSessionModel createAuthenticationSession(ClientModel client);

    /**
     * Removes authentication session from root authentication session.
     * If there's no child authentication session left in the root authentication session, it's removed as well.
     * @param tabId String
     */
    void removeAuthenticationSessionByTabId(String tabId);

    /**
     * Will completely restart whole state of authentication session. It will just keep same ID. It will setup it with provided realm.
     */
    void restartSession(RealmModel realm);

}
