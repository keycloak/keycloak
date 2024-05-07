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
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.SynchronizationResult;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Ben Cresitello-Dittmar
 *
 * A provider used to periodically fetch attributes from a configured external attribute store and update them in the internal
 * user database.
 */
public class AttributeFederationProvider implements UserStorageProvider {
    private static final Logger logger = Logger.getLogger(AttributeFederationProvider.class);

    private final ComponentModel component;
    private final KeycloakSession session;
    private final AttributeFederationProviderConfig config;

    public AttributeFederationProvider(AttributeFederationProviderFactory factory, KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.component = model;
        try {
            this.config = AttributeFederationProviderConfig.parse(session, model);
        } catch (VerificationException e){
            throw new RuntimeException("failed to parse component configuration: %s", e);
        }
    }

    /**
     * Get the parsed provider configuration
     * @return The parsed provider configuration
     */
    public AttributeFederationProviderConfig getConfig(){
        return config;
    }

    /**
     * Sync attribute for the specified user
     * @param user The user to sync attributes for
     */
    public void syncAttributes(UserModel user) throws ProcessingException {
        RealmModel realm = session.getContext().getRealm();

        Map<String, Object> attributes = config.getProvider().getAttributes(session, realm, user);
        logger.debugf("pre-mapper attributes for user %s: %s", user.getUsername(), attributes);

        Map<String, String> processedAttributes = new HashMap<>();
        realm.getComponentsStream(component.getId(), AttributeMapper.class.getName())
                .forEach(mapperModel -> {
                    AttributeMapper attrMapper = session.getComponentProvider(AttributeMapper.class, mapperModel.getId());
                    attrMapper.transform(session, attributes, processedAttributes);
                });
        logger.debugf("post-mapper attributes for user %s: %s", user.getUsername(), processedAttributes);

        processedAttributes.forEach(user::setSingleAttribute);
        logger.debugf("updated attributes for user %s: %s", user.getUsername(), user.getAttributes());
    }

    @Override
    public void close() {}
}
