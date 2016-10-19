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
package org.keycloak.testsuite.federation.storage;

import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.user.ImportSynchronization;
import org.keycloak.storage.user.SynchronizationResult;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserPropertyFileStorageFactory implements UserStorageProviderFactory<UserPropertyFileStorage>, ImportSynchronization {


    public static final String PROVIDER_ID = "user-password-props";

    @Override
    public UserPropertyFileStorage create(KeycloakSession session, ComponentModel model) {
        Properties props = new Properties();
        try {
            props.load(getClass().getResourceAsStream(model.getConfig().getFirst("propertyFile")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new UserPropertyFileStorage(session, model, props);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    static List<ProviderConfigProperty> OPTIONS = new LinkedList<>();
    static {
        ProviderConfigProperty prop = new ProviderConfigProperty("propertyFile", "Property File", "file that contains name value pairs", ProviderConfigProperty.STRING_TYPE, null);
        OPTIONS.add(prop);
        prop = new ProviderConfigProperty("federatedStorage", "User Federated Storage", "use federated storage?", ProviderConfigProperty.BOOLEAN_TYPE, null);
        OPTIONS.add(prop);

    }
    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
         return OPTIONS;
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
    public SynchronizationResult sync(KeycloakSessionFactory sessionFactory, String realmId, UserStorageProviderModel model) {
        return SynchronizationResult.ignored();
    }

    @Override
    public SynchronizationResult syncSince(Date lastSync, KeycloakSessionFactory sessionFactory, String realmId, UserStorageProviderModel model) {
        return SynchronizationResult.ignored();
    }
}
