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

import org.keycloak.storage.SearchableModelField;

import java.util.Collection;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface UserSessionModel {

    class SearchableFields {
        public static final SearchableModelField<UserSessionModel> ID       = new SearchableModelField<>("id", String.class);

        /**
         * Represents the corresponding offline user session for the online user session.
         * null if there is no corresponding offline user session.
         */
        public static final SearchableModelField<UserSessionModel> CORRESPONDING_SESSION_ID = new SearchableModelField<>("correspondingSessionId", String.class);
        public static final SearchableModelField<UserSessionModel> REALM_ID = new SearchableModelField<>("realmId", String.class);
        public static final SearchableModelField<UserSessionModel> USER_ID  = new SearchableModelField<>("userId", String.class);
        public static final SearchableModelField<UserSessionModel> CLIENT_ID  = new SearchableModelField<>("clientId", String.class);
        public static final SearchableModelField<UserSessionModel> BROKER_SESSION_ID  = new SearchableModelField<>("brokerSessionId", String.class);
        public static final SearchableModelField<UserSessionModel> BROKER_USER_ID  = new SearchableModelField<>("brokerUserId", String.class);
        public static final SearchableModelField<UserSessionModel> IS_OFFLINE  = new SearchableModelField<>("isOffline", Boolean.class);
        public static final SearchableModelField<UserSessionModel> LAST_SESSION_REFRESH  = new SearchableModelField<>("lastSessionRefresh", Integer.class);
    }

    /**
     * Represents the corresponding online/offline user session.
     */
    String CORRESPONDING_SESSION_ID = "correspondingSessionId";

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
        LOGGED_OUT,
        LOGGED_OUT_UNCONFIRMED;
    }

    /**
     * Flag used when creating user session
     */
    enum SessionPersistenceState {

        /**
         * Session will be marked as persistent when created and it will be saved into the persistent storage (EG. infinispan cache).
         * This is the default behaviour
         */
        PERSISTENT,

        /**
         *  This userSession will be valid just for the single request. Hence there won't be real
         *  userSession created in the persistent store. Flag can be used for the protocols, which need just "dummy"
         *  userSession to be able to run protocolMappers SPI. Example is DockerProtocol or OAuth2 client credentials grant.
         */
        TRANSIENT;

        public static SessionPersistenceState fromString(String sessionPersistenceString) {
            return (sessionPersistenceString == null) ? PERSISTENT : Enum.valueOf(SessionPersistenceState.class, sessionPersistenceString);
        }
    }

}
