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

package org.keycloak.models.sessions.infinispan.stream;

import org.keycloak.models.sessions.infinispan.AuthenticatedClientSessionAdapter;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

import org.keycloak.models.sessions.infinispan.util.KeycloakMarshallUtil;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;
import java.util.function.Predicate;
import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@SerializeWith(UserSessionPredicate.ExternalizerImpl.class)
public class UserSessionPredicate implements Predicate<Map.Entry<String, SessionEntityWrapper<UserSessionEntity>>> {

    private final String realm;

    private String user;

    private String client;

    private Integer expired;

    private Integer expiredRefresh;

    private Integer expiredRememberMe;

    private Integer expiredRefreshRememberMe;

    private String brokerSessionId;
    private String brokerUserId;

    private UserSessionPredicate(String realm) {
        this.realm = realm;
    }

    /**
     * Creates a user session predicate. If using the {@link #client(java.lang.String)} method, see its warning.
     * @param realm
     * @return
     */
    public static UserSessionPredicate create(String realm) {
        return new UserSessionPredicate(realm);
    }

    public UserSessionPredicate user(String user) {
        this.user = user;
        return this;
    }

    /**
     * Adds a test for client. Note that this test can return stale sessions because on detaching client session
     * from user session, only client session is deleted and user session is not updated for performance reason.
     *
     * @see AuthenticatedClientSessionAdapter#detachFromUserSession()
     * @param clientUUID
     * @return
     */
    public UserSessionPredicate client(String clientUUID) {
        this.client = clientUUID;
        return this;
    }

    public UserSessionPredicate expired(Integer expired, Integer expiredRefresh) {
        return this.expired(expired, expiredRefresh, null, null);
    }

    public UserSessionPredicate expired(Integer expired, Integer expiredRefresh, Integer expiredRememberMe, Integer expiredRefreshRememberMe) {
        this.expired = expired;
        this.expiredRefresh = expiredRefresh;
        this.expiredRememberMe = expiredRememberMe;
        this.expiredRefreshRememberMe = expiredRefreshRememberMe;
        return this;
    }


    public UserSessionPredicate brokerSessionId(String id) {
        this.brokerSessionId = id;
        return this;
    }

    public UserSessionPredicate brokerUserId(String id) {
        this.brokerUserId = id;
        return this;
    }

    /**
     * Returns the user id.
     * @return
     */
    public String getUserId() {
        return user;
    }

    public String getBrokerSessionId() {
        return brokerSessionId;
    }

    public String getBrokerUserId() {
        return brokerUserId;
    }

    @Override
    public boolean test(Map.Entry<String, SessionEntityWrapper<UserSessionEntity>> entry) {
        UserSessionEntity entity = entry.getValue().getEntity();

        if (!realm.equals(entity.getRealmId())) {
            return false;
        }

        if (user != null && !entity.getUser().equals(user)) {
            return false;
        }

        if (client != null && (entity.getAuthenticatedClientSessions() == null || !entity.getAuthenticatedClientSessions().containsKey(client))) {
            return false;
        }

        if (brokerSessionId != null && !brokerSessionId.equals(entity.getBrokerSessionId())) {
            return false;
        }

        if (brokerUserId != null && !brokerUserId.equals(entity.getBrokerUserId())) {
            return false;
        }

        if (entity.isRememberMe()) {
            if (expiredRememberMe != null && expiredRefreshRememberMe != null && entity.getStarted() > expiredRememberMe && entity.getLastSessionRefresh() > expiredRefreshRememberMe) {
                return false;
            }
        }
        else {
            if (expired != null && expiredRefresh != null && entity.getStarted() > expired && entity.getLastSessionRefresh() > expiredRefresh) {
                return false;
            }
        }

        if (expired == null && expiredRefresh != null && entity.getLastSessionRefresh() > expiredRefresh) {
            return false;
        }
        return true;
    }

    public static class ExternalizerImpl implements Externalizer<UserSessionPredicate> {

        private static final int VERSION_1 = 1;
        private static final int VERSION_2 = 2;

        @Override
        public void writeObject(ObjectOutput output, UserSessionPredicate obj) throws IOException {
            output.writeByte(VERSION_2);

            MarshallUtil.marshallString(obj.realm, output);
            MarshallUtil.marshallString(obj.user, output);
            MarshallUtil.marshallString(obj.client, output);
            KeycloakMarshallUtil.marshall(obj.expired, output);
            KeycloakMarshallUtil.marshall(obj.expiredRefresh, output);
            KeycloakMarshallUtil.marshall(obj.expiredRememberMe, output);
            KeycloakMarshallUtil.marshall(obj.expiredRefreshRememberMe, output);
            MarshallUtil.marshallString(obj.brokerSessionId, output);
            MarshallUtil.marshallString(obj.brokerUserId, output);

        }

        @Override
        public UserSessionPredicate readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                case VERSION_2:
                    return readObjectVersion2(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public UserSessionPredicate readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            UserSessionPredicate res = new UserSessionPredicate(MarshallUtil.unmarshallString(input));
            res.user(MarshallUtil.unmarshallString(input));
            res.client(MarshallUtil.unmarshallString(input));
            res.expired(KeycloakMarshallUtil.unmarshallInteger(input), KeycloakMarshallUtil.unmarshallInteger(input));
            res.brokerSessionId(MarshallUtil.unmarshallString(input));
            res.brokerUserId(MarshallUtil.unmarshallString(input));
            return res;
        }

        public UserSessionPredicate readObjectVersion2(ObjectInput input) throws IOException, ClassNotFoundException {
            UserSessionPredicate res = new UserSessionPredicate(MarshallUtil.unmarshallString(input));
            res.user(MarshallUtil.unmarshallString(input));
            res.client(MarshallUtil.unmarshallString(input));
            res.expired(KeycloakMarshallUtil.unmarshallInteger(input), KeycloakMarshallUtil.unmarshallInteger(input),
                    KeycloakMarshallUtil.unmarshallInteger(input), KeycloakMarshallUtil.unmarshallInteger(input));
            res.brokerSessionId(MarshallUtil.unmarshallString(input));
            res.brokerUserId(MarshallUtil.unmarshallString(input));
            return res;
        }
    }
}
