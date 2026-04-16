/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.events.hooks;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;
import org.keycloak.timer.TimerProvider;

public class EventHookEventListenerProviderFactory implements EventListenerProviderFactory {

    public static final String ID = "event-hooks";

    private long pollIntervalMillis = TimeUnit.SECONDS.toMillis(30);
    private long claimTimeoutMillis = TimeUnit.MINUTES.toMillis(5);
    private int maxPollBatchSize = 50;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new EventHookEventListenerProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
        pollIntervalMillis = config.getLong("pollIntervalMillis", pollIntervalMillis);
        claimTimeoutMillis = config.getLong("claimTimeoutMillis", claimTimeoutMillis);
        maxPollBatchSize = config.getInt("maxPollBatchSize", maxPollBatchSize);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        try (KeycloakSession session = factory.create()) {
            TimerProvider timer = session.getProvider(TimerProvider.class);
            if (timer != null) {
                timer.schedule(new ClusterAwareScheduledTaskRunner(factory, new EventHookDeliveryTask(factory, maxPollBatchSize, claimTimeoutMillis), pollIntervalMillis), pollIntervalMillis, pollIntervalMillis);
            }
        }
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean isGlobal() {
        return true;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name("pollIntervalMillis")
            .label("eventHookListenerPollIntervalMillis")
            .helpText("eventHookListenerPollIntervalMillisHelp")
                .type(ProviderConfigProperty.INTEGER_TYPE)
                .defaultValue((int) pollIntervalMillis)
                .add()
                .property()
                .name("claimTimeoutMillis")
            .label("eventHookListenerClaimTimeoutMillis")
            .helpText("eventHookListenerClaimTimeoutMillisHelp")
                .type(ProviderConfigProperty.INTEGER_TYPE)
                .defaultValue((int) claimTimeoutMillis)
                .add()
                .property()
                .name("maxPollBatchSize")
            .label("eventHookListenerMaxPollBatchSize")
            .helpText("eventHookListenerMaxPollBatchSizeHelp")
                .type(ProviderConfigProperty.INTEGER_TYPE)
                .defaultValue(maxPollBatchSize)
                .add()
                .build();
    }

}
