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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.Config;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
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
import org.keycloak.models.sessions.infinispan.remote.transaction.RemoteChangeLogTransaction;
import org.keycloak.models.sessions.infinispan.transaction.InfinispanTransactionProvider;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.util.concurrent.BlockingManager;
import org.jboss.logging.Logger;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME;

public class RemoteUserLoginFailureProviderFactory implements UserLoginFailureProviderFactory<RemoteUserLoginFailureProvider>, UpdaterFactory<LoginFailureKey, LoginFailureEntity, LoginFailuresUpdater>, EnvironmentDependentProviderFactory, RemoteChangeLogTransaction.SharedState<LoginFailureKey, LoginFailureEntity>, ServerInfoAwareProviderFactory {

    private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());
    public static final String PROTO_ENTITY = Marshalling.protoEntity(LoginFailureEntity.class);

    private volatile RemoteCache<LoginFailureKey, LoginFailureEntity> cache;
    private volatile BlockingManager blockingManager;
    private volatile int maxRetries = InfinispanUtils.DEFAULT_MAX_RETRIES;
    private volatile int backOffBaseTimeMillis = InfinispanUtils.DEFAULT_RETRIES_BASE_TIME_MILLIS;

    @Override
    public RemoteUserLoginFailureProvider create(KeycloakSession session) {
        return new RemoteUserLoginFailureProvider(createAndEnlistTransaction(session));
    }

    @Override
    public void init(Config.Scope config) {
        maxRetries = InfinispanUtils.getMaxRetries(config);
        backOffBaseTimeMillis = InfinispanUtils.getRetryBaseTimeMillis(config);
    }

    @Override
    public void postInit(final KeycloakSessionFactory factory) {
        try (var session = factory.create()) {
            var provider = session.getProvider(InfinispanConnectionProvider.class);
            cache = provider.getRemoteCache(LOGIN_FAILURE_CACHE_NAME);
            blockingManager = provider.getBlockingManager();
        }
        factory.register(event -> {
            if (event instanceof UserModel.UserRemovedEvent userRemovedEvent) {
                UserLoginFailureProvider provider = userRemovedEvent.getKeycloakSession().getProvider(UserLoginFailureProvider.class, getId());
                provider.removeUserLoginFailure(userRemovedEvent.getRealm(), userRemovedEvent.getUser().getId());
            }
        });
        log.debugf("Post Init. Cache=%s", cache.getName());
    }

    @Override
    public void close() {
        cache = null;
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
    public List<ProviderConfigProperty> getConfigMetadata() {
        ProviderConfigurationBuilder builder = ProviderConfigurationBuilder.create();
        InfinispanUtils.configureMaxRetries(builder);
        InfinispanUtils.configureRetryBaseTime(builder);
        return builder.build();
    }

    @Override
    public Map<String, String> getOperationalInfo() {
        Map<String, String> map = new HashMap<>();
        InfinispanUtils.maxRetriesToOperationalInfo(map, maxRetries);
        InfinispanUtils.retryBaseTimeMillisToOperationalInfo(map, backOffBaseTimeMillis);
        return map;
    }

    @Override
    public Set<Class<? extends Provider>> dependsOn() {
        return Set.of(InfinispanTransactionProvider.class);
    }

    @Override
    public LoginFailuresUpdater create(LoginFailureKey key, LoginFailureEntity entity) {
        return LoginFailuresUpdater.create(key, entity);
    }

    @Override
    public LoginFailuresUpdater wrapFromCache(LoginFailureKey key, LoginFailureEntity value, long version) {
        return LoginFailuresUpdater.wrap(key, value, version);
    }

    @Override
    public LoginFailuresUpdater deleted(LoginFailureKey key) {
        return LoginFailuresUpdater.delete(key);
    }

    @Override
    public RemoteCache<LoginFailureKey, LoginFailureEntity> cache() {
        return cache;
    }

    @Override
    public int maxRetries() {
        return maxRetries;
    }

    @Override
    public int backOffBaseTimeMillis() {
        return backOffBaseTimeMillis;
    }

    @Override
    public BlockingManager blockingManager() {
        return blockingManager;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = Math.max(0, maxRetries);
    }

    private LoginFailureChangeLogTransaction createAndEnlistTransaction(KeycloakSession session) {
        var provider = session.getProvider(InfinispanTransactionProvider.class);
        var tx = new LoginFailureChangeLogTransaction(this, this, new ByRealmIdQueryConditionalRemover<>(PROTO_ENTITY));
        provider.registerTransaction(tx);
        return tx;
    }
}
