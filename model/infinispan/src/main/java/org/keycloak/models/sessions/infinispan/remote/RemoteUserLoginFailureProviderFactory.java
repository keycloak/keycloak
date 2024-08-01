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
package org.keycloak.models.sessions.infinispan.remote;

import java.lang.invoke.MethodHandles;

import org.infinispan.client.hotrod.MetadataValue;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserLoginFailureProvider;
import org.keycloak.models.UserLoginFailureProviderFactory;
import org.keycloak.models.UserModel;
import org.keycloak.models.sessions.infinispan.changes.remote.remover.query.ByRealmIdQueryConditionalRemover;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.UpdaterFactory;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.loginfailures.LoginFailuresUpdater;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureKey;
import org.keycloak.models.sessions.infinispan.remote.transaction.LoginFailureChangeLogTransaction;
import org.keycloak.models.sessions.infinispan.remote.transaction.RemoteCacheAndExecutor;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME;

public class RemoteUserLoginFailureProviderFactory implements UserLoginFailureProviderFactory<RemoteUserLoginFailureProvider>, UpdaterFactory<LoginFailureKey, LoginFailureEntity, LoginFailuresUpdater>, EnvironmentDependentProviderFactory {

    private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());
    private static final String PROTO_ENTITY = Marshalling.protoEntity(LoginFailureEntity.class);

    private volatile RemoteCacheAndExecutor<LoginFailureKey, LoginFailureEntity> cacheHolder;

    @Override
    public RemoteUserLoginFailureProvider create(KeycloakSession session) {
        return new RemoteUserLoginFailureProvider(createAndEnlistTransaction(session));
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(final KeycloakSessionFactory factory) {
        cacheHolder = RemoteCacheAndExecutor.create(factory, LOGIN_FAILURE_CACHE_NAME);
        factory.register(event -> {
            if (event instanceof UserModel.UserRemovedEvent userRemovedEvent) {
                UserLoginFailureProvider provider = userRemovedEvent.getKeycloakSession().getProvider(UserLoginFailureProvider.class, getId());
                provider.removeUserLoginFailure(userRemovedEvent.getRealm(), userRemovedEvent.getUser().getId());
            }
        });
        log.debugf("Post Init. Cache=%s", cacheHolder.cache().getName());
    }

    @Override
    public void close() {
        cacheHolder = null;
    }

    @Override
    public String getId() {
        return InfinispanUtils.REMOTE_PROVIDER_ID;
    }

    @Override
    public int order() {
        return InfinispanUtils.PROVIDER_ORDER;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return InfinispanUtils.isRemoteInfinispan();
    }

    @Override
    public LoginFailuresUpdater create(LoginFailureKey key, LoginFailureEntity entity) {
        return LoginFailuresUpdater.create(key, entity);
    }

    @Override
    public LoginFailuresUpdater wrapFromCache(LoginFailureKey key, MetadataValue<LoginFailureEntity> entity) {
        assert entity != null;
        return LoginFailuresUpdater.wrap(key, entity);
    }

    @Override
    public LoginFailuresUpdater deleted(LoginFailureKey key) {
        return LoginFailuresUpdater.delete(key);
    }

    private LoginFailureChangeLogTransaction createAndEnlistTransaction(KeycloakSession session) {
        var tx = new LoginFailureChangeLogTransaction(this, cacheHolder.cache(), new ByRealmIdQueryConditionalRemover<>(PROTO_ENTITY, cacheHolder.executor()));
        session.getTransactionManager().enlistAfterCompletion(tx);
        return tx;
    }
}
