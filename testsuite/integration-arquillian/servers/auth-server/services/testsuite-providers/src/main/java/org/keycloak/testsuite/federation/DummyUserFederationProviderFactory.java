/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.federation;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.keycloak.Config;
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.user.ImportSynchronization;
import org.keycloak.storage.user.SynchronizationResult;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class DummyUserFederationProviderFactory implements UserStorageProviderFactory<DummyUserFederationProvider>, ImportSynchronization {

    private static final Logger logger = Logger.getLogger(DummyUserFederationProviderFactory.class);
    public static final String PROVIDER_NAME = "dummy";

    // Test-only configuration: when set on the component model, sync() sleeps for this many
    // milliseconds to simulate a slow sync run (used to test the periodic sync gate)
    public static final String SYNC_DELAY_MS_CONFIG = "test-sync-delay-ms";

    private final AtomicInteger fullSyncCounter = new AtomicInteger();
    private final AtomicInteger changedSyncCounter = new AtomicInteger();
    private final AtomicInteger lastFullSyncStartEpochSec = new AtomicInteger();
    private final AtomicInteger fullSyncInFlight = new AtomicInteger();

    private Map<String, UserModel> users = new HashMap<String, UserModel>();

    @Override
    public DummyUserFederationProvider create(KeycloakSession session, ComponentModel model) {
        return new DummyUserFederationProvider(session, model, users);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property().name("important.config")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add().build();
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_NAME;
    }

    @Override
    public SynchronizationResult sync(KeycloakSessionFactory sessionFactory, String realmId, UserStorageProviderModel model) {
        logger.info("syncAllUsers invoked");
        lastFullSyncStartEpochSec.set(Time.currentTime());
        fullSyncInFlight.incrementAndGet();
        fullSyncCounter.incrementAndGet();
        sleepIfConfigured(model);
        fullSyncInFlight.decrementAndGet();
        return SynchronizationResult.empty();
    }

    @Override
    public SynchronizationResult syncSince(Date lastSync, KeycloakSessionFactory sessionFactory, String realmId, UserStorageProviderModel model) {
        logger.info("syncChangedUsers invoked");
        changedSyncCounter.incrementAndGet();
        return SynchronizationResult.empty();
    }

    public int getFullSyncCounter() {
        return fullSyncCounter.get();
    }

    public int getLastFullSyncStartEpochSec() {
        return lastFullSyncStartEpochSec.get();
    }

    public boolean isFullSyncInFlight() {
        return fullSyncInFlight.get() > 0;
    }

    // Simulate a slow sync run when SYNC_DELAY_MS_CONFIG is set on the component model
    private static void sleepIfConfigured(UserStorageProviderModel model) {
        String delayMs = model.getConfig().getFirst(SYNC_DELAY_MS_CONFIG);
        if (delayMs == null) {
            return;
        }

        try {
            Thread.sleep(Long.parseLong(delayMs));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int getChangedSyncCounter() {
        return changedSyncCounter.get();
    }

}
