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

package org.keycloak.broker.oidc.mappers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.keycloak.broker.oidc.KeycloakOIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.provider.ProviderConfigProperty;

import static org.keycloak.utils.RegexUtils.valueMatchesRegex;

public class AdvancedClaimToGroupMapper extends AbstractClaimToGroupMapper {

    public static final String CLAIM_PROPERTY_NAME = "claims";
    public static final String ARE_CLAIM_VALUES_REGEX_PROPERTY_NAME = "are.claim.values.regex";

    public static final String[] COMPATIBLE_PROVIDERS = {KeycloakOIDCIdentityProviderFactory.PROVIDER_ID, OIDCIdentityProviderFactory.PROVIDER_ID};

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty claimsProperty = new ProviderConfigProperty();
        claimsProperty.setName(CLAIM_PROPERTY_NAME);
        claimsProperty.setLabel("Claims");
        claimsProperty.setHelpText("Name and value of the claims to search for in token. You can reference nested claims using a '.', i.e. 'address.locality'. To use dot (.) literally, escape it with backslash (\\.)");
        claimsProperty.setType(ProviderConfigProperty.MAP_TYPE);
        configProperties.add(claimsProperty);
        ProviderConfigProperty isClaimValueRegexProperty = new ProviderConfigProperty();
        isClaimValueRegexProperty.setName(ARE_CLAIM_VALUES_REGEX_PROPERTY_NAME);
        isClaimValueRegexProperty.setLabel("Regex Claim Values");
        isClaimValueRegexProperty.setHelpText("If enabled claim values are interpreted as regular expressions.");
        isClaimValueRegexProperty.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        configProperties.add(isClaimValueRegexProperty);
        ProviderConfigProperty groupProperty = new ProviderConfigProperty();
        groupProperty.setName(ConfigConstants.GROUP);
        groupProperty.setLabel("Group");
        groupProperty.setHelpText("Group to assign the user to if claim is present.");
        groupProperty.setType(ProviderConfigProperty.GROUP_TYPE);
        configProperties.add(groupProperty);
    }

    public static final String PROVIDER_ID = "oidc-advanced-group-idp-mapper";

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return true;
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
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getDisplayCategory() {
        return "Group Importer";
    }

    @Override
    public String getDisplayType() {
        return "Advanced Claim to Group";
    }

    @Override
    public String getHelpText() {
        return "If all claims exists, assign the user to the specified group.";
    }

    @Override
    protected boolean applies(IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        Map<String, List<String>> claims = mapperModel.getConfigMap(CLAIM_PROPERTY_NAME);
        boolean areClaimValuesRegex = Boolean.parseBoolean(mapperModel.getConfig().get(ARE_CLAIM_VALUES_REGEX_PROPERTY_NAME));

        for (Map.Entry<String, List<String>> claim : claims.entrySet()) {
            Object claimValue = getClaimValue(context, claim.getKey());
            for (String value : claim.getValue()) {
                boolean claimValuesMismatch = !(areClaimValuesRegex ? valueMatchesRegex(value, claimValue) : valueEquals(value, claimValue));
                if (claimValuesMismatch) {
                    return false;
                }
            }
        }

        return true;
    }
}
