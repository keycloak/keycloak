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
import org.keycloak.federation.sssd.impl.PAMAuthenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderFactory;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserFederationSyncResult;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>
 * @version $Revision: 1 $
 */
public class SSSDFederationProviderFactory implements UserFederationProviderFactory {

    private static final String PROVIDER_NAME = "sssd";
    private static final Logger logger = Logger.getLogger(SSSDFederationProvider.class);

    static final Set<String> configOptions = new HashSet<String>();

    @Override
    public String getId() {
        return PROVIDER_NAME;
    }

    @Override
    public UserFederationProvider getInstance(KeycloakSession session, UserFederationProviderModel model) {
        return new SSSDFederationProvider(session, model, this);
    }

    /**
     * List the configuration options to render and display in the admin console's generic management page for this
     * plugin
     *
     * @return
     */
    @Override
    public Set<String> getConfigurationOptions() {
        return configOptions;
    }

    @Override
    public UserFederationProvider create(KeycloakSession session) {
        return null;
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
    public UserFederationSyncResult syncAllUsers(KeycloakSessionFactory sessionFactory, final String realmId, final UserFederationProviderModel model) {
        logger.info("Sync users not supported for this provider");
        return UserFederationSyncResult.empty();
    }

    @Override
    public UserFederationSyncResult syncChangedUsers(KeycloakSessionFactory sessionFactory, final String realmId, final UserFederationProviderModel model, Date lastSync) {
        logger.info("Sync users not supported for this provider");
        return UserFederationSyncResult.empty();
    }

    protected PAMAuthenticator createPAMAuthenticator(String username, String... factors) {
        return new PAMAuthenticator(username, factors);
    }

}
