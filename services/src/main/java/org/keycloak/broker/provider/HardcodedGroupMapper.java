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

package org.keycloak.broker.provider;

import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.mappers.AbstractClaimToGroupMapper;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:dmartino@redhat.com">Daniele Martinoli</a>
 * @version $Revision: 1 $
 */
public class HardcodedGroupMapper extends AbstractClaimToGroupMapper {
    protected static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    private static final Logger LOG = Logger.getLogger(HardcodedGroupMapper.class);

    private static final Set<IdentityProviderSyncMode> IDENTITY_PROVIDER_SYNC_MODES =
            new HashSet<>(Arrays.asList(IdentityProviderSyncMode.values()));

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(ConfigConstants.GROUP);
        property.setLabel("Group");
        property.setHelpText("Group to assign the user.");
        property.setType(ProviderConfigProperty.GROUP_TYPE);
        configProperties.add(property);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getDisplayCategory() {
        return "Group Importer";
    }

    @Override
    public String getDisplayType() {
        return "Hardcoded Group";
    }

    public static final String[] COMPATIBLE_PROVIDERS = {ANY_PROVIDER};


    public static final String PROVIDER_ID = "oidc-hardcoded-group-idp-mapper";

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return EnumSet.allOf(IdentityProviderSyncMode.class).contains(syncMode);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getHelpText() {
        return "Assign the user to the specified group.";
    }

    @Override
    protected boolean applies(IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        return true;
    }
}
