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

package org.keycloak.protocol.oidc.mappers;

import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.OrganizationRoleModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OIDC Protocol Mapper for including organization roles in tokens.
 * 
 * Maps organization roles to JWT tokens, allowing clients to receive
 * organization-scoped role information for authorization decisions.
 */
public class OrganizationRoleMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

    private static final String PROVIDER_ID = "oidc-organization-role-mapper";
    private static final String ORGANIZATION_ROLES_CLAIM = "organization_roles";
    
    public static final String ORGANIZATION_ID_PROPERTY = "organization.id";
    public static final String CLAIM_NAME = "claim.name";
    public static final String INCLUDE_IN_ACCESS_TOKEN = "access.token.claim";
    public static final String INCLUDE_IN_ID_TOKEN = "id.token.claim";
    public static final String INCLUDE_IN_USERINFO = "userinfo.token.claim";
    
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty organizationProperty = new ProviderConfigProperty();
        organizationProperty.setName(ORGANIZATION_ID_PROPERTY);
        organizationProperty.setLabel("Organization ID");
        organizationProperty.setType(ProviderConfigProperty.STRING_TYPE);
        organizationProperty.setHelpText("The organization ID to include roles for. Leave empty to include roles from all user's organizations.");
        configProperties.add(organizationProperty);

        ProviderConfigProperty claimProperty = new ProviderConfigProperty();
        claimProperty.setName(CLAIM_NAME);
        claimProperty.setLabel("Token Claim Name");
        claimProperty.setType(ProviderConfigProperty.STRING_TYPE);
        claimProperty.setDefaultValue(ORGANIZATION_ROLES_CLAIM);
        claimProperty.setHelpText("Name of the claim to add to the token. Defaults to 'organization_roles'.");
        configProperties.add(claimProperty);

        OIDCAttributeMapperHelper.addTokenClaimNameConfig(configProperties);
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, OrganizationRoleMapper.class);
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getDisplayType() {
        return "Organization Role";
    }

    @Override
    public String getHelpText() {
        return "Map organization roles to a token claim";
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
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession,
                          KeycloakSession keycloakSession, ClientSessionContext clientSessionCtx) {
        UserModel user = userSession.getUser();
        String claimName = mappingModel.getConfig().get(CLAIM_NAME);
        if (claimName == null || claimName.isEmpty()) {
            claimName = ORGANIZATION_ROLES_CLAIM;
        }

        Map<String, Set<String>> organizationRoles = getOrganizationRoles(user, mappingModel, keycloakSession);
        
        if (!organizationRoles.isEmpty()) {
            token.getOtherClaims().put(claimName, organizationRoles);
        }
    }

    /**
     * Get organization roles for the user based on the mapper configuration.
     */
    private Map<String, Set<String>> getOrganizationRoles(UserModel user, ProtocolMapperModel mappingModel, KeycloakSession session) {
        OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
        Map<String, Set<String>> result = new HashMap<>();
        
        String specificOrgId = mappingModel.getConfig().get(ORGANIZATION_ID_PROPERTY);
        
        if (specificOrgId != null && !specificOrgId.trim().isEmpty()) {
            // Get roles for specific organization
            OrganizationModel org = orgProvider.getById(specificOrgId.trim());
            if (org != null) {
                Set<String> roleNames = org.getUserRolesStream(user)
                        .map(OrganizationRoleModel::getName)
                        .collect(Collectors.toSet());
                
                if (!roleNames.isEmpty()) {
                    result.put(org.getAlias() != null ? org.getAlias() : org.getName(), roleNames);
                }
            }
        } else {
            // Get roles for all user's organizations
            orgProvider.getAllStream()
                    .filter(org -> orgProvider.isMember(org, user))
                    .forEach(org -> {
                        Set<String> roleNames = org.getUserRolesStream(user)
                                .map(OrganizationRoleModel::getName)
                                .collect(Collectors.toSet());
                        
                        if (!roleNames.isEmpty()) {
                            result.put(org.getAlias() != null ? org.getAlias() : org.getName(), roleNames);
                        }
                    });
        }
        
        return result;
    }
}
