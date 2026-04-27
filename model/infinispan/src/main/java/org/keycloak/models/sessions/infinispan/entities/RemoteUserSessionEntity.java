/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.keycloak.common.util.Time;
import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.OfflineUserSessionModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;

import org.infinispan.api.annotations.indexing.Basic;
import org.infinispan.api.annotations.indexing.Indexed;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

@ProtoTypeId(Marshalling.REMOTE_USER_SESSION_ENTITY)
@Indexed
public class RemoteUserSessionEntity {

    // immutable state
    private final String userSessionId;

    // mutable state
    private String realmId;
    private String userId;
    private String brokerSessionId;
    private String brokerUserId;
    private String loginUsername;
    private String ipAddress;
    private String authMethod;
    private boolean rememberMe;
    private int started;
    private int lastSessionRefresh;
    private UserSessionModel.State state;
    private Map<String, String> notes;

    private RemoteUserSessionEntity(String userSessionId) {
        this.userSessionId = Objects.requireNonNull(userSessionId);
    }

    public static RemoteUserSessionEntity create(String id, RealmModel realm, UserModel user, String loginUsername, String ipAddress, String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId) {
        var e = new RemoteUserSessionEntity(id);
        e.restart(realm.getId(), user.getId(), loginUsername, ipAddress, authMethod, rememberMe, brokerSessionId, brokerUserId);
        return e;
    }

    public static RemoteUserSessionEntity createFromModel(UserSessionModel model) {
        String userId;
        String loginUsername = null;
        if (model instanceof OfflineUserSessionModel offline) {
            // this is a hack so that UserModel doesn't have to be available when offline token is imported.
            // see related JIRA - KEYCLOAK-5350 and corresponding test
            userId = offline.getUserId();
            // NOTE: Hack
            // We skip calling entity.setLoginUsername(userSession.getLoginUsername())
        } else {
            userId = model.getUser().getId();
            loginUsername = model.getLoginUsername();
        }
        var e = new RemoteUserSessionEntity(model.getId());
        e.restart(model.getRealm().getId(), userId, loginUsername, model.getIpAddress(), model.getAuthMethod(), model.isRememberMe(), model.getBrokerSessionId(), model.getBrokerUserId());
        var notes = model.getNotes();
        if (notes != null && !notes.isEmpty()) {
            e.notes = new HashMap<>(notes);
        }
        e.state = model.getState();
        return e;
    }

    // for testing purposes only!
    public static RemoteUserSessionEntity mockEntity(String id, String realmId, String userId) {
        return mockEntity(id, realmId, userId, null, null);
    }

    // for testing purposes only!
    public static RemoteUserSessionEntity mockEntity(String id, String realmId, String userId, String brokerSessionId, String brokerUserId) {
        var e = new RemoteUserSessionEntity(id);
        e.realmId = realmId;
        e.userId = userId;
        e.brokerSessionId = brokerSessionId;
        e.brokerUserId = brokerUserId;
        return e;
    }

    @ProtoFactory
    static RemoteUserSessionEntity protoFactory(String userSessionId, String authMethod, String brokerSessionId, String brokerUserId, String ipAddress, int lastSessionRefresh, String loginUsername, Map<String, String> notes, String realmId, boolean rememberMe, int started, UserSessionModel.State state, String userId) {
        var e = new RemoteUserSessionEntity(userSessionId);
        e.applyState(authMethod, brokerSessionId, brokerUserId, ipAddress, lastSessionRefresh, loginUsername, notes, realmId, rememberMe, started, state, userId);
        return e;
    }

    @ProtoField(1)
    @Basic(sortable = true)
    public String getUserSessionId() {
        return userSessionId;
    }

    @ProtoField(2)
    public String getAuthMethod() {
        return authMethod;
    }

    @ProtoField(3)
    @Basic
    public String getBrokerSessionId() {
        return brokerSessionId;
    }

    @ProtoField(4)
    @Basic
    public String getBrokerUserId() {
        return brokerUserId;
    }

    @ProtoField(5)
    public String getIpAddress() {
        return ipAddress;
    }

    @ProtoField(6)
    public int getLastSessionRefresh() {
        return lastSessionRefresh;
    }

    public void setLastSessionRefresh(int lastSessionRefresh) {
        this.lastSessionRefresh = Math.max(this.lastSessionRefresh, lastSessionRefresh);
    }

    @ProtoField(7)
    public String getLoginUsername() {
        return loginUsername;
    }

    @ProtoField(value = 8, mapImplementation = HashMap.class)
    public Map<String, String> getNotes() {
        return notes;
    }

    public void setNotes(Map<String, String> notes) {
        this.notes = notes;
    }

    @ProtoField(9)
    @Basic
    public String getRealmId() {
        return realmId;
    }

    @ProtoField(10)
    public boolean isRememberMe() {
        return rememberMe;
    }

    @ProtoField(11)
    public int getStarted() {
        return started;
    }

    @ProtoField(12)
    public UserSessionModel.State getState() {
        return state;
    }

    public void setState(UserSessionModel.State state) {
        this.state = state;
    }

    @ProtoField(13)
    @Basic
    public String getUserId() {
        return userId;
    }

    public void restart(String realmId, String userId, String loginUsername, String ipAddress, String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId) {
        var currentTime = Time.currentTime();
        applyState(authMethod, brokerSessionId, brokerUserId, ipAddress, currentTime, loginUsername, null, realmId, rememberMe, currentTime, null, userId);
    }

    private void applyState(String authMethod, String brokerSessionId, String brokerUserId, String ipAddress, int lastSessionRefresh, String loginUsername, Map<String, String> notes, String realmId, boolean rememberMe, int started, UserSessionModel.State state, String userId) {
        this.realmId = realmId;
        this.userId = userId;
        this.loginUsername = loginUsername;
        this.ipAddress = ipAddress;
        this.authMethod = authMethod;
        this.rememberMe = rememberMe;
        this.brokerSessionId = brokerSessionId;
        this.brokerUserId = brokerUserId;
        this.started = started;
        this.lastSessionRefresh = lastSessionRefresh;
        this.notes = notes;
        this.state = state;
    }
}
