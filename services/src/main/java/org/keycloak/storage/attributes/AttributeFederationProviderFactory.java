/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.storage.attributes;

import jakarta.ws.rs.ProcessingException;
import org.jboss.logging.Logger;
import org.keycloak.common.VerificationException;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.user.ImportSynchronization;
import org.keycloak.storage.user.SynchronizationResult;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Ben Cresitello-Dittmar
 *
 * Factory for creating instances of {@link AttributeFederationProvider}
 */
public class AttributeFederationProviderFactory implements UserStorageProviderFactory<AttributeFederationProvider>, ImportSynchronization {
    private static final Logger logger = Logger.getLogger(AttributeFederationProvider.class);

    String MAPPER_METADATA_ATTRIBUTE = "mapperType";

    @Override
    public AttributeFederationProvider create(KeycloakSession session, ComponentModel model) {
        return new AttributeFederationProvider(this, session, model);
    }

    @Override
    public String getId() {
        return "attributes";
    }

    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property().name(AttributeFederationProviderConfig.CONFIG_ATTRIBUTE_STORE_PROVIDER)
                .type(ProviderConfigProperty.PROVIDER_INSTANCE_TYPE)
                .options(Collections.singletonList("type/" + AttributeStoreProvider.class.getName()))
                .label("Attribute Store Provider")
                .helpText("The external attribute store to sync attributes from")
                .required(true)
                .add()
                .property().name(AttributeFederationProviderConfig.CONFIG_GROUPS)
                .type(ProviderConfigProperty.GROUP_TYPE)
                .label("Groups")
                .options("multi")
                .helpText("This provider will only sync attributes for users belonging to the selected groups")
                .add()
                .build();
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
        try {
            session.getContext().setRealm(realm);
            AttributeFederationProviderConfig.parse(session, config);
        } catch (VerificationException e){
            throw new ComponentValidationException(e.getMessage());
        }
    }

    @Override
    public SynchronizationResult sync(KeycloakSessionFactory sessionFactory, String realmId, UserStorageProviderModel model) {
        logger.debugf("syncing attributes from %s", model.getConfig());
        return syncImpl(sessionFactory, realmId, model);
    }

    @Override
    public SynchronizationResult syncSince(Date lastSync, KeycloakSessionFactory sessionFactory, String realmId, UserStorageProviderModel model) {
        logger.debugf("syncing attributes from %s sync last sync %s", model.getConfig(), lastSync);
        return syncImpl(sessionFactory, realmId, model);
    }

    /**
     * Implementation of attribute federation sync functionality. Syncs attributes from the configured attribute store for users
     * that belong to the configured groups.
     * @param sessionFactory The keycloak session factory
     * @param realmId The ID of the realm the sync action is taking place in
     * @param model The component of the {@link AttributeFederationProvider} instance to use for syncing attributes
     * @return The synchronization result
     */
    public SynchronizationResult syncImpl(KeycloakSessionFactory sessionFactory, String realmId, UserStorageProviderModel model){
        SynchronizationResult result = new SynchronizationResult();

        KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {
            @Override
            public void run(KeycloakSession session) {
                // set the realm in the context so it can be used by the provider
                RealmModel realm = session.realms().getRealm(realmId);
                session.getContext().setRealm(realm);

                // fetch the provider instance
                AttributeFederationProvider provider = (AttributeFederationProvider) session.getProvider(UserStorageProvider.class, model);
                if (provider == null){
                    throw new RuntimeException(String.format("failed to find component provider %s", model.getId()));
                }

                // sync attributes for users that are members of each configured group
                provider.getConfig().getGroups().forEach(g -> session.users().getGroupMembersStream(realm, g).forEach(user -> {
                    logger.debugf("syncing attributes for user %s", user.getUsername());
                    try {
                        provider.syncAttributes(user);
                        result.increaseUpdated();
                    } catch (ProcessingException e){
                        logger.warnf("failed to sync attributes for user %s: %s", user.getUsername(), e);
                        result.increaseFailed();
                    }
                }));
            }
        });

        return result;
    }

    @Override
    public Map<String, Object> getTypeMetadata() {
        Map<String, Object> metadata = UserStorageProviderFactory.super.getTypeMetadata();
        metadata.put(MAPPER_METADATA_ATTRIBUTE, AttributeMapper.class.getName());

        return metadata;
    }
}
