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
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;

import org.infinispan.client.hotrod.RemoteCache;

/**
 * A {@link ExpirationTask} for non clustered environment, when an external Infinispan is available.
 * <p>
 * During network partitions, it has a probability of two or more Keycloak instances to be assigned to the same realm.
 * In this scenario, we rely on the database lock to keep data consistent.
 * <p>
 * Keycloak instances starting and stopping information may not be available in real time, and it is possible some
 * realms not being checked during an iteration.
 */
class RemoteExpirationTask extends BaseExpirationTask {

    private final ConsistentHash consistentHash;

    RemoteExpirationTask(KeycloakSessionFactory factory, ScheduledExecutorService scheduledExecutorService, int intervalSeconds, Consumer<Duration> onTaskExecuted, RemoteCache<String, String> workCache, String nodeName) {
        super(factory, scheduledExecutorService, intervalSeconds, onTaskExecuted);
        this.consistentHash = ConsistentHash.create(workCache, scheduledExecutorService, UUID.randomUUID().toString(), nodeName, intervalSeconds);
    }

    @Override
    public final void start() {
        consistentHash.start();
        super.start();
    }

    @Override
    public final void stop() {
        super.stop();
        consistentHash.stop();
    }

    @Override
    final Predicate<RealmModel> realmFilter() {
        return consistentHash.consistentHashSnapshot();
    }

    int membersSize() {
        return consistentHash.size();
    }

}
