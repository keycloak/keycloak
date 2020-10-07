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

package org.keycloak.models;

import java.util.Collection;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface UserSessionModel {

    String getId();
    RealmModel getRealm();

    /**
     * If created via a broker external login, this is an identifier that can be
     * used to match external broker backchannel logout requests to a UserSession
     *
     * @return
     */
    String getBrokerSessionId();
    String getBrokerUserId();

    UserModel getUser();

    String getLoginUsername();

    String getIpAddress();

    String getAuthMethod();

    boolean isRememberMe();

    int getStarted();

    int getLastSessionRefresh();

    void setLastSessionRefresh(int seconds);

    boolean isOffline();

    /**
     * Returns map where key is ID of the client (its UUID) and value is ID respective {@link AuthenticatedClientSessionModel} object.
     * @return 
     */
    Map<String, AuthenticatedClientSessionModel> getAuthenticatedClientSessions();
    /**
     * Returns a client session for the given client UUID.
     * @return
     */
    default AuthenticatedClientSessionModel getAuthenticatedClientSessionByClient(String clientUUID) {
        return getAuthenticatedClientSessions().get(clientUUID);
    };
    /**
     * Removes authenticated client sessions for all clients whose UUID is present in {@code removedClientUUIDS} parameter.
     * @param removedClientUUIDS
     */
    void removeAuthenticatedClientSessions(Collection<String> removedClientUUIDS);


    String getNote(String name);
    void setNote(String name, String value);
    void removeNote(String name);
    Map<String, String> getNotes();

    State getState();
    void setState(State state);

    // Will completely restart whole state of user session. It will just keep same ID.
    void restartSession(RealmModel realm, UserModel user, String loginUsername, String ipAddress, String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId);

    enum State {
        LOGGED_IN,
        LOGGING_OUT,
        LOGGED_OUT
    }

}
