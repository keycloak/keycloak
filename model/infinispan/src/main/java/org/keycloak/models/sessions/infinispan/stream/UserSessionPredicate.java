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

import java.util.Map;
import java.util.function.Predicate;

import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.infinispan.AuthenticatedClientSessionAdapter;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@ProtoTypeId(Marshalling.USER_SESSION_PREDICATE)
public class UserSessionPredicate implements Predicate<Map.Entry<String, SessionEntityWrapper<UserSessionEntity>>> {

    private final String realm;

    private String user;

    private String client;

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
    @ProtoField(1)
    public String getUserId() {
        return user;
    }

    @ProtoField(2)
    public String getBrokerSessionId() {
        return brokerSessionId;
    }

    @ProtoField(3)
    public String getBrokerUserId() {
        return brokerUserId;
    }

    @ProtoField(4)
    String getRealm() {
        return realm;
    }

    @ProtoField(5)
    public String getClient() {
        return client;
    }

    @ProtoFactory
    static UserSessionPredicate create(String userId, String brokerSessionId, String brokerUserId, String realm, String client) {
        return create(realm)
                .user(userId)
                .client(client)
                .brokerSessionId(brokerSessionId)
                .brokerUserId(brokerUserId);
    }

    @Override
    public boolean test(Map.Entry<String, SessionEntityWrapper<UserSessionEntity>> entry) {
        UserSessionEntity entity = entry.getValue().getEntity();

        return realm.equals(entity.getRealmId()) &&
                (user == null || entity.getUser().equals(user)) &&
                (client == null || entity.getClientSessions().contains(client)) &&
                (brokerSessionId == null || brokerSessionId.equals(entity.getBrokerSessionId())) &&
                (brokerUserId == null || brokerUserId.equals(entity.getBrokerUserId()));

    }

    public Predicate<? super UserSessionModel> toModelPredicate() {

        return (Predicate<UserSessionModel>) entity ->
                entity != null && realm.equals(entity.getRealm().getId()) &&
                        (user == null || entity.getUser().getId().equals(user)) &&
                        (client == null || (entity.getAuthenticatedClientSessions() != null && entity.getAuthenticatedClientSessions().containsKey(client))) &&
                        (brokerSessionId == null || brokerSessionId.equals(entity.getBrokerSessionId())) &&
                        (brokerUserId == null || brokerUserId.equals(entity.getBrokerUserId()));
    }


}
