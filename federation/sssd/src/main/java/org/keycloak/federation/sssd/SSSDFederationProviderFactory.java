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

package org.keycloak.federation.sssd;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.federation.sssd.api.Sssd;
import org.keycloak.federation.sssd.impl.PAMAuthenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.storage.UserStorageProviderModel;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>
 * @version $Revision: 1 $
 */
public class SSSDFederationProviderFactory implements UserStorageProviderFactory<SSSDFederationProvider>, EnvironmentDependentProviderFactory {

    private static final String PROVIDER_NAME = "sssd";
    private static final Logger logger = Logger.getLogger(SSSDFederationProvider.class);


    @Override
    public String getId() {
        return PROVIDER_NAME;
    }

    @Override
    public SSSDFederationProvider create(KeycloakSession session, ComponentModel model) {
        return new SSSDFederationProvider(session, new UserStorageProviderModel(model), this);
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

    protected PAMAuthenticator createPAMAuthenticator(String username, String... factors) {
        return new PAMAuthenticator(username, factors);
    }

    @Override
    public boolean isSupported() {
        return Sssd.isAvailable();
    }
}