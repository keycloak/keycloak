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

package org.keycloak.models.sessions.infinispan.changes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletionStage;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelIllegalStateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.delegate.ClientModelLazyDelegate;
import org.keycloak.models.session.PersistentAuthenticatedClientSessionAdapter;
import org.keycloak.models.session.PersistentUserSessionAdapter;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.utils.RealmModelDelegate;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.models.utils.UserSessionModelDelegate;

import org.infinispan.commons.util.concurrent.AggregateCompletionStage;
import org.infinispan.util.function.TriConsumer;
import org.jboss.logging.Logger;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.USER_SESSION_CACHE_NAME;

public class JpaChangesPerformer<K, V extends SessionEntity> {
    private static final Logger LOG = Logger.getLogger(JpaChangesPerformer.class);

    private final List<PersistentUpdate> changes;
    private final TriConsumer<KeycloakSession, Map.Entry<K, SessionUpdatesList<V>>, MergedUpdate<V>> processor;
    private final ArrayBlockingQueue<PersistentUpdate> batchingQueue;
    private boolean warningShown = false;

    public JpaChangesPerformer(String cacheName, ArrayBlockingQueue<PersistentUpdate> batchingQueue) {
        // The changes list is only used when batching is disabled.
        this.changes = batchingQueue == null ? new ArrayList<>(2) : List.of();
        this.batchingQueue = batchingQueue;
        processor = processor(cacheName);

    }

    /**
     * Checks if this instance support non-blocking writes.
     * <p>
     * If this instance is non-blocking, the invoker must use
     * {@link #asyncWrite(AggregateCompletionStage, Map.Entry, MergedUpdate)}.
     * <p>
     * Otherwise, the implementation must support {@link #registerChange(Map.Entry, MergedUpdate)} and
     * {@link #write(KeycloakSession)}. The invoker should register the change using the first method and applied them
     * in a blocking way using the later method.
     *
     * @return {@code true} if this instance is non-blocking.
     * @see #asyncWrite(AggregateCompletionStage, Map.Entry, MergedUpdate)
     * @see #registerChange(Map.Entry, MergedUpdate)
     * @see #write(KeycloakSession)
     */
    public boolean isNonBlocking() {
        return batchingQueue != null;
    }

    /**
     * Performs a non-blocking write into the database.
     * <p>
     * The implementation should register the {@link CompletionStage} into the {@link AggregateCompletionStage}.
     *
     * @param stage  The {@link AggregateCompletionStage} to collect the {@link CompletionStage}.
     * @param entry  The {@link Map.Entry} with the ID and the session.
     * @param merged The {@link MergedUpdate} to be applied to the existing session.
     * @throws NullPointerException if this instance does not support non-blocking writes.
     * @see #isNonBlocking()
     */
    public void asyncWrite(AggregateCompletionStage<Void> stage, Map.Entry<K, SessionUpdatesList<V>> entry, MergedUpdate<V> merged) {
        var update = newUpdate(entry, merged);
        offer(update);
        stage.dependsOn(update.future());
    }

    /**
     * It queues a database write to be applied at a future invocation.
     *
     * @param entry  The {@link Map.Entry} with the ID and the session.
     * @param merged The {@link MergedUpdate} to be applied to the existing session.
     * @throws UnsupportedOperationException if this instance does not support blocking writes.
     * @see #isNonBlocking()
     */
    public void registerChange(Map.Entry<K, SessionUpdatesList<V>> entry, MergedUpdate<V> merged) {
        changes.add(newUpdate(entry, merged));
    }

    /**
     * Applies all the pending write operation into the database.
     *
     * @param session The {@link KeycloakSession} to access the database.
     */
    public void write(KeycloakSession session) {
        changes.forEach(persistentUpdate -> persistentUpdate.perform(session));
    }

    /**
     * Clears any pending blocking changes.
     *
     * @throws UnsupportedOperationException if this instance does not support blocking writes.
     */
    public void clear() {
        changes.clear();
    }

    private PersistentUpdate newUpdate(Map.Entry<K, SessionUpdatesList<V>> entry, MergedUpdate<V> merged) {
        return new PersistentUpdate(innerSession -> processor.accept(innerSession, entry, merged));
    }

