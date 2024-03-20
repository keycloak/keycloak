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

package org.keycloak.models.sessions.infinispan.changes;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.jboss.logging.Logger;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Retry;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.delegate.ClientModelLazyDelegate;
import org.keycloak.models.session.PersistentAuthenticatedClientSessionAdapter;
import org.keycloak.models.session.PersistentUserSessionAdapter;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.sessions.infinispan.CacheDecorators;
import org.keycloak.models.sessions.infinispan.SessionFunction;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionStore;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.sessions.infinispan.remotestore.RemoteCacheInvoker;
import org.keycloak.connections.infinispan.InfinispanUtil;
import org.keycloak.models.utils.RealmModelDelegate;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.models.utils.UserSessionModelDelegate;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanChangelogBasedTransaction<K, V extends SessionEntity> extends AbstractKeycloakTransaction {

    public static final Logger logger = Logger.getLogger(InfinispanChangelogBasedTransaction.class);

    private final KeycloakSession kcSession;
    private final String cacheName;
    private final Cache<K, SessionEntityWrapper<V>> cache;
    private final RemoteCacheInvoker remoteCacheInvoker;

    protected final Map<K, SessionUpdatesList<V>> updates = new HashMap<>();

    protected final SessionFunction<V> lifespanMsLoader;
    protected final SessionFunction<V> maxIdleTimeMsLoader;

    public InfinispanChangelogBasedTransaction(KeycloakSession kcSession, Cache<K, SessionEntityWrapper<V>> cache, RemoteCacheInvoker remoteCacheInvoker,
                                               SessionFunction<V> lifespanMsLoader, SessionFunction<V> maxIdleTimeMsLoader) {
        this.kcSession = kcSession;
        this.cacheName = cache.getName();
        this.cache = cache;
        this.remoteCacheInvoker = remoteCacheInvoker;
        this.lifespanMsLoader = lifespanMsLoader;
        this.maxIdleTimeMsLoader = maxIdleTimeMsLoader;
    }


    public void addTask(K key, SessionUpdateTask<V> task) {
        SessionUpdatesList<V> myUpdates = updates.get(key);
        if (myUpdates == null) {
            // Lookup entity from cache
            SessionEntityWrapper<V> wrappedEntity = cache.get(key);
            if (wrappedEntity == null) {
                logger.tracef("Not present cache item for key %s", key);
                return;
            }

            RealmModel realm = kcSession.realms().getRealm(wrappedEntity.getEntity().getRealmId());

            myUpdates = new SessionUpdatesList<>(realm, wrappedEntity);
            updates.put(key, myUpdates);
        }

        // Run the update now, so reader in same transaction can see it (TODO: Rollback may not work correctly. See if it's an issue..)
        task.runUpdate(myUpdates.getEntityWrapper().getEntity());
        myUpdates.add(task);
    }


    // Create entity and new version for it
    public void addTask(K key, SessionUpdateTask<V> task, V entity, UserSessionModel.SessionPersistenceState persistenceState) {
        if (entity == null) {
            throw new IllegalArgumentException("Null entity not allowed");
        }

        RealmModel realm = kcSession.realms().getRealm(entity.getRealmId());
        SessionEntityWrapper<V> wrappedEntity = new SessionEntityWrapper<>(entity);
        SessionUpdatesList<V> myUpdates = new SessionUpdatesList<>(realm, wrappedEntity, persistenceState);
        updates.put(key, myUpdates);

        // Run the update now, so reader in same transaction can see it
        task.runUpdate(entity);
        myUpdates.add(task);
    }


    public void reloadEntityInCurrentTransaction(RealmModel realm, K key, SessionEntityWrapper<V> entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Null entity not allowed");
        }

        SessionEntityWrapper<V> latestEntity = cache.get(key);
        if (latestEntity == null) {
            return;
        }

        SessionUpdatesList<V> newUpdates = new SessionUpdatesList<>(realm, latestEntity);

        SessionUpdatesList<V> existingUpdates = updates.get(key);
        if (existingUpdates != null) {
            newUpdates.setUpdateTasks(existingUpdates.getUpdateTasks());
        }

        updates.put(key, newUpdates);
    }


    public SessionEntityWrapper<V> get(K key) {
        SessionUpdatesList<V> myUpdates = updates.get(key);
        if (myUpdates == null) {
            SessionEntityWrapper<V> wrappedEntity = cache.get(key);
            if (wrappedEntity == null) {
                return null;
            }

            RealmModel realm = kcSession.realms().getRealm(wrappedEntity.getEntity().getRealmId());

            myUpdates = new SessionUpdatesList<>(realm, wrappedEntity);
            updates.put(key, myUpdates);

            return wrappedEntity;
        } else {
            boolean scheduledForRemove = isScheduledForRemove(myUpdates);

            return scheduledForRemove ? null : myUpdates.getEntityWrapper();
        }
    }

    public boolean isScheduledForRemove(K key) {
        return isScheduledForRemove(updates.get(key));
    }

    private static <V extends SessionEntity> boolean isScheduledForRemove(SessionUpdatesList<V> myUpdates) {
        if (myUpdates == null) {
            return false;
        }

        V entity = myUpdates.getEntityWrapper().getEntity();

        // If entity is scheduled for remove, we don't return it.
        boolean scheduledForRemove = myUpdates.getUpdateTasks().stream().filter((SessionUpdateTask task) -> {

            return task.getOperation(entity) == SessionUpdateTask.CacheOperation.REMOVE;

        }).findFirst().isPresent();
        return scheduledForRemove;
    }


    @Override
    protected void commitImpl() {
        for (Map.Entry<K, SessionUpdatesList<V>> entry : updates.entrySet()) {
            SessionUpdatesList<V> sessionUpdates = entry.getValue();
            SessionEntityWrapper<V> sessionWrapper = sessionUpdates.getEntityWrapper();

            // Don't save transient entities to infinispan. They are valid just for current transaction
            if (sessionUpdates.getPersistenceState() == UserSessionModel.SessionPersistenceState.TRANSIENT) continue;

            RealmModel realm = sessionUpdates.getRealm();

            long lifespanMs = lifespanMsLoader.apply(realm, sessionUpdates.getClient(), sessionWrapper.getEntity());
            long maxIdleTimeMs = maxIdleTimeMsLoader.apply(realm, sessionUpdates.getClient(), sessionWrapper.getEntity());

            MergedUpdate<V> merged = MergedUpdate.computeUpdate(sessionUpdates.getUpdateTasks(), sessionWrapper, lifespanMs, maxIdleTimeMs);

            if (Profile.isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS)) {
                Retry.executeWithBackoff(iteration -> persistUserAndClientSession(entry, sessionUpdates, merged, sessionWrapper, realm),
                        10, 10);
            }

            if (merged != null) {
                // Now run the operation in our cluster
                runOperationInCluster(entry.getKey(), merged, sessionWrapper);

                // Check if we need to send message to second DC
                remoteCacheInvoker.runTask(kcSession, realm, cacheName, entry.getKey(), merged, sessionWrapper);
            }
        }
    }

    private void persistUserAndClientSession(Map.Entry<K, SessionUpdatesList<V>> entry, SessionUpdatesList<V> sessionUpdates, MergedUpdate<V> merged, SessionEntityWrapper<V> sessionWrapper, RealmModel realm) {
        if (!sessionUpdates.getUpdateTasks().isEmpty()) {
            if (cacheName.equals("sessions")) {
                try (KeycloakSession session = kcSession.getKeycloakSessionFactory().create()) {
                    session.getTransactionManager().begin();
                    try {
                        if (merged.getOperation(sessionWrapper.getEntity()) == SessionUpdateTask.CacheOperation.REMOVE) {
                            session.getProvider(UserSessionPersisterProvider.class).removeUserSession(entry.getKey().toString(), false);
                        } else if (merged.getOperation(sessionWrapper.getEntity()) == SessionUpdateTask.CacheOperation.ADD || merged.getOperation(sessionWrapper.getEntity()) == SessionUpdateTask.CacheOperation.ADD_IF_ABSENT){
                            UserSessionEntity entity = (UserSessionEntity) sessionWrapper.getEntity();
                            session.getProvider(UserSessionPersisterProvider.class).createUserSession(new UserSessionModel() {
                                @Override
                                public String getId() {
                                    return entity.getId();
                                }

                                @Override
                                public RealmModel getRealm() {
                                    return new RealmModelDelegate(null) {
                                        @Override
                                        public String getId() {
                                            return entity.getRealmId();
                                        }
                                    };
                                }

                                @Override
                                public String getBrokerSessionId() {
                                    return entity.getBrokerSessionId();
                                }

                                @Override
                                public String getBrokerUserId() {
                                    return entity.getBrokerUserId();
                                }

                                @Override
                                public UserModel getUser() {
                                    return new UserModelDelegate(null) {
                                        @Override
                                        public String getId() {
                                            return entity.getUser();
                                        }
                                    };
                                }

                                @Override
                                public String getLoginUsername() {
                                    return entity.getLoginUsername();
                                }

                                @Override
                                public String getIpAddress() {
                                    return entity.getIpAddress();
                                }

                                @Override
                                public String getAuthMethod() {
                                    return entity.getAuthMethod();
                                }

                                @Override
                                public boolean isRememberMe() {
                                    return entity.isRememberMe();
                                }

                                @Override
                                public int getStarted() {
                                    return entity.getStarted();
                                }

                                @Override
                                public int getLastSessionRefresh() {
                                    return entity.getLastSessionRefresh();
                                }

                                @Override
                                public void setLastSessionRefresh(int seconds) {
                                    throw new IllegalStateException("not implemented");
                                }

                                @Override
                                public boolean isOffline() {
                                    return false;
                                }

                                @Override
                                public Map<String, AuthenticatedClientSessionModel> getAuthenticatedClientSessions() {
                                    // This is not used when saving this to the database.
                                    return Collections.emptyMap();
                                }

                                @Override
                                public void removeAuthenticatedClientSessions(Collection<String> removedClientUUIDS) {
                                    throw new IllegalStateException("not implemented");
                                }

                                @Override
                                public String getNote(String name) {
                                    return entity.getNotes().get(name);
                                }

                                @Override
                                public void setNote(String name, String value) {
                                    throw new IllegalStateException("not implemented");
                                }

                                @Override
                                public void removeNote(String name) {
                                    throw new IllegalStateException("not implemented");
                                }

                                @Override
                                public Map<String, String> getNotes() {
                                    return entity.getNotes();
                                }

                                @Override
                                public State getState() {
                                    return entity.getState();
                                }

                                @Override
                                public void setState(State state) {
                                    throw new IllegalStateException("not implemented");
                                }

                                @Override
                                public void restartSession(RealmModel realm, UserModel user, String loginUsername, String ipAddress, String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId) {
                                    throw new IllegalStateException("not implemented");
                                }
                            }, false);
                        } else {
                            PersistentUserSessionAdapter userSessionModel = (PersistentUserSessionAdapter) session.getProvider(UserSessionPersisterProvider.class).loadUserSession(realm, entry.getKey().toString(), false);
                            if (userSessionModel != null) {
                                UserSessionEntity userSessionEntity = new UserSessionEntity() {
                                    @Override
                                    public Map<String, String> getNotes() {
                                        return new HashMap<>() {

                                            @Override
                                            public String get(Object key) {
                                                return userSessionModel.getNotes().get(key);
                                            }

                                            @Override
                                            public String put(String key, String value) {
                                                String oldValue = userSessionModel.getNotes().get(key);
                                                userSessionModel.setNote(key, value);
                                                return oldValue;
                                            }

                                            @Override
                                            public String remove(Object key) {
                                                String oldValue = userSessionModel.getNotes().get(key);
                                                userSessionModel.removeNote(key.toString());
                                                return oldValue;
                                            }

                                            @Override
                                            public void clear() {
                                                userSessionModel.getNotes().forEach((k, v) -> userSessionModel.removeNote(k));
                                            }
                                        };
                                    }

                                    @Override
                                    public void setLastSessionRefresh(int lastSessionRefresh) {
                                        userSessionModel.setLastSessionRefresh(lastSessionRefresh);
                                    }

                                    @Override
                                    public void setState(UserSessionModel.State state) {
                                        userSessionModel.setState(state);
                                    }

                                    @Override
                                    public AuthenticatedClientSessionStore getAuthenticatedClientSessions() {
                                        return new AuthenticatedClientSessionStore() {
                                            @Override
                                            public void clear() {
                                                userSessionModel.getAuthenticatedClientSessions().clear();
                                            }
                                        };
                                    }

                                    @Override
                                    public String getRealmId() {
                                        return userSessionModel.getRealm().getId();
                                    }

                                    @Override
                                    public void setRealmId(String realmId) {
                                        userSessionModel.setRealm(session.realms().getRealm(realmId));
                                    }

                                    @Override
                                    public String getId() {
                                        return userSessionModel.getId();
                                    }

                                    @Override
                                    public void setId(String id) {
                                        throw new IllegalStateException("not supported");
                                    }

                                    @Override
                                    public String getUser() {
                                        return userSessionModel.getUser().getId();
                                    }

                                    @Override
                                    public void setUser(String userId) {
                                        userSessionModel.setUser(session.users().getUserById(realm, userId));
                                    }

                                    @Override
                                    public String getLoginUsername() {
                                        return userSessionModel.getLoginUsername();
                                    }

                                    @Override
                                    public void setLoginUsername(String loginUsername) {
                                        // TODO: ignored. Will not be stored in the offline session
                                    }

                                    @Override
                                    public String getIpAddress() {
                                        return userSessionModel.getIpAddress();
                                    }

                                    @Override
                                    public void setIpAddress(String ipAddress) {
                                        userSessionModel.setIpAddress(ipAddress);
                                    }

                                    @Override
                                    public String getAuthMethod() {
                                        return userSessionModel.getAuthMethod();
                                    }

                                    @Override
                                    public void setAuthMethod(String authMethod) {
                                        userSessionModel.setAuthMethod(authMethod);
                                    }

                                    @Override
                                    public boolean isRememberMe() {
                                        return userSessionModel.isRememberMe();
                                    }

                                    @Override
                                    public void setRememberMe(boolean rememberMe) {
                                        userSessionModel.setRememberMe(rememberMe);
                                    }

                                    @Override
                                    public int getStarted() {
                                        return userSessionModel.getStarted();
                                    }

                                    @Override
                                    public void setStarted(int started) {
                                        userSessionModel.setStarted(started);
                                    }

                                    @Override
                                    public int getLastSessionRefresh() {
                                        return userSessionModel.getLastSessionRefresh();
                                    }

                                    @Override
                                    public void setNotes(Map<String, String> notes) {
                                        userSessionModel.getNotes().keySet().forEach(userSessionModel::removeNote);
                                        notes.forEach((k, v) -> userSessionModel.setNote(k, v));
                                    }

                                    @Override
                                    public void setAuthenticatedClientSessions(AuthenticatedClientSessionStore authenticatedClientSessions) {
                                        throw new IllegalStateException("not supported");
                                    }

                                    @Override
                                    public UserSessionModel.State getState() {
                                        return userSessionModel.getState();
                                    }

                                    @Override
                                    public String getBrokerSessionId() {
                                        return userSessionModel.getBrokerSessionId();
                                    }

                                    @Override
                                    public void setBrokerSessionId(String brokerSessionId) {
                                        userSessionModel.setBrokerSessionId(brokerSessionId);
                                    }

                                    @Override
                                    public String getBrokerUserId() {
                                        return userSessionModel.getBrokerUserId();
                                    }

                                    @Override
                                    public void setBrokerUserId(String brokerUserId) {
                                        userSessionModel.setBrokerUserId(brokerUserId);
                                    }

                                    @Override
                                    public SessionEntityWrapper mergeRemoteEntityWithLocalEntity(SessionEntityWrapper localEntityWrapper) {
                                        throw new IllegalStateException("not supported");
                                    }
                                };
                                sessionUpdates.getUpdateTasks().forEach(vSessionUpdateTask -> {
                                    vSessionUpdateTask.runUpdate((V) userSessionEntity);
                                    if (vSessionUpdateTask.getOperation((V)userSessionEntity) == SessionUpdateTask.CacheOperation.REMOVE) {
                                        session.getProvider(UserSessionPersisterProvider.class).removeUserSession(entry.getKey().toString(), false);
                                    }
                                });
                                userSessionModel.getUpdatedModel();
                            }
                        }
                    } catch (Exception e) {
                        session.getTransactionManager().setRollbackOnly();
                        throw e;
                    }
                }
            }

            if (cacheName.equals("clientSessions")) {
                try (KeycloakSession session = kcSession.getKeycloakSessionFactory().create()) {
                    session.getTransactionManager().begin();
                    try {
                        if (merged.getOperation(sessionWrapper.getEntity()) == SessionUpdateTask.CacheOperation.REMOVE) {
                            AuthenticatedClientSessionEntity entity = (AuthenticatedClientSessionEntity) sessionWrapper.getEntity();
                            session.getProvider(UserSessionPersisterProvider.class).removeClientSession(entity.getUserSessionId(), entity.getClientId(), false);
                        } else if (merged.getOperation(sessionWrapper.getEntity()) == SessionUpdateTask.CacheOperation.ADD || merged.getOperation(sessionWrapper.getEntity()) == SessionUpdateTask.CacheOperation.ADD_IF_ABSENT){
                            AuthenticatedClientSessionEntity entity = (AuthenticatedClientSessionEntity) sessionWrapper.getEntity();
                            session.getProvider(UserSessionPersisterProvider.class).createClientSession(new AuthenticatedClientSessionModel() {
                                @Override
                                public int getStarted() {
                                    return entity.getStarted();
                                }

                                @Override
                                public int getUserSessionStarted() {
                                    return entity.getUserSessionStarted();
                                }

                                @Override
                                public boolean isUserSessionRememberMe() {
                                    return entity.isUserSessionRememberMe();
                                }

                                @Override
                                public String getId() {
                                    return entity.getId().toString();
                                }

                                @Override
                                public int getTimestamp() {
                                    return entity.getTimestamp();
                                }

                                @Override
                                public void setTimestamp(int timestamp) {
                                    throw new IllegalStateException("not implemented");
                                }

                                @Override
                                public void detachFromUserSession() {
                                    throw new IllegalStateException("not implemented");
                                }

                                @Override
                                public UserSessionModel getUserSession() {
                                    return new UserSessionModelDelegate(null) {
                                        @Override
                                        public String getId() {
                                            return entity.getUserSessionId();
                                        }
                                    };
                                }

                                @Override
                                public String getCurrentRefreshToken() {
                                    return entity.getCurrentRefreshToken();
                                }

                                @Override
                                public void setCurrentRefreshToken(String currentRefreshToken) {
                                    throw new IllegalStateException("not implemented");
                                }

                                @Override
                                public int getCurrentRefreshTokenUseCount() {
                                    return entity.getCurrentRefreshTokenUseCount();
                                }

                                @Override
                                public void setCurrentRefreshTokenUseCount(int currentRefreshTokenUseCount) {
                                    throw new IllegalStateException("not implemented");
                                }

                                @Override
                                public String getNote(String name) {
                                    return entity.getNotes().get(name);
                                }

                                @Override
                                public void setNote(String name, String value) {
                                    throw new IllegalStateException("not implemented");
                                }

                                @Override
                                public void removeNote(String name) {
                                    throw new IllegalStateException("not implemented");
                                }

                                @Override
                                public Map<String, String> getNotes() {
                                    return entity.getNotes();
                                }

                                @Override
                                public String getRedirectUri() {
                                    return entity.getRedirectUri();
                                }

                                @Override
                                public void setRedirectUri(String uri) {
                                    throw new IllegalStateException("not implemented");
                                }

                                @Override
                                public RealmModel getRealm() {
                                    return session.realms().getRealm(entity.getRealmId());
                                }

                                @Override
                                public ClientModel getClient() {
                                    return new ClientModelLazyDelegate(() -> null) {
                                        @Override
                                        public String getId() {
                                            return entity.getClientId();
                                        }
                                    };
                                }

                                @Override
                                public String getAction() {
                                    return entity.getAction();
                                }

                                @Override
                                public void setAction(String action) {
                                    throw new IllegalStateException("not implemented");
                                }

                                @Override
                                public String getProtocol() {
                                    return entity.getAuthMethod();
                                }

                                @Override
                                public void setProtocol(String method) {
                                    throw new IllegalStateException("not implemented");
                                }
                            }, false);
                        } else {
                            AuthenticatedClientSessionEntity entity = (AuthenticatedClientSessionEntity) sessionWrapper.getEntity();
                            ClientModel client = session.clients().getClientById(realm, entity.getClientId());
                            PersistentUserSessionAdapter userSession = (PersistentUserSessionAdapter) session.getProvider(UserSessionPersisterProvider.class).loadUserSession(realm, entity.getUserSessionId(), false);
                            if (userSession != null) {
                                PersistentAuthenticatedClientSessionAdapter clientSessionModel = (PersistentAuthenticatedClientSessionAdapter) session.getProvider(UserSessionPersisterProvider.class).loadClientSession(realm, client, userSession, false);
                                if (clientSessionModel != null) {
                                    AuthenticatedClientSessionEntity authenticatedClientSessionEntity = new AuthenticatedClientSessionEntity(entity.getId()) {
                                        @Override
                                        public Map<String, String> getNotes() {
                                            return new HashMap<>() {
                                                @Override
                                                public String get(Object key) {
                                                    return clientSessionModel.getNotes().get(key);
                                                }

                                                @Override
                                                public String put(String key, String value) {
                                                    String oldValue = clientSessionModel.getNotes().get(key);
                                                    clientSessionModel.setNote(key, value);
                                                    return oldValue;
                                                }
                                            };
                                        }

                                        @Override
                                        public void setRedirectUri(String redirectUri) {
                                            clientSessionModel.setRedirectUri(redirectUri);
                                        }

                                        @Override
                                        public void setTimestamp(int timestamp) {
                                            clientSessionModel.setTimestamp(timestamp);
                                        }

                                        @Override
                                        public void setCurrentRefreshToken(String currentRefreshToken) {
                                            clientSessionModel.setCurrentRefreshToken(currentRefreshToken);
                                        }

                                        @Override
                                        public void setCurrentRefreshTokenUseCount(int currentRefreshTokenUseCount) {
                                            clientSessionModel.setCurrentRefreshTokenUseCount(currentRefreshTokenUseCount);
                                        }

                                        @Override
                                        public void setAction(String action) {
                                            clientSessionModel.setAction(action);
                                        }

                                        @Override
                                        public void setAuthMethod(String authMethod) {
                                            clientSessionModel.setProtocol(authMethod);
                                        }

                                        @Override
                                        public String getAuthMethod() {
                                            throw new IllegalStateException("not implemented");
                                        }

                                        @Override
                                        public String getRedirectUri() {
                                            return clientSessionModel.getRedirectUri();
                                        }

                                        @Override
                                        public int getTimestamp() {
                                            return clientSessionModel.getTimestamp();
                                        }

                                        @Override
                                        public int getUserSessionStarted() {
                                            return clientSessionModel.getUserSessionStarted();
                                        }

                                        @Override
                                        public int getStarted() {
                                            return clientSessionModel.getStarted();
                                        }

                                        @Override
                                        public boolean isUserSessionRememberMe() {
                                            return clientSessionModel.isUserSessionRememberMe();
                                        }

                                        @Override
                                        public String getClientId() {
                                            return clientSessionModel.getClient().getClientId();
                                        }

                                        @Override
                                        public void setClientId(String clientId) {
                                            throw new IllegalStateException("not implemented");
                                        }

                                        @Override
                                        public String getAction() {
                                            return clientSessionModel.getAction();
                                        }

                                        @Override
                                        public void setNotes(Map<String, String> notes) {
                                            clientSessionModel.getNotes().keySet().forEach(clientSessionModel::removeNote);
                                            notes.forEach((k, v) -> clientSessionModel.setNote(k, v));
                                        }

                                        @Override
                                        public String getCurrentRefreshToken() {
                                            return clientSessionModel.getCurrentRefreshToken();
                                        }

                                        @Override
                                        public int getCurrentRefreshTokenUseCount() {
                                            return clientSessionModel.getCurrentRefreshTokenUseCount();
                                        }

                                        @Override
                                        public UUID getId() {
                                            return UUID.fromString(clientSessionModel.getId());
                                        }

                                        @Override
                                        public SessionEntityWrapper mergeRemoteEntityWithLocalEntity(SessionEntityWrapper localEntityWrapper) {
                                            throw new IllegalStateException("not implemented");
                                        }

                                        @Override
                                        public String getUserSessionId() {
                                            return clientSessionModel.getUserSession().getId();
                                        }

                                        @Override
                                        public void setUserSessionId(String userSessionId) {
                                            throw new IllegalStateException("not implemented");
                                        }
                                    };
                                    sessionUpdates.getUpdateTasks().forEach(vSessionUpdateTask -> {
                                        vSessionUpdateTask.runUpdate((V) authenticatedClientSessionEntity);
                                        if (vSessionUpdateTask.getOperation((V) authenticatedClientSessionEntity) == SessionUpdateTask.CacheOperation.REMOVE) {
                                            session.getProvider(UserSessionPersisterProvider.class).removeClientSession(entity.getUserSessionId(), entity.getClientId(), false);
                                        }
                                    });
                                    clientSessionModel.getUpdatedModel();
                                }
                            } else {
                                session.getProvider(UserSessionPersisterProvider.class).removeClientSession(entity.getUserSessionId(), entity.getClientId(), false);
                            }
                        }
                    } catch (Exception e) {
                        session.getTransactionManager().setRollbackOnly();
                        throw e;
                    }
                }
            }
        }
    }


    private void runOperationInCluster(K key, MergedUpdate<V> task,  SessionEntityWrapper<V> sessionWrapper) {
        V session = sessionWrapper.getEntity();
        SessionUpdateTask.CacheOperation operation = task.getOperation(session);

        // Don't need to run update of underlying entity. Local updates were already run
        //task.runUpdate(session);

        switch (operation) {
            case REMOVE:
                // Just remove it
                CacheDecorators.skipCacheStoreIfRemoteCacheIsEnabled(cache)
                        .withFlags(Flag.IGNORE_RETURN_VALUES)
                        .remove(key);
                break;
            case ADD:
                CacheDecorators.skipCacheStoreIfRemoteCacheIsEnabled(cache)
                        .withFlags(Flag.IGNORE_RETURN_VALUES)
                        .put(key, sessionWrapper, task.getLifespanMs(), TimeUnit.MILLISECONDS, task.getMaxIdleTimeMs(), TimeUnit.MILLISECONDS);

                logger.tracef("Added entity '%s' to the cache '%s' . Lifespan: %d ms, MaxIdle: %d ms", key, cache.getName(), task.getLifespanMs(), task.getMaxIdleTimeMs());
                break;
            case ADD_IF_ABSENT:
                SessionEntityWrapper<V> existing = CacheDecorators.skipCacheStoreIfRemoteCacheIsEnabled(cache).putIfAbsent(key, sessionWrapper, task.getLifespanMs(), TimeUnit.MILLISECONDS, task.getMaxIdleTimeMs(), TimeUnit.MILLISECONDS);
                if (existing != null) {
                    logger.debugf("Existing entity in cache for key: %s . Will update it", key);

                    // Apply updates on the existing entity and replace it
                    task.runUpdate(existing.getEntity());

                    replace(key, task, existing, task.getLifespanMs(), task.getMaxIdleTimeMs());
                } else {
                    logger.tracef("Add_if_absent successfully called for entity '%s' to the cache '%s' . Lifespan: %d ms, MaxIdle: %d ms", key, cache.getName(), task.getLifespanMs(), task.getMaxIdleTimeMs());
                }
                break;
            case REPLACE:
                replace(key, task, sessionWrapper, task.getLifespanMs(), task.getMaxIdleTimeMs());
                break;
            default:
                throw new IllegalStateException("Unsupported state " +  operation);
        }

    }


    private void replace(K key, MergedUpdate<V> task, SessionEntityWrapper<V> oldVersionEntity, long lifespanMs, long maxIdleTimeMs) {
        boolean replaced = false;
        int iteration = 0;
        V session = oldVersionEntity.getEntity();

        while (!replaced && iteration < InfinispanUtil.MAXIMUM_REPLACE_RETRIES) {
            iteration++;

            SessionEntityWrapper<V> newVersionEntity = generateNewVersionAndWrapEntity(session, oldVersionEntity.getLocalMetadata());

            // Atomic cluster-aware replace
            replaced = CacheDecorators.skipCacheStoreIfRemoteCacheIsEnabled(cache).replace(key, oldVersionEntity, newVersionEntity, lifespanMs, TimeUnit.MILLISECONDS, maxIdleTimeMs, TimeUnit.MILLISECONDS);

            // Replace fail. Need to load latest entity from cache, apply updates again and try to replace in cache again
            if (!replaced) {
                if (logger.isDebugEnabled()) {
                    logger.debugf("Replace failed for entity: %s, old version %s, new version %s. Will try again", key, oldVersionEntity.getVersion(), newVersionEntity.getVersion());
                }

                oldVersionEntity = cache.get(key);

                if (oldVersionEntity == null) {
                    logger.debugf("Entity %s not found. Maybe removed in the meantime. Replace task will be ignored", key);
                    return;
                }

                session = oldVersionEntity.getEntity();

                task.runUpdate(session);
            } else {
                if (logger.isTraceEnabled()) {
                    logger.tracef("Replace SUCCESS for entity: %s . old version: %s, new version: %s, Lifespan: %d ms, MaxIdle: %d ms", key, oldVersionEntity.getVersion(), newVersionEntity.getVersion(), task.getLifespanMs(), task.getMaxIdleTimeMs());
                }
            }
        }

        if (!replaced) {
            logger.warnf("Failed to replace entity '%s' in cache '%s'", key, cache.getName());
        }

    }


    @Override
    protected void rollbackImpl() {
    }

    private SessionEntityWrapper<V> generateNewVersionAndWrapEntity(V entity, Map<String, String> localMetadata) {
        return new SessionEntityWrapper<>(localMetadata, entity);
    }

}
