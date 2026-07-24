/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.sessions.infinispan.expiration;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;

import org.infinispan.Cache;
import org.infinispan.distribution.DistributionManager;

/**
 * A {@link ExpirationTask} implementation that uses the {@link DistributionManager} from an Infinispan {@link Cache}.
 * <p>
 * It takes advantage of the {@link DistributionManager} to assign Keycloak instances to distinct {@link RealmModel},
 * allowing a distributed check of expired session through the cluster.
 * <p>
 * A consistent hash is not updated at the same time in all Keycloak cluster members. It is possible some realms are not
 * checked during an iteration, or the same realms are checked by multiple Keycloak instances. In the latter case, we rely on
 * database locking.
 */
class DistributionAwareExpirationTask extends BaseExpirationTask implements Predicate<RealmModel> {

    private final DistributionManager distributionManager;

    DistributionAwareExpirationTask(KeycloakSessionFactory factory, ScheduledExecutorService scheduledExecutorService, int intervalSeconds, Consumer<Duration> onTaskExecuted, DistributionManager distributionManager) {
        super(factory, scheduledExecutorService, intervalSeconds, onTaskExecuted);
        this.distributionManager = Objects.requireNonNull(distributionManager);
    }

    @Override
    final Predicate<RealmModel> realmFilter() {
        return this;
    }

    @Override
    public final boolean test(RealmModel realm) {
        return distributionManager.getCacheTopology().getDistribution(realm.getId()).isPrimary();
    }
}
