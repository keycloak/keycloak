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
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;


/**
 * A {@link ExpirationTask} for development or single instance without clustering.
 */
class LocalExpirationTask extends BaseExpirationTask implements Predicate<RealmModel> {

    LocalExpirationTask(KeycloakSessionFactory factory, ScheduledExecutorService scheduledExecutorService, int intervalSeconds, Consumer<Duration> onTaskExecuted) {
        super(factory, scheduledExecutorService, intervalSeconds, onTaskExecuted);
    }

    @Override
    final Predicate<RealmModel> realmFilter() {
        return this;
    }

    @Override
    public final boolean test(RealmModel realmModel) {
        return true;
    }
}
