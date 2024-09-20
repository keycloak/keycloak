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

import org.infinispan.util.function.TriConsumer;
import org.jboss.logging.Logger;
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
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionStore;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.utils.RealmModelDelegate;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.models.utils.UserSessionModelDelegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.USER_SESSION_CACHE_NAME;

public class JpaChangesPerformer<K, V extends SessionEntity> implements SessionChangesPerformer<K, V> {
    private static final Logger LOG = Logger.getLogger(JpaChangesPerformer.class);

    private final String cacheName;
    private final List<PersistentUpdate> changes = new LinkedList<>();
    private final TriConsumer<KeycloakSession, Map.Entry<K, SessionUpdatesList<V>>, MergedUpdate<V>> processor;
    private final ArrayBlockingQueue<PersistentUpdate> batchingQueue;

    public JpaChangesPerformer(String cacheName, ArrayBlockingQueue<PersistentUpdate> batchingQueue) {
        this.cacheName = cacheName;
        this.batchingQueue = batchingQueue;
        processor = processor();
    }

    @Override
    public void registerChange(Map.Entry<K, SessionUpdatesList<V>> entry, MergedUpdate<V> merged) {
        changes.add(new PersistentUpdate(innerSession -> processor.accept(innerSession, entry, merged)));
    }

    private TriConsumer<KeycloakSession, Map.Entry<K, SessionUpdatesList<V>>, MergedUpdate<V>> processor() {
        return switch (cacheName) {
            case USER_SESSION_CACHE_NAME, OFFLINE_USER_SESSION_CACHE_NAME -> this::processUserSessionUpdate;
            case CLIENT_SESSION_CACHE_NAME, OFFLINE_CLIENT_SESSION_CACHE_NAME -> this::processClientSessionUpdate;
            default -> throw new IllegalStateException("Unexpected value: " + cacheName);
        };
    }

    private boolean warningShown = false;

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

    @Override
    public void applyChanges() {
        if (!changes.isEmpty()) {
            changes.forEach(this::offer);
            List<Throwable> exceptions = new ArrayList<>();
            CompletableFuture.allOf(changes.stream().map(f -> f.future().exceptionally(throwable -> {
                exceptions.add(throwable);
                return null;
            })).toArray(CompletableFuture[]::new)).join();
            // If any of those futures has failed, add the exceptions as suppressed exceptions to our runtime exception
            if (!exceptions.isEmpty()) {
                RuntimeException ex = new RuntimeException("unable to complete the session updates");
                exceptions.forEach(ex::addSuppressed);
                throw ex;
            }
            changes.clear();
        }
    }

    public void applyChangesSynchronously(KeycloakSession session) {
        if (!changes.isEmpty()) {
            changes.forEach(persistentUpdate -> persistentUpdate.perform(session));
            changes.clear();
        }
    }

    private void processClientSessionUpdate(KeycloakSession innerSession, Map.Entry<K, SessionUpdatesList<V>> entry, MergedUpdate<V> merged) {
        SessionUpdatesList<V> sessionUpdates = entry.getValue();
        SessionEntityWrapper<V> sessionWrapper = sessionUpdates.getEntityWrapper();
        RealmModel realm = sessionUpdates.getRealm();
        UserSessionPersisterProvider userSessionPersister = innerSession.getProvider(UserSessionPersisterProvider.class);

        if (merged.getOperation() == SessionUpdateTask.CacheOperation.REMOVE) {
            AuthenticatedClientSessionEntity entity = (AuthenticatedClientSessionEntity) sessionWrapper.getEntity();
            userSessionPersister.removeClientSession(entity.getUserSessionId(), entity.getClientId(), entity.isOffline());
        } else if (merged.getOperation() == SessionUpdateTask.CacheOperation.ADD || merged.getOperation() == SessionUpdateTask.CacheOperation.ADD_IF_ABSENT){
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
                    return innerSession.realms().getRealm(entity.getRealmId());
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
        } else {
            AuthenticatedClientSessionEntity entity = (AuthenticatedClientSessionEntity) sessionWrapper.getEntity();
            ClientModel client = new ClientModelLazyDelegate(null) {
                @Override
                public String getId() {
                    return entity.getClientId();
                }
            };
            UserSessionModel userSession = new UserSessionModelDelegate(null) {
                @Override
                public String getId() {
                    return entity.getUserSessionId();
                }
            };
            PersistentAuthenticatedClientSessionAdapter clientSessionModel = (PersistentAuthenticatedClientSessionAdapter) userSessionPersister.loadClientSession(realm, client, userSession, entity.isOffline());
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
                    if (vSessionUpdateTask.getOperation() == SessionUpdateTask.CacheOperation.REMOVE) {
                        userSessionPersister.removeClientSession(entity.getUserSessionId(), entity.getClientId(), entity.isOffline());
                    }
                });
                clientSessionModel.getUpdatedModel();
            }
        }

    }

    private void processUserSessionUpdate(KeycloakSession innerSession, Map.Entry<K, SessionUpdatesList<V>> entry, MergedUpdate<V> merged) {
        SessionUpdatesList<V> sessionUpdates = entry.getValue();
        SessionEntityWrapper<V> sessionWrapper = sessionUpdates.getEntityWrapper();
        RealmModel realm = sessionUpdates.getRealm();
        UserSessionPersisterProvider userSessionPersister = innerSession.getProvider(UserSessionPersisterProvider.class);
        UserSessionEntity entity = (UserSessionEntity) sessionWrapper.getEntity();

        if (merged.getOperation() == SessionUpdateTask.CacheOperation.REMOVE) {
            userSessionPersister.removeUserSession(entry.getKey().toString(), entity.isOffline());
        } else if (merged.getOperation() == SessionUpdateTask.CacheOperation.ADD || merged.getOperation() == SessionUpdateTask.CacheOperation.ADD_IF_ABSENT){
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
        } else {
            PersistentUserSessionAdapter userSessionModel = (PersistentUserSessionAdapter) userSessionPersister.loadUserSession(realm, entry.getKey().toString(), entity.isOffline());
            if (userSessionModel != null) {
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
                    if (vSessionUpdateTask.getOperation() == SessionUpdateTask.CacheOperation.REMOVE) {
                        userSessionPersister.removeUserSession(entry.getKey().toString(), entity.isOffline());
                    }
                });
                userSessionModel.getUpdatedModel();
            }

        }
    }
}
