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
package org.keycloak.testsuite.federation;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.user.ImportSynchronization;
import org.keycloak.storage.user.SynchronizationResult;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class FailableHardcodedStorageProviderFactory implements UserStorageProviderFactory<FailableHardcodedStorageProvider>, ImportSynchronization {

    public static final String PROVIDER_ID = "failable-hardcoded-storage";

    @Override
    public FailableHardcodedStorageProvider create(KeycloakSession session, ComponentModel model) {
        return new FailableHardcodedStorageProvider(model, session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    static List<ProviderConfigProperty> OPTIONS = new LinkedList<>();
    static {
        ProviderConfigProperty prop = new ProviderConfigProperty("fail", "fail", "If on, provider will throw exception", ProviderConfigProperty.BOOLEAN_TYPE, "false");
        OPTIONS.add(prop);
    }
    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return OPTIONS;
    }

    @Override
    public SynchronizationResult sync(KeycloakSessionFactory sessionFactory, String realmId, UserStorageProviderModel model) {
        if (FailableHardcodedStorageProvider.isInFailMode(model)) FailableHardcodedStorageProvider.throwFailure();
        return SynchronizationResult.empty();
    }

    @Override
    public SynchronizationResult syncSince(Date lastSync, KeycloakSessionFactory sessionFactory, String realmId, UserStorageProviderModel model) {
        if (FailableHardcodedStorageProvider.isInFailMode(model)) FailableHardcodedStorageProvider.throwFailure();
        return SynchronizationResult.empty();
    }
}
