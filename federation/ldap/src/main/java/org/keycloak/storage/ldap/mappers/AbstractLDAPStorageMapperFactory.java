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

package org.keycloak.storage.ldap.mappers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.storage.ldap.LDAPStorageProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractLDAPStorageMapperFactory implements LDAPStorageMapperFactory<LDAPStorageMapper> {

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public LDAPStorageMapper create(KeycloakSession session, ComponentModel model) {
        // LDAPStorageProvider is in the session already as mappers are always called from it
        String ldapProviderModelId = model.getParentId();
        LDAPStorageProvider ldapProvider = (LDAPStorageProvider) session.getAttribute(ldapProviderModelId);

        return createMapper(model, ldapProvider);
    }

    // Used just by LDAPFederationMapperBridge.
    protected abstract AbstractLDAPStorageMapper createMapper(ComponentModel mapperModel, LDAPStorageProvider federationProvider);

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public Map<String, Object> getTypeMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fedToKeycloakSyncSupported", false);
        metadata.put("keycloakToFedSyncSupported", false);

        return metadata;
    }

    @Override
    public void close() {
    }

    public static ProviderConfigProperty createConfigProperty(String name, String label, String helpText, String type, List<String> options) {
        ProviderConfigProperty configProperty = new ProviderConfigProperty();
        configProperty.setName(name);
        configProperty.setLabel(label);
        configProperty.setHelpText(helpText);
        configProperty.setType(type);
        configProperty.setOptions(options);
        return configProperty;
    }

    public static ProviderConfigProperty createConfigProperty(String name, String label, String helpText, String type, List<String> options, boolean required) {
        ProviderConfigProperty property = createConfigProperty(name, label, helpText, type, options);
        property.setRequired(required);
        return property;
    }

    protected void checkMandatoryConfigAttribute(String name, String displayName, ComponentModel mapperModel) throws ComponentValidationException {
        String attrConfigValue = mapperModel.getConfig().getFirst(name);
        if (attrConfigValue == null || attrConfigValue.trim().isEmpty()) {
            throw new ComponentValidationException("Missing configuration for '" + displayName + "'");
        }
    }


}
