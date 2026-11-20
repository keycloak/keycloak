/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.sessions.infinispan;


import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.executors.ExecutorsProvider;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.models.SingleUseObjectProviderFactory;
import org.keycloak.models.session.RevokedTokenPersisterProvider;
import org.keycloak.models.sessions.infinispan.entities.SingleUseObjectValueEntity;
import org.keycloak.models.sessions.infinispan.transaction.InfinispanTransactionProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import org.infinispan.Cache;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.TopologyChanged;
import org.infinispan.notifications.cachelistener.event.TopologyChangedEvent;
import org.infinispan.notifications.cachemanagerlistener.annotation.Merged;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;
import org.infinispan.partitionhandling.AvailabilityMode;
import org.infinispan.remoting.transport.Address;
import org.jboss.logging.Logger;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.ACTION_TOKEN_CACHE;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanSingleUseObjectProviderFactory implements SingleUseObjectProviderFactory<InfinispanSingleUseObjectProvider>, EnvironmentDependentProviderFactory, ServerInfoAwareProviderFactory {

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());
    private static final String RELOAD_EXECUTOR_TASK = "revoked-token-reload";

    public static final String CONFIG_PERSIST_REVOKED_TOKENS = "persistRevokedTokens";
    public static final boolean DEFAULT_PERSIST_REVOKED_TOKENS = true;
    public static final String LOADED = "loaded" + SingleUseObjectProvider.REVOKED_KEY;

    protected Cache<String, SingleUseObjectValueEntity> singleUseObjectCache;

    private volatile boolean initialized;
    private boolean persistRevokedTokens;
    private final ViewChangeListener viewChangeListener = new ViewChangeListener();
    private final TopologyListener topologyListener = new TopologyListener();
    // Guards reloadPending. It is only held briefly and never during the database scan, so notification threads don't block.
    // An explicit lock instead of synchronized, as notification threads may be virtual threads that synchronized would pin.
    private final ReentrantLock stateLock = new ReentrantLock();
    private boolean reloadPending;
    private Executor reloadExecutor;
    private KeycloakSessionFactory sessionFactory;
    private volatile boolean closed;

    @Override
    public Set<Class<? extends Provider>> dependsOn() {
        return Set.of(InfinispanConnectionProvider.class, InfinispanTransactionProvider.class, ExecutorsProvider.class);
    }

    @Override
    public InfinispanSingleUseObjectProvider create(KeycloakSession session) {
        initialize(session);
        return new InfinispanSingleUseObjectProvider(session, singleUseObjectCache, persistRevokedTokens, createTransaction(session));
    }

    @Override
    public void init(Config.Scope config) {
        persistRevokedTokens = config.getBoolean(CONFIG_PERSIST_REVOKED_TOKENS, DEFAULT_PERSIST_REVOKED_TOKENS);
    }

    private void initialize(KeycloakSession session) {
        if (persistRevokedTokens && !initialized) {
            synchronized (this) {
                if (!initialized) {
                    RevokedTokenPersisterProvider provider = session.getProvider(RevokedTokenPersisterProvider.class);
                    if (singleUseObjectCache.get(LOADED) == null) {
                        // LOADED is cluster-wide, so nodes joining an initialized cluster avoid repeating the database scan.
                        // Multiple nodes can still race during the first startup, but importing the same entries is harmless.
                        loadRevokedTokens(provider);
                        singleUseObjectCache.put(LOADED, new SingleUseObjectValueEntity(Collections.emptyMap()));
                    }
                    initialized = true;
                }
            }
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // It is necessary to put the cache initialization here, otherwise the cache would be initialized lazily, that
        // means also listeners will start only after first cache initialization - that would be too late
        try (var session = factory.create()) {
            if (singleUseObjectCache == null) {
                InfinispanConnectionProvider connections = session.getProvider(InfinispanConnectionProvider.class);
                singleUseObjectCache = connections.getCache(ACTION_TOKEN_CACHE);
            }
            if (persistRevokedTokens) {
                reloadExecutor = session.getProvider(ExecutorsProvider.class).getExecutor(RELOAD_EXECUTOR_TASK);
            }
        }

        if (persistRevokedTokens) {
            sessionFactory = factory;
            // Revocation entries can be lost with departed owners while LOADED survives. Reconcile on membership loss
            // instead of adding a database lookup to every cache miss on the token-validation path.
            singleUseObjectCache.getCacheManager().addListener(viewChangeListener);
            singleUseObjectCache.addListener(topologyListener);
            factory.register(event -> {
                if (event instanceof PostMigrationEvent pme) {
                    KeycloakSessionFactory sessionFactory = pme.getFactory();
                    try (KeycloakSession session = sessionFactory.create()) {
                        // Load persisted revocations during startup, not on the first token check, to avoid request-time congestion.
                        initialize(session);
                    }
                }
            });
        }
    }

    @Override
    public void close() {
        closed = true;
        // The cache is not assigned when initialization failed before postInit(), and the cleanup must not mask that failure.
        if (persistRevokedTokens && singleUseObjectCache != null) {
            singleUseObjectCache.getCacheManager().removeListener(viewChangeListener);
            singleUseObjectCache.removeListener(topologyListener);
        }
    }

    @Override
    public String getId() {
        return InfinispanUtils.EMBEDDED_PROVIDER_ID;
    }

    @Override
    public int order() {
        return InfinispanUtils.PROVIDER_ORDER;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return !Profile.isFeatureEnabled(Profile.Feature.STATELESS) && InfinispanUtils.isEmbeddedInfinispan();
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

    private static InfinispanKeycloakTransaction createTransaction(KeycloakSession session) {
        InfinispanTransactionProvider transactionProvider = session.getProvider(InfinispanTransactionProvider.class);
        InfinispanKeycloakTransaction tx = new InfinispanKeycloakTransaction();
        transactionProvider.registerTransaction(tx);
        return tx;
    }

    private void loadRevokedTokens(RevokedTokenPersisterProvider provider) {
        provider.getAllRevokedTokens().forEach(revokedToken -> {
            // The scan can run for a long time, so the time is taken per record to skip tokens that expired meanwhile.
            long lifespanSeconds = revokedToken.expiry() - Time.currentTimeSeconds();
            if (lifespanSeconds > 0) {
                String key = revokedToken.tokenId() + SingleUseObjectProvider.REVOKED_KEY;
                SingleUseObjectValueEntity value = new SingleUseObjectValueEntity(Collections.emptyMap());
                // Loading only fills gaps. Replacing an entry written by another node would unnecessarily restart its
                // cache lifespan, both during concurrent startup and membership-change recovery.
                singleUseObjectCache.putIfAbsent(key, value, lifespanSeconds, TimeUnit.SECONDS);
            }
        });
    }

    private void markReloadPending() {
        stateLock.lock();
        try {
            reloadPending = true;
        } finally {
            stateLock.unlock();
        }
    }

    private boolean isReloadPending() {
        stateLock.lock();
        try {
            return reloadPending;
        } finally {
            stateLock.unlock();
        }
    }

    private void requestReload() {
        // Topology changes are frequent, so only submit a task when a membership change is waiting for a reload.
        if (closed || !isReloadPending()) {
            return;
        }
        // Keycloak's executor provider owns this shared pool. Offloading avoids blocking the Infinispan notification
        // thread without retaining a thread owned by this provider between the rare cluster membership changes.
        try {
            reloadExecutor.execute(this::reloadIfPendingAndSettled);
        } catch (RejectedExecutionException e) {
            // The default executor has an unbounded queue, so a rejection means the executor is shutting down.
            // There is nothing left to retry, but clearing the marker lets the next node starting up reload.
            if (!closed) {
                logger.warn("Failed to submit the reload of revoked tokens after a cluster membership change", e);
            }
            clearLoadedMarker();
        }
    }

    private void clearLoadedMarker() {
        try {
            // Without the marker, the next node starting up loads the persisted revocations again. This also applies
            // to every later node until a reload succeeds, which is intended: the cache may be incomplete, and
            // claiming otherwise would hide the missing entries. A successful scan sets the marker again.
            singleUseObjectCache.remove(LOADED);
        } catch (RuntimeException removeFailure) {
            if (!closed) {
                logger.warn("Failed to clear the revoked tokens loaded marker", removeFailure);
            }
        }
    }

    /**
     * The view change is delivered before Infinispan has recovered the cache topology. Entries written in that window
     * can be discarded when the partitions are reconciled, so the reload is deferred to a topology change that
     * leaves the cache settled.
     */
    private synchronized void reloadIfPendingAndSettled() {
        // Synchronized like the scan, so a task queued behind a running scan checks the topology again once it runs.
        if (closed) {
            return;
        }
        // Checking and clearing the flag together keeps a membership change that arrives meanwhile from being lost.
        stateLock.lock();
        try {
            if (!reloadPending || !isTopologySettled()) {
                return;
            }
            reloadPending = false;
        } finally {
            stateLock.unlock();
        }
        reloadRevokedTokens();
    }

    private boolean isTopologySettled() {
        var cache = singleUseObjectCache.getAdvancedCache();
        DistributionManager dm = cache.getDistributionManager();
        if (dm == null) {
            return cache.getAvailability() == AvailabilityMode.AVAILABLE;
        }
        // During a merge, Infinispan can publish an intermediate topology without a rebalance that still lacks the
        // members of the other partition. Requiring the topology to match the cluster view excludes it.
        List<Address> viewMembers = cache.getCacheManager().getMembers();
        boolean membersRecovered = viewMembers == null
                || new HashSet<>(dm.getCacheTopology().getMembers()).equals(new HashSet<>(viewMembers));
        return cache.getAvailability() == AvailabilityMode.AVAILABLE
                && dm.isJoinComplete() && !dm.isRehashInProgress() && membersRecovered;
    }

    private synchronized void reloadRevokedTokens() {
        if (closed) {
            return;
        }

        // Every member observes the view change. Restrict the database scan to one node; writes to the distributed
        // cache restore the entries for the whole cluster.
        if (!singleUseObjectCache.getCacheManager().isCoordinator()) {
            return;
        }

        try {
            logger.debug("Reloading revoked tokens after a cluster membership change");
            KeycloakModelUtils.runJobInTransaction(sessionFactory, session ->
                    loadRevokedTokens(session.getProvider(RevokedTokenPersisterProvider.class)));
        } catch (RuntimeException e) {
            if (!closed) {
                logger.warn("Failed to reload revoked tokens after a cluster membership change", e);
                clearLoadedMarker();
            }
        }
    }

    @Listener
    public class ViewChangeListener {

        // On a merge, Infinispan notifies only @Merged listeners and not @ViewChanged, so both are needed.
        @Merged
        @ViewChanged
        public void viewChanged(ViewChangedEvent event) {
            // Joining members receive state from existing owners and must not make every scale-up scan the database.
            // After a merge, neither partition can assume its surviving entries are complete.
            if (event.isMergeView() || !event.getNewMembers().containsAll(event.getOldMembers())) {
                markReloadPending();
            }
        }
    }

    @Listener
    public class TopologyListener {

        // Runs for every topology change of the cache, but only acts when a membership change flagged a reload.
        @TopologyChanged
        public void topologyChanged(TopologyChangedEvent<?, ?> event) {
            if (!event.isPre()) {
                requestReload();
            }
        }
    }

}
