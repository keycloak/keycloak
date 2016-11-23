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

import org.jboss.logging.Logger;
import org.keycloak.Config;
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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class DummyUserFederationProviderFactory implements UserStorageProviderFactory<DummyUserFederationProvider>, ImportSynchronization {

    private static final Logger logger = Logger.getLogger(DummyUserFederationProviderFactory.class);
    public static final String PROVIDER_NAME = "dummy";

    private AtomicInteger fullSyncCounter = new AtomicInteger();
    private AtomicInteger changedSyncCounter = new AtomicInteger();

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
        fullSyncCounter.incrementAndGet();
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

    public int getChangedSyncCounter() {
        return changedSyncCounter.get();
    }

}
