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

package org.keycloak.models.sessions.infinispan.entities;

import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.jboss.logging.Logger;
import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@ProtoTypeId(Marshalling.USER_SESSION_ENTITY)
public class UserSessionEntity extends SessionEntity {

    public static final Logger logger = Logger.getLogger(UserSessionEntity.class);

    // Metadata attribute, which contains the lastSessionRefresh available on remoteCache. Used in decide whether we need to write to remoteCache (DC) or not
    public static final String LAST_SESSION_REFRESH_REMOTE = "lsrr";

    private final String id;

    private String user;

    private String brokerSessionId;
    private String brokerUserId;

    private String loginUsername;

    private String ipAddress;

    private String authMethod;

    private boolean rememberMe;

    private int started;

    private int lastSessionRefresh;

    private UserSessionModel.State state;

    public UserSessionEntity(String id) {
        this.id = id;
    }

    @ProtoFactory
    static UserSessionEntity protoFactory(String realmId, String id, String user, String loginUsername, String ipAddress, String authMethod, boolean rememberMe, int started, int lastSessionRefresh, Map<String, String> notes, AuthenticatedClientSessionStore authenticatedClientSessions, UserSessionModel.State state, String brokerSessionId, String brokerUserId) {
        var entity = new UserSessionEntity(id);
        entity.setRealmId(realmId);
        entity.setUser(user);
        entity.setLoginUsername(loginUsername);
        entity.setIpAddress(ipAddress);
        entity.setAuthMethod(authMethod);
        entity.setRememberMe(rememberMe);
        entity.setStarted(started);
        entity.setLastSessionRefresh(lastSessionRefresh);
        entity.setBrokerSessionId(brokerSessionId);
        entity.setBrokerUserId(brokerUserId);
        entity.setState(state);
        entity.setNotes(notes);
        entity.setAuthenticatedClientSessions(authenticatedClientSessions);
        return entity;
    }

    @ProtoField(2)
    public String getId() {
        return id;
    }

    private Map<String, String> notes = new ConcurrentHashMap<>();

    private AuthenticatedClientSessionStore authenticatedClientSessions = new AuthenticatedClientSessionStore();

    @ProtoField(3)
    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    @ProtoField(4)
    public String getLoginUsername() {
        return loginUsername;
    }

    public void setLoginUsername(String loginUsername) {
        this.loginUsername = loginUsername;
    }

    @ProtoField(5)
    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @ProtoField(6)
    public String getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(String authMethod) {
        this.authMethod = authMethod;
    }

    @ProtoField(7)
    public boolean isRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    @ProtoField(8)
    public int getStarted() {
        return started;
    }

    public void setStarted(int started) {
        this.started = started;
    }

    @ProtoField(9)
    public int getLastSessionRefresh() {
        return lastSessionRefresh;
    }

    public void setLastSessionRefresh(int lastSessionRefresh) {
        this.lastSessionRefresh = lastSessionRefresh;
    }

    @ProtoField(value = 10, mapImplementation = ConcurrentHashMap.class)
    public Map<String, String> getNotes() {
        return notes;
    }

    public void setNotes(Map<String, String> notes) {
        this.notes = notes;
    }

    @ProtoField(11)
    public AuthenticatedClientSessionStore getAuthenticatedClientSessions() {
        return authenticatedClientSessions;
    }

    public void setAuthenticatedClientSessions(AuthenticatedClientSessionStore authenticatedClientSessions) {
        this.authenticatedClientSessions = authenticatedClientSessions;
    }

    @ProtoField(value = 12)
    public UserSessionModel.State getState() {
        return state;
    }

    public void setState(UserSessionModel.State state) {
        this.state = state;
    }

    @ProtoField(13)
    public String getBrokerSessionId() {
        return brokerSessionId;
    }

    public void setBrokerSessionId(String brokerSessionId) {
        this.brokerSessionId = brokerSessionId;
    }

    @ProtoField(14)
    public String getBrokerUserId() {
        return brokerUserId;
    }

    public void setBrokerUserId(String brokerUserId) {
        this.brokerUserId = brokerUserId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof UserSessionEntity that &&
                Objects.equals(id, that.id);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return String.format("UserSessionEntity [id=%s, realm=%s, lastSessionRefresh=%d, clients=%s]", getId(), getRealmId(), getLastSessionRefresh(),
          new TreeSet(this.authenticatedClientSessions.keySet()));
    }

    @Override
    public SessionEntityWrapper mergeRemoteEntityWithLocalEntity(SessionEntityWrapper localEntityWrapper) {
        int lsrRemote = getLastSessionRefresh();

        SessionEntityWrapper entityWrapper;
        if (localEntityWrapper == null) {
            entityWrapper = new SessionEntityWrapper<>(this);
        } else {
            UserSessionEntity localUserSession = (UserSessionEntity) localEntityWrapper.getEntity();

            // local lastSessionRefresh should always contain the bigger
            if (lsrRemote < localUserSession.getLastSessionRefresh()) {
                setLastSessionRefresh(localUserSession.getLastSessionRefresh());
            }

            entityWrapper = new SessionEntityWrapper<>(localEntityWrapper.getLocalMetadata(), this);
        }

        entityWrapper.putLocalMetadataNoteInt(LAST_SESSION_REFRESH_REMOTE, lsrRemote);

        logger.debugf("Updating session entity '%s'. lastSessionRefresh=%d, lastSessionRefreshRemote=%d", getId(), getLastSessionRefresh(), lsrRemote);

        return entityWrapper;
    }

}
