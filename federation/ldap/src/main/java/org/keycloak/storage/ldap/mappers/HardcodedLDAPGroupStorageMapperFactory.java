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

import java.util.ArrayList;
import java.util.List;

import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.storage.ldap.LDAPStorageProvider;

/**
 * @author <a href="mailto:jean-loup.maillet@yesitis.fr">Jean-Loup Maillet</a>
 */
public class HardcodedLDAPGroupStorageMapperFactory extends AbstractLDAPStorageMapperFactory {

    public static final String PROVIDER_ID = "hardcoded-ldap-group-mapper";
    protected static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty groupAttr = createConfigProperty(HardcodedLDAPGroupStorageMapper.GROUP,
                "Group",
                "Group to add the user in. Fill the full path of the group including path. For example '/root-group/child-group'",
                ProviderConfigProperty.STRING_TYPE,
                null,
                true);

        configProperties.add(groupAttr);
    }

    @Override
    public String getHelpText() {
        return "When user is imported from LDAP, he will be automatically added into this configured group.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
        String groupName = config.getConfig().getFirst(HardcodedLDAPGroupStorageMapper.GROUP);
        if (groupName == null) {
            throw new ComponentValidationException("Group can't be null");
        }
        GroupModel group = KeycloakModelUtils.findGroupByPath(session, realm, groupName);
        if (group == null) {
            throw new ComponentValidationException("There is no group corresponding to configured value");
        }
    }

    @Override
    protected AbstractLDAPStorageMapper createMapper(ComponentModel mapperModel, LDAPStorageProvider federationProvider) {
        return new HardcodedLDAPGroupStorageMapper(mapperModel, federationProvider);
    }
}
