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

import org.jboss.logging.Logger;
import org.keycloak.common.VerificationException;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.ModelToRepresentation;

import java.util.Collections;
import java.util.List;

/**
 * @author Ben Cresitello-Dittmar
 *
 * Configuration model for {@link AttributeFederationProvider} instances
 */
public class AttributeFederationProviderConfig {
    private static final Logger logger = Logger.getLogger(AttributeFederationProviderConfig.class);

    // The component ID of the attribute store provider instance to use for attribute sync
    public static final String CONFIG_ATTRIBUTE_STORE_PROVIDER = "attributeStoreProvider";

    // A list of group paths. Attributes will only be synced for users that are members of these groups
    public static final String CONFIG_GROUPS = "groups";

    private AttributeStoreProvider provider;
    private List<GroupModel> groups;

    /**
     * Get the component ID of the attribute store provider instance to use when syncing attributes
     * @return The component ID of the attribute store provider instance
     */
    public AttributeStoreProvider getProvider() {
        return provider;
    }

    /**
     * Set the component ID of the attribute store provider instance
     * @param provider The component ID of the attribute store provider instance
     */
    public void setProvider(AttributeStoreProvider provider) {
        this.provider = provider;
    }

    /**
     * Get the groups to sync attributes for. Attributes will only be synced for users that are members of these groups
     * @return
     */
    public List<GroupModel> getGroups() {
        return groups;
    }

    /**
     * Set the groups of users that will have their attributes synced with this provider.
     * @param groups The groups
     */
    public void setGroups(List<GroupModel> groups) {
        this.groups = groups;
    }

    /**
     * Helper function to parse the configuration from the provider component model
     * @param session The keycloak session
     * @param model The component model to parse
     * @return The parsed provider configuration
     * @throws VerificationException Thrown if the component model represents an invalid configuration
     */
    public static AttributeFederationProviderConfig parse(KeycloakSession session, ComponentModel model) throws VerificationException{
        AttributeFederationProviderConfig parsed = new AttributeFederationProviderConfig();

        AttributeStoreProvider provider = parseProvider(session, model);
        parsed.setProvider(provider);

        List<GroupModel> groups = parseGroups(session, model);
        parsed.setGroups(groups);

        return parsed;
    }

    /**
     * Helper function to parse the provider from the specified component model
     * @param session The keycloak session
     * @param model The component model to parse
     * @return The initialized attribute store provider instance configured in the model
     * @throws VerificationException Thrown if the configured provider instance cannot be found in the current session / realm
     */
    private static AttributeStoreProvider parseProvider(KeycloakSession session, ComponentModel model) throws VerificationException {
        String providerId = model.getConfig().getFirst(CONFIG_ATTRIBUTE_STORE_PROVIDER);

        ComponentModel providerComp = session.getContext().getRealm().getComponent(providerId);
        if (providerComp == null){
            throw new VerificationException(String.format("failed to find component %s", providerId));
        }
        AttributeStoreProvider provider = session.getProvider(AttributeStoreProvider.class, providerComp);
        if (provider == null){
            throw new VerificationException(String.format("failed to find component provider %s", providerId));
        }

        return provider;
    }

    /**
     * Helper function to parse the groups configured in the component model
     * @param session The keycloak session
     * @param model The component model to parse
     * @return The parsed groups specified in the component model
     * @throws VerificationException Thrown if any of the groups specified in the component model cannot be found in the session / realm
     */
    private static List<GroupModel> parseGroups(KeycloakSession session, ComponentModel model) throws VerificationException{
        List<String> groups = model.getConfig().get(CONFIG_GROUPS);

        if (groups == null){
            return Collections.emptyList();
        }

        List<GroupModel> groupModels = groups.stream().map(groupPath -> {
            GroupModel m = session.groups().getGroupsStream(session.getContext().getRealm()).filter(group -> ModelToRepresentation.buildGroupPath(group).equals(groupPath)).findFirst().orElse(null);
            if (m == null){
                logger.warnf("group %s does not exist", groupPath);
            }
            return m;
        }).toList();

        // throw error if any group was not found
        if (groupModels.contains(null)){
            throw new VerificationException("config contains invalid group");
        }

        return groupModels;
    }
}