    private TriConsumer<KeycloakSession, Map.Entry<K, SessionUpdatesList<V>>, MergedUpdate<V>> processor(String cacheName) {
        return switch (cacheName) {
            case USER_SESSION_CACHE_NAME, OFFLINE_USER_SESSION_CACHE_NAME ->
                    JpaChangesPerformer::processUserSessionUpdate;
            case CLIENT_SESSION_CACHE_NAME, OFFLINE_CLIENT_SESSION_CACHE_NAME ->
                    JpaChangesPerformer::processClientSessionUpdate;
            default -> throw new IllegalStateException("Unexpected value: " + cacheName);
        };
    }

    private void offer(PersistentUpdate update) {
        if (!batchingQueue.offer(update)) {
            if (!warningShown) {
                warningShown = true;
                LOG.warn("Queue is full, will block");
            }
            try {
                // this will block until there is a free spot in the queue
                batchingQueue.put(update);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }

    private static <K, V extends SessionEntity> void processClientSessionUpdate(KeycloakSession session, Map.Entry<K, SessionUpdatesList<V>> entry, MergedUpdate<V> merged) {
        SessionUpdatesList<V> sessionUpdates = entry.getValue();
        SessionEntityWrapper<V> sessionWrapper = sessionUpdates.getEntityWrapper();
        RealmModel realm = sessionUpdates.getRealm();
        session.getContext().setRealm(realm);
        UserSessionPersisterProvider userSessionPersister = session.getProvider(UserSessionPersisterProvider.class);

        switch (merged.getOperation()) {
            case REMOVE -> {
                AuthenticatedClientSessionEntity entity = (AuthenticatedClientSessionEntity) sessionWrapper.getEntity();
                userSessionPersister.removeClientSession(entity.getUserSessionId(), entity.getClientId(), entity.isOffline());
            }
            case ADD, ADD_IF_ABSENT -> createClientSession(session, sessionWrapper, userSessionPersister);
            case REPLACE -> mergeClientSession(sessionWrapper, userSessionPersister, realm, sessionUpdates);
        }

    }

    private static <K, V extends SessionEntity> void mergeClientSession(SessionEntityWrapper<V> sessionWrapper, UserSessionPersisterProvider userSessionPersister, RealmModel realm, SessionUpdatesList<V> sessionUpdates) {
        AuthenticatedClientSessionEntity entity = (AuthenticatedClientSessionEntity) sessionWrapper.getEntity();
        ClientModel client = new ClientModelLazyDelegate(null) {
            @Override
            public String getId() {
                return Objects.requireNonNull(entity.getClientId());
            }
        };
        UserSessionModel userSession = new UserSessionModelDelegate(null) {
            @Override
            public String getId() {
                return Objects.requireNonNull(entity.getUserSessionId());
            }
        };
        PersistentAuthenticatedClientSessionAdapter clientSessionModel = (PersistentAuthenticatedClientSessionAdapter) userSessionPersister.loadClientSession(realm, client, userSession, entity.isOffline());
        if (clientSessionModel != null) {
            AuthenticatedClientSessionEntity authenticatedClientSessionEntity = new AuthenticatedClientSessionEntity() {
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
                    notes.forEach(clientSessionModel::setNote);
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
                if (vSessionUpdateTask.getOperation() == SessionUpdateTask.CacheOperation.REMOVE) {
                    userSessionPersister.removeClientSession(entity.getUserSessionId(), entity.getClientId(), entity.isOffline());
                }
            });
            clientSessionModel.getUpdatedModel();
        } else {
            LOG.debugf("No client session found for %s/%s", entity.getUserSessionId(), entity.getClientId());
        }
    }

    private static <V extends SessionEntity> void createClientSession(KeycloakSession session, SessionEntityWrapper<V> sessionWrapper, UserSessionPersisterProvider userSessionPersister) {
        AuthenticatedClientSessionEntity entity = (AuthenticatedClientSessionEntity) sessionWrapper.getEntity();
        userSessionPersister.createClientSession(new AuthenticatedClientSessionModel() {
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
                throw new UnsupportedOperationException();
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
        }, entity.isOffline());
    }

    private static <K, V extends SessionEntity> void processUserSessionUpdate(KeycloakSession session, Map.Entry<K, SessionUpdatesList<V>> entry, MergedUpdate<V> merged) {
        SessionUpdatesList<V> sessionUpdates = entry.getValue();
        SessionEntityWrapper<V> sessionWrapper = sessionUpdates.getEntityWrapper();
        RealmModel realm = sessionUpdates.getRealm();
        session.getContext().setRealm(realm);
        UserSessionPersisterProvider userSessionPersister = session.getProvider(UserSessionPersisterProvider.class);
        UserSessionEntity entity = (UserSessionEntity) sessionWrapper.getEntity();

        switch (merged.getOperation()) {
            case REMOVE -> userSessionPersister.removeUserSession(entry.getKey().toString(), entity.isOffline());
            case ADD -> throw new ModelIllegalStateException("Operation ADD is not implemented to overwrite an existing user session");
            case ADD_IF_ABSENT -> {
                PersistentUserSessionAdapter userSessionModel = (PersistentUserSessionAdapter) userSessionPersister.loadUserSession(realm, entry.getKey().toString(), entity.isOffline());
                if (userSessionModel != null) {
                    // This might happen if the user logs in via multiple tabs at the same time from an external broker, and the same authentication session creates
                    // multiple user sessions concurrently.
                    if (!Objects.equals(userSessionModel.getUserId(), entity.getUser())) {
                        // This should never happen, and if it does, it shows a bug somewhere else where a wrong ID was used.
                        // Still, this check would help us to identify if such a problem exists, so this is why we keep it here.
                        throw new ModelIllegalStateException("User ID of the session does not match, the user ID should not change");
                    }
                    if (Math.abs(userSessionModel.getStarted() - entity.getStarted()) > 10) {
                        // The only valid situation where a session is created with an already existing ID is that there are concurrent requests.
                        // For example, an authentication flow is triggered in two different tabs of a browser, and processed concurrently.
                        // In such a case, it is valid and this code will then update the first one created.
                        // In all other cases, if such a request for an authentication session would come in later, this should be handled in other places.
                        // Due to this, this is limited to the first 10 seconds of a user session to handle only the current login case.
                        throw new ModelIllegalStateException("Session has already aged, concurrent requests to create it should not happen");
                    }
                    mergeUserSession(session, entry, userSessionModel, realm, sessionUpdates, userSessionPersister, entity);
                } else {
                    createUserSession(userSessionPersister, entity);
                }
            }
            case REPLACE -> {
                PersistentUserSessionAdapter userSessionModel = (PersistentUserSessionAdapter) userSessionPersister.loadUserSession(realm, entry.getKey().toString(), entity.isOffline());
                if (userSessionModel != null) {
                    mergeUserSession(session, entry, userSessionModel, realm, sessionUpdates, userSessionPersister, entity);
                } else {
                    LOG.debugf("No user session found for %s", entry.getKey());
                }
            }
        }
    }

    private static void createUserSession(UserSessionPersisterProvider userSessionPersister, UserSessionEntity entity) {
        userSessionPersister.createUserSession(new UserSessionModel() {
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
                return entity.isOffline();
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
        }, entity.isOffline());
    }

    private static <K, V extends SessionEntity> void mergeUserSession(KeycloakSession innerSession, Map.Entry<K, SessionUpdatesList<V>> entry, PersistentUserSessionAdapter userSessionModel, RealmModel realm, SessionUpdatesList<V> sessionUpdates, UserSessionPersisterProvider userSessionPersister, UserSessionEntity entity) {
        UserSessionEntity userSessionEntity = new UserSessionEntity(userSessionModel.getId()) {
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
                        userSessionModel.getNotes().clear();
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
            public String getRealmId() {
                return userSessionModel.getRealm().getId();
            }

            @Override
            public void setRealmId(String realmId) {
                userSessionModel.setRealm(innerSession.realms().getRealm(realmId));
            }

            @Override
            public String getUser() {
                return userSessionModel.getUser().getId();
            }

            @Override
            public void setUser(String userId) {
                userSessionModel.setUser(innerSession.users().getUserById(realm, userId));
            }

            @Override
            public String getLoginUsername() {
                return userSessionModel.getLoginUsername();
            }

            @Override
            public void setLoginUsername(String loginUsername) {
                userSessionModel.setLoginUsername(loginUsername);
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
                notes.forEach(userSessionModel::setNote);
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
            if (vSessionUpdateTask.getOperation() == SessionUpdateTask.CacheOperation.REMOVE) {
                userSessionPersister.removeUserSession(entry.getKey().toString(), entity.isOffline());
            }
        });
        userSessionModel.getUpdatedModel();
    }
}
