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

package org.keycloak.models.sessions.infinispan.changes.remote.updater.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.RemoteCache;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.infinispan.changes.remote.RemoteChangeLogTransaction;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.BaseUpdater;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.Expiration;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.Updater;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.UpdaterFactory;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.helper.MapUpdater;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.SessionKey;
import org.keycloak.models.sessions.infinispan.util.SessionTimeouts;

/**
 * An {@link Updater} implementation that keeps track of {@link AuthenticatedClientSessionModel} changes.
 */
public class AuthenticatedClientSessionUpdater extends BaseUpdater<SessionKey, AuthenticatedClientSessionEntity> implements AuthenticatedClientSessionModel {

    public static final UpdaterFactory<SessionKey, AuthenticatedClientSessionEntity, AuthenticatedClientSessionUpdater> FACTORY = new Factory();

    private final MapUpdater<String, String> notesUpdater;
    private final List<Consumer<AuthenticatedClientSessionEntity>> changes;
    private UserSessionModel userSession;
    private ClientModel client;
    private RemoteChangeLogTransaction<SessionKey, AuthenticatedClientSessionEntity, AuthenticatedClientSessionUpdater> clientTransaction;

    private AuthenticatedClientSessionUpdater(SessionKey cacheKey, AuthenticatedClientSessionEntity cacheValue, long version, UpdaterState initialState) {
        super(cacheKey, cacheValue, version, initialState);
        if (cacheValue == null) {
            assert initialState == UpdaterState.DELETED; // cannot be undone
            notesUpdater = null;
            changes = List.of();
            return;
        }
        initNotes(cacheValue);
        notesUpdater = new MapUpdater<>(cacheValue.getNotes());
        changes = new ArrayList<>(4);
    }

    @Override
    public AuthenticatedClientSessionEntity apply(SessionKey uuid, AuthenticatedClientSessionEntity entity) {
        initNotes(entity);
        notesUpdater.applyChanges(entity.getNotes());
        changes.forEach(change -> change.accept(entity));
        return entity;
    }

    @Override
    public Expiration computeExpiration() {
        long maxIdle;
        long lifespan;
        if (getKey().offline()) {
            maxIdle = SessionTimeouts.getOfflineClientSessionMaxIdleMs(userSession.getRealm(), client, getValue());
            lifespan = SessionTimeouts.getOfflineClientSessionLifespanMs(userSession.getRealm(), client, getValue());
        } else {
            maxIdle = SessionTimeouts.getClientSessionMaxIdleMs(userSession.getRealm(), client, getValue());
            lifespan = SessionTimeouts.getClientSessionLifespanMs(userSession.getRealm(), client, getValue());
        }
        return new Expiration(maxIdle, lifespan);
    }

    @Override
    public String getId() {
        return getValue().getId().toString();
    }

    @Override
    public int getTimestamp() {
        return getValue().getTimestamp();
    }

    @Override
    public void setTimestamp(int timestamp) {
        addAndApplyChange(entity -> entity.setTimestamp(timestamp));
    }

    @Override
    public void detachFromUserSession() {
        clientTransaction.remove(getKey());
    }

    @Override
    public UserSessionModel getUserSession() {
        return userSession;
    }

    @Override
    public String getNote(String name) {
        return notesUpdater.get(name);
    }

    @Override
    public void setNote(String name, String value) {
        notesUpdater.put(name, value);
    }

    @Override
    public void removeNote(String name) {
        notesUpdater.remove(name);
    }

    @Override
    public Map<String, String> getNotes() {
        return notesUpdater;
    }

    @Override
    public String getRedirectUri() {
        return getValue().getRedirectUri();
    }

    @Override
    public void setRedirectUri(String uri) {
        addAndApplyChange(entity -> entity.setRedirectUri(uri));
    }

    @Override
    public RealmModel getRealm() {
        return userSession.getRealm();
    }

    @Override
    public ClientModel getClient() {
        return client;
    }

    @Override
    public String getAction() {
        return getValue().getAction();
    }

    @Override
    public void setAction(String action) {
        addAndApplyChange(entity -> entity.setAction(action));
    }

    @Override
    public String getProtocol() {
        return getValue().getAuthMethod();
    }

    @Override
    public void setProtocol(String method) {
        addAndApplyChange(entity -> entity.setAuthMethod(method));
    }

    @Override
    public boolean isTransient() {
        return !isDeleted() && userSession.getPersistenceState() == UserSessionModel.SessionPersistenceState.TRANSIENT;
    }

    @Override
    protected boolean isUnchanged() {
        return changes.isEmpty() && notesUpdater.isUnchanged();
    }

    /**
     * Initializes this class with references to other models classes.
     *
     * @param userSession       The {@link UserSessionModel} associated with this client session.
     * @param client            The {@link ClientModel} associated with this client session.
     * @param clientTransaction The {@link RemoteChangeLogTransaction} to perform the changes in this class into the
     *                          {@link RemoteCache}.
     */
    public synchronized void initialize(UserSessionModel userSession, ClientModel client, RemoteChangeLogTransaction<SessionKey, AuthenticatedClientSessionEntity, AuthenticatedClientSessionUpdater> clientTransaction) {
        this.userSession = Objects.requireNonNull(userSession);
        this.client = Objects.requireNonNull(client);
        this.clientTransaction = Objects.requireNonNull(clientTransaction);
    }

    /**
     * @return {@code true} if it is already initialized.
     */
    public synchronized boolean isInitialized() {
        return userSession != null;
    }

    /**
     * Keeps track of a model changes and applies it to the entity.
     */
    private void addAndApplyChange(Consumer<AuthenticatedClientSessionEntity> change) {
        changes.add(change);
        change.accept(getValue());
    }

    private static void initNotes(AuthenticatedClientSessionEntity entity) {
        var notes = entity.getNotes();
        if (notes == null) {
            entity.setNotes(new HashMap<>());
        }
    }

    private static class Factory implements UpdaterFactory<SessionKey, AuthenticatedClientSessionEntity, AuthenticatedClientSessionUpdater> {

        @Override
        public AuthenticatedClientSessionUpdater create(SessionKey key, AuthenticatedClientSessionEntity entity) {
            return new AuthenticatedClientSessionUpdater(key, Objects.requireNonNull(entity), -1, UpdaterState.CREATED);
        }

        @Override
        public AuthenticatedClientSessionUpdater wrapFromCache(SessionKey key, MetadataValue<AuthenticatedClientSessionEntity> entity) {
            assert entity != null;
            return new AuthenticatedClientSessionUpdater(key, Objects.requireNonNull(entity.getValue()), entity.getVersion(), UpdaterState.READ);
        }

        @Override
        public AuthenticatedClientSessionUpdater deleted(SessionKey key) {
            return new AuthenticatedClientSessionUpdater(key, null, -1, UpdaterState.DELETED);
        }
    }

}
