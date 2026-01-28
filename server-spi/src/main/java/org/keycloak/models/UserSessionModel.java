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
import java.util.Objects;

import org.keycloak.util.EnumWithStableIndex;

import org.infinispan.protostream.annotations.Proto;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface UserSessionModel {

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

    /**
     * Note: will not be an address when a proxy does not provide a valid one
     *
     * @return the ip address
     */
    String getIpAddress();

    String getAuthMethod();

    boolean isRememberMe();

    int getStarted();

    int getLastSessionRefresh();

    /**
     * Set the last session refresh timestamp for the user session.
     * If the timestamp is smaller or equal than the current timestamp, the operation is ignored.
     */
    void setLastSessionRefresh(int seconds);

    boolean isOffline();

    /**
     * Returns map where key is ID of the client (its UUID) and value is ID respective {@link AuthenticatedClientSessionModel} object.
     * <p>
     * Any direct modification via the {@link Map} interface will throw an {@link UnsupportedOperationException}. To add a
     * new mapping, use a method like {@link UserSessionProvider#createClientSession(RealmModel, ClientModel, UserSessionModel)} or
     * equivalent. To remove a mapping, use {@link AuthenticatedClientSessionModel#detachFromUserSession()}.
     */
    Map<String, AuthenticatedClientSessionModel> getAuthenticatedClientSessions();
    /**
     * Returns a client session for the given client UUID.
     * @return
     */
    default AuthenticatedClientSessionModel getAuthenticatedClientSessionByClient(String clientUUID) {
        return getAuthenticatedClientSessions().get(clientUUID);
    }

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

    @ProtoTypeId(65536) // see org.keycloak.Marshalling
    @Proto
    enum State implements EnumWithStableIndex {
        LOGGED_IN(0),
        LOGGING_OUT(1),
        LOGGED_OUT(2),
        LOGGED_OUT_UNCONFIRMED(3);

        private final int stableIndex;
        private static final Map<Integer, State> BY_ID = EnumWithStableIndex.getReverseIndex(values());

        private State(int stableIndex) {
            Objects.requireNonNull(stableIndex);
            this.stableIndex = stableIndex;
        }

        @Override
        public int getStableIndex() {
            return stableIndex;
        }

        public static State valueOfInteger(Integer id) {
            return id == null ? null : BY_ID.get(id);
        }
    }

    /**
     * @return Persistence state of the session
     */
    default SessionPersistenceState getPersistenceState() {
        return SessionPersistenceState.PERSISTENT;
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
