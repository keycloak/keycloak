/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.lock;

import org.keycloak.common.util.Retry;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionTaskWithResult;
import org.keycloak.models.locking.GlobalLockProvider;
import org.keycloak.models.locking.LockAcquiringTimeoutException;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.models.map.storage.QueryParameters;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

import static org.keycloak.models.map.storage.criteria.DefaultModelCriteria.criteria;


/**
 * Implementing a {@link GlobalLockProvider} based on a map storage.
 * This requires the map store to support the entity type {@link MapLockEntity}. One of the stores which supports
 * this is the JPA Map Store. The store needs to support the uniqueness of entries in the lock area, see
 * {@link #lock(String)} for details.
 *
 * @author Alexander Schwartz
 */
public class MapGlobalLockProvider implements GlobalLockProvider {

    private final KeycloakSession session;
    private final long defaultTimeoutMilliseconds;
    private MapStorage<MapLockEntity, MapLockEntity> store;

    /**
     * The lockStoreSupplier allows the store to be initialized lazily and only when needed: As this provider is initialized
     * for both the outer and the inner transactions, and the store is needed only for the inner transactions.
     */
    private final Supplier<MapStorage<MapLockEntity, MapLockEntity>> lockStoreSupplier;

    public MapGlobalLockProvider(KeycloakSession session, long defaultTimeoutMilliseconds, Supplier<MapStorage<MapLockEntity, MapLockEntity>> lockStoreSupplier) {
        this.defaultTimeoutMilliseconds = defaultTimeoutMilliseconds;
        this.session = session;
        this.lockStoreSupplier = lockStoreSupplier;
    }

    @Override
    public <V> V withLock(String lockName, Duration timeToWaitForLock, KeycloakSessionTaskWithResult<V> task) throws LockAcquiringTimeoutException {
        MapLockEntity[] lockEntity = {null};
        try {
            if (timeToWaitForLock == null) {
                // Set default timeout if null provided
                timeToWaitForLock = Duration.ofMillis(defaultTimeoutMilliseconds);
            }
            String[] keycloakInstanceIdentifier = {null};
            Instant[] timeWhenAcquired = {null};
            try {
                Retry.executeWithBackoff(i -> lockEntity[0] = KeycloakModelUtils.runJobInTransactionWithResult(this.session.getKeycloakSessionFactory(),
                        innerSession -> {
                            MapGlobalLockProvider provider = (MapGlobalLockProvider) innerSession.getProvider(GlobalLockProvider.class);
                            // even if the call to provider.lock() succeeds, due to concurrency one can only be sure after a commit that all DB constraints have been met
                            return provider.lock(lockName);
                        }), (iteration, t) -> {
                    if (t instanceof LockAcquiringTimeoutException) {
                        LockAcquiringTimeoutException ex = (LockAcquiringTimeoutException) t;
                        keycloakInstanceIdentifier[0] = ex.getKeycloakInstanceIdentifier();
                        timeWhenAcquired[0] = ex.getTimeWhenAcquired();
                    }
                }, timeToWaitForLock, 500);
            } catch (RuntimeException ex) {
                if (!(ex instanceof LockAcquiringTimeoutException)) {
                    throw new LockAcquiringTimeoutException(lockName, keycloakInstanceIdentifier[0], timeWhenAcquired[0], ex);
                }
                throw ex;
            }
            return KeycloakModelUtils.runJobInTransactionWithResult(this.session.getKeycloakSessionFactory(), task);
        } finally {
            if (lockEntity[0] != null) {
                KeycloakModelUtils.runJobInTransaction(this.session.getKeycloakSessionFactory(), innerSession -> {
                    MapGlobalLockProvider provider = (MapGlobalLockProvider) innerSession.getProvider(GlobalLockProvider.class);
                    provider.unlock(lockEntity[0]);
                });
            }
        }
    }

    @Override
    public void forceReleaseAllLocks() {
        KeycloakModelUtils.runJobInTransaction(this.session.getKeycloakSessionFactory(), innerSession -> {
            MapGlobalLockProvider provider = (MapGlobalLockProvider) innerSession.getProvider(GlobalLockProvider.class);
            provider.releaseAllLocks();
        });
    }

    @Override
    public void close() {
    }

    private void prepareTx() {
        if (store == null) {
            this.store = lockStoreSupplier.get();
        }
    }

    /**
     * Create a {@link MapLockEntity} for the provided <code>lockName</code>.
     * The underlying store must ensure that a lock with the given name can be created only once in the store.
     * This constraint needs to be checked either at the time of creation, or at the latest when the transaction
     * is committed. If such a constraint violation is detected at the time of the transaction commit, it should
     * throw an exception and the transaction should roll back.
     * <p/>
     * The JPA Map Store implements this with a unique index, which is checked by the database both at the time of
     * insertion and at the time the transaction is committed.
     */
    private MapLockEntity lock(String lockName) {
        prepareTx();
        DefaultModelCriteria<MapLockEntity> mcb = criteria();
        mcb = mcb.compare(MapLockEntity.SearchableFields.NAME, ModelCriteriaBuilder.Operator.EQ, lockName);
        Optional<MapLockEntity> entry = store.read(QueryParameters.withCriteria(mcb)).findFirst();

        if (entry.isEmpty()) {
            MapLockEntity entity = DeepCloner.DUMB_CLONER.newInstance(MapLockEntity.class);
            entity.setName(lockName);
            entity.setKeycloakInstanceIdentifier(getKeycloakInstanceIdentifier());
            entity.setTimeAcquired(Time.currentTimeMillis());
            return store.create(entity);
        } else {
            throw new LockAcquiringTimeoutException(lockName, entry.get().getKeycloakInstanceIdentifier(), Instant.ofEpochMilli(entry.get().getTimeAcquired()));
        }
    }

    /**
     * Unlock the previously created lock.
     * Will fail if the lock doesn't exist, or has a different owner.
     */
    private void unlock(MapLockEntity lockEntity) {
        prepareTx();
        MapLockEntity readLockEntity = store.read(lockEntity.getId());

        if (readLockEntity == null) {
            throw new RuntimeException("didn't find lock - someone else unlocked it?");
        } else if (!lockEntity.isLockUnchanged(readLockEntity)) {
            // this case is there for stores which might re-use IDs or derive it from the name of the entity (like the file store map store does in some cases).
            throw new RuntimeException(String.format("Lock owned by different instance: Lock [%s] acquired by keycloak instance [%s] at the time [%s]",
                    readLockEntity.getName(), readLockEntity.getKeycloakInstanceIdentifier(), readLockEntity.getTimeAcquired()));
        } else {
            store.delete(readLockEntity.getId());
        }
    }

    private void releaseAllLocks() {
        prepareTx();
        DefaultModelCriteria<MapLockEntity> mcb = criteria();
        store.delete(QueryParameters.withCriteria(mcb));
    }

    private static String getKeycloakInstanceIdentifier() {
        long pid = ProcessHandle.current().pid();
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = "unknown-host";
        }

        String threadName = Thread.currentThread().getName();
        return threadName + "#" + pid + "@" + hostname;
    }

}
