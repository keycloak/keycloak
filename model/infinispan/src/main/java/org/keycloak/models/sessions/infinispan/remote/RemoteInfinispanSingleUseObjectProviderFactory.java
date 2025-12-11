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
import java.util.concurrent.TimeUnit;

import org.keycloak.Config;
import org.keycloak.common.util.Time;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.SingleUseObjectProviderFactory;
import org.keycloak.models.session.RevokedToken;
import org.keycloak.models.session.RevokedTokenPersisterProvider;
import org.keycloak.models.sessions.infinispan.entities.SingleUseObjectValueEntity;
import org.keycloak.models.sessions.infinispan.remote.transaction.SingleUseObjectTransaction;
import org.keycloak.models.sessions.infinispan.transaction.InfinispanTransactionProvider;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import org.infinispan.client.hotrod.RemoteCache;
import org.jboss.logging.Logger;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.ACTION_TOKEN_CACHE;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.getRemoteCache;
import static org.keycloak.models.SingleUseObjectProvider.REVOKED_KEY;
import static org.keycloak.models.sessions.infinispan.InfinispanSingleUseObjectProviderFactory.CONFIG_PERSIST_REVOKED_TOKENS;
import static org.keycloak.models.sessions.infinispan.InfinispanSingleUseObjectProviderFactory.DEFAULT_PERSIST_REVOKED_TOKENS;
import static org.keycloak.models.sessions.infinispan.InfinispanSingleUseObjectProviderFactory.LOADED;
import static org.keycloak.models.sessions.infinispan.remote.RemoteInfinispanSingleUseObjectProvider.REVOKED_TOKEN_VALUE;
import static org.keycloak.models.sessions.infinispan.remote.RemoteInfinispanSingleUseObjectProvider.RevokeTokenConsumer;
import static org.keycloak.storage.datastore.DefaultDatastoreProviderFactory.setupClearExpiredRevokedTokensScheduledTask;

public class RemoteInfinispanSingleUseObjectProviderFactory implements SingleUseObjectProviderFactory<RemoteInfinispanSingleUseObjectProvider>, EnvironmentDependentProviderFactory, ProviderEventListener, ServerInfoAwareProviderFactory {

    private final static Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());
    private static final RevokeTokenConsumer VOLATILE_REVOKE_TOKEN = (token, lifespanSeconds) -> {
    };
    // max of 16 remote cache puts concurrently.
    private static final int REVOKED_TOKENS_IMPORT_CONCURRENCY = 16;

    private volatile RemoteCache<String, SingleUseObjectValueEntity> cache;
    private volatile boolean persistRevokedTokens;

    @Override
    public RemoteInfinispanSingleUseObjectProvider create(KeycloakSession session) {
        assert cache != null;
        return new RemoteInfinispanSingleUseObjectProvider(createAndEnlistTransaction(session), createRevokeTokenConsumer(session));
    }

    @Override
    public void init(Config.Scope config) {
        persistRevokedTokens = config.getBoolean(CONFIG_PERSIST_REVOKED_TOKENS, DEFAULT_PERSIST_REVOKED_TOKENS);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        cache = getRemoteCache(factory, ACTION_TOKEN_CACHE);
        factory.register(this);
        logger.debug("Provided initialized.");
    }

    @Override
    public void close() {

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
    public Map<String, String> getOperationalInfo() {
        Map<String, String> info = new HashMap<>();
        info.put(CONFIG_PERSIST_REVOKED_TOKENS, Boolean.toString(persistRevokedTokens));
        return info;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        ProviderConfigurationBuilder builder = ProviderConfigurationBuilder.create();
        builder.property()
                .name(CONFIG_PERSIST_REVOKED_TOKENS)
                .type("boolean")
                .helpText("If revoked tokens are stored persistently across restarts")
                .defaultValue(DEFAULT_PERSIST_REVOKED_TOKENS)
                .add();

        return builder.build();
    }

    @Override
    public void onEvent(ProviderEvent event) {
        if (!(event instanceof PostMigrationEvent pme)) {
            return;
        }
        if (!persistRevokedTokens) {
            //nothing to do
            return;
        }

        // preload revoked tokens from the database and register cleanup expired tokens task
        KeycloakSessionFactory sessionFactory = pme.getFactory();
        setupClearExpiredRevokedTokensScheduledTask(sessionFactory);
        try (var session = sessionFactory.create()) {
            preloadRevokedTokens(session);
        }
    }

    @Override
    public Set<Class<? extends Provider>> dependsOn() {
        return Set.of(InfinispanTransactionProvider.class);
    }

    private SingleUseObjectTransaction createAndEnlistTransaction(KeycloakSession session) {
        var provider = session.getProvider(InfinispanTransactionProvider.class);
        var tx = new SingleUseObjectTransaction(cache);
        provider.registerTransaction(tx);
        return tx;
    }

    private RevokedTokenPersisterProvider getRevokedTokenPersisterProvider(KeycloakSession session) {
        return session.getProvider(RevokedTokenPersisterProvider.class);
    }

    private RevokeTokenConsumer createRevokeTokenConsumer(KeycloakSession session) {
        return persistRevokedTokens ? getRevokedTokenPersisterProvider(session)::revokeToken : VOLATILE_REVOKE_TOKEN;
    }

    private void preloadRevokedTokens(KeycloakSession session) {
        var provider = getRevokedTokenPersisterProvider(session);
        if (cache.get(LOADED) == null) {
            logger.debug("Preloading revoked tokens from database.");
            var currentTime = Time.currentTime();
            Flowable.fromStream(provider.getAllRevokedTokens())
                    .filter(revokedToken -> revokedToken.expiry() - currentTime > 0) // skip expired tokens
                    .flatMapCompletable(token -> preloadToken(token, currentTime), false, REVOKED_TOKENS_IMPORT_CONCURRENCY)
                    .blockingAwait();
            cache.put(LOADED, REVOKED_TOKEN_VALUE);
            logger.debug("Preload completed.");
        }
    }

    private Completable preloadToken(RevokedToken token, long currentTime) {
        var lifespan = token.expiry() - currentTime;
        return Completable.fromCompletionStage(cache.putIfAbsentAsync(token.tokenId() + REVOKED_KEY, REVOKED_TOKEN_VALUE, lifespan, TimeUnit.SECONDS));
    }


}
